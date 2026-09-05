package com.wallet.security.service;

import com.wallet.security.testutil.PaySecurityEngineTestSupport;
import com.wallet.security.enums.IdentityPurposeEnum;
import com.wallet.security.error.PaySecurityErrorCode;
import com.wallet.security.model.AuthorizationConsumeCommand;
import com.wallet.security.model.ClientInfo;
import com.wallet.security.model.UserIdentity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AuthorizationServiceTest extends PaySecurityEngineTestSupport {

    private static final String PASSWORD = "739135";

    private AuthorizationConsumeCommand command(String token, BigDecimal payPrice) {
        AuthorizationConsumeCommand cmd = new AuthorizationConsumeCommand();
        cmd.setRequired(true);
        cmd.setPayAuthorizationToken(token);
        cmd.setUser(user);
        cmd.setOrderNo("o1");
        cmd.setOrderType("order");
        cmd.setPayPrice(payPrice);
        cmd.setCurrency("JPY");
        cmd.setClientInfo(appClientInfo);
        return cmd;
    }

    private String issueLegacyToken() {
        orders.register("o1", "order", new BigDecimal("100.00"), "JPY", false);
        return engine.getPasswordAuthService().authorizeLegacyConfirm(user, "o1", "order", appClientInfo)
            .getPayAuthorizationToken();
    }

    @Test
    public void notRequiredWithoutTokenPassesSilently() {
        AuthorizationConsumeCommand cmd = command(null, new BigDecimal("100.00"));
        cmd.setRequired(false);
        engine.getAuthorizationService().consume(cmd);
    }

    @Test
    public void requiredWithoutTokenRejected() {
        expectError(PaySecurityErrorCode.PAY_AUTH_TOKEN_REQUIRED,
            () -> engine.getAuthorizationService().consume(command(" ", new BigDecimal("100.00"))));
    }

    @Test
    public void unknownTokenTreatedAsExpired() {
        expectError(PaySecurityErrorCode.PAY_AUTH_TOKEN_EXPIRED,
            () -> engine.getAuthorizationService().consume(command("ghost-token", new BigDecimal("100.00"))));
    }

    @Test
    public void happyPathConsumesOnceAndRejectsReplayAsUsed() {
        String token = issueLegacyToken();
        engine.getAuthorizationService().consume(command(token, new BigDecimal("100.00")));
        assertTrue(audits.eventTypes().contains("PAY_AUTH_CONSUME"));
        expectError(PaySecurityErrorCode.PAY_AUTH_TOKEN_USED,
            () -> engine.getAuthorizationService().consume(command(token, new BigDecimal("100.00"))));
    }

    @Test
    public void mismatchReleasesUsedMarkerForLegitimateRetry() {
        String token = issueLegacyToken();
        expectError(PaySecurityErrorCode.PAY_AUTH_TOKEN_INVALID,
            () -> engine.getAuthorizationService().consume(command(token, new BigDecimal("99.00"))));
        // 校验失败释放 used 占位，携带正确金额可以重试成功
        engine.getAuthorizationService().consume(command(token, new BigDecimal("100.00")));
    }

    @Test
    public void orderAmountDriftAfterIssueRejectedAsStateChanged() {
        String token = issueLegacyToken();
        orders.changeAmount("o1", new BigDecimal("120.00"));
        expectError(PaySecurityErrorCode.PAY_SECURITY_STATE_CHANGED,
            () -> engine.getAuthorizationService().consume(command(token, new BigDecimal("100.00"))));
    }

    @Test
    public void paidOrderRejectedAsStateChanged() {
        String token = issueLegacyToken();
        orders.markPaid("o1");
        expectError(PaySecurityErrorCode.PAY_SECURITY_STATE_CHANGED,
            () -> engine.getAuthorizationService().consume(command(token, new BigDecimal("100.00"))));
    }

    @Test
    public void nullOrInactiveUserRejected() {
        String token = issueLegacyToken();
        AuthorizationConsumeCommand withoutUser = command(token, new BigDecimal("100.00"));
        withoutUser.setUser(null);
        expectError(PaySecurityErrorCode.PAY_AUTH_TOKEN_INVALID, () -> engine.getAuthorizationService().consume(withoutUser));
        AuthorizationConsumeCommand inactive = command(token, new BigDecimal("100.00"));
        inactive.setUser(UserIdentity.of(user.getUid(), user.getPhone(), false));
        expectError(PaySecurityErrorCode.PAY_AUTH_TOKEN_INVALID, () -> engine.getAuthorizationService().consume(inactive));
    }

    @Test
    public void legacyTokenInvalidatedOncePasswordIsSet() {
        String token = issueLegacyToken();
        setPassword(PASSWORD);
        expectError(PaySecurityErrorCode.PAY_SECURITY_STATE_CHANGED,
            () -> engine.getAuthorizationService().consume(command(token, new BigDecimal("100.00"))));
    }

    @Test
    public void passwordTokenInvalidatedByPasswordChange() {
        setPassword(PASSWORD);
        orders.register("o1", "order", new BigDecimal("100.00"), "JPY", false);
        String token = engine.getPasswordAuthService().authorizePassword(user, "o1", "order", PASSWORD, appClientInfo)
            .getPayAuthorizationToken();
        String identityToken = engine.getIdentityService().authorizePasswordIdentity(user, IdentityPurposeEnum.PASSWORD_CHANGE, PASSWORD, null,
            null, appClientInfo);
        engine.getIdentityService().updatePassword(user, IdentityPurposeEnum.PASSWORD_CHANGE, identityToken, "351397", "351397", appClientInfo);
        expectError(PaySecurityErrorCode.PAY_SECURITY_STATE_CHANGED,
            () -> engine.getAuthorizationService().consume(command(token, new BigDecimal("100.00"))));
    }

    @Test
    public void policyGateChecksEnabledPlatformAndVersionBoundary() {
        assertFalse(engine.getAuthorizationService().isAuthorizationRequired(user, appClientInfo));
        expectError(PaySecurityErrorCode.PAY_SECURITY_CLIENT_INVALID,
            () -> engine.getAuthorizationService().isAuthorizationRequired(user, new ClientInfo(" ", "ios", "100", null, null)));
        expectError(PaySecurityErrorCode.PAY_SECURITY_CLIENT_INVALID,
            () -> engine.getAuthorizationService().isAuthorizationRequired(user,
                new ClientInfo("app", "ios", "not-a-number", null, null)));
        properties.setEnforceVersion(100);
        assertTrue(engine.getAuthorizationService().isAuthorizationRequired(user, appClientInfo));
        assertFalse(engine.getAuthorizationService()
            .isAuthorizationRequired(user, new ClientInfo("app", "ios", "99", null, null)));
        properties.setEnabled(false);
        assertFalse(engine.getAuthorizationService().isAuthorizationRequired(user, appClientInfo));
    }

    @Test
    public void amountBoundTokenConsumableForOrderCreatedAfterAuthorization() {
        // 金额授权模式：签发时不传订单号（订单在支付请求内才生成的流程）
        String token = engine.getPasswordAuthService()
            .authorizeLegacyConfirm(user, null, null, new BigDecimal("100.00"), "JPY", appClientInfo)
            .getPayAuthorizationToken();
        // 支付时订单已生成：订单号不做绑定，但金额、币种、归属与未支付照常强校验
        orders.register("new-order", "subscribe", new BigDecimal("100.00"), "JPY", false);
        AuthorizationConsumeCommand cmd = command(token, new BigDecimal("100.00"));
        cmd.setOrderNo("new-order");
        cmd.setOrderType("subscribe");
        engine.getAuthorizationService().consume(cmd);
    }

    @Test
    public void amountBoundTokenRejectedWhenActualOrderAmountDiffers() {
        String token = engine.getPasswordAuthService()
            .authorizeLegacyConfirm(user, null, null, new BigDecimal("100.00"), "JPY", appClientInfo)
            .getPayAuthorizationToken();
        // 实际订单金额与授权金额不一致：即使请求金额与订单一致也拒绝
        orders.register("new-order", "subscribe", new BigDecimal("120.00"), "JPY", false);
        AuthorizationConsumeCommand cmd = command(token, new BigDecimal("120.00"));
        cmd.setOrderNo("new-order");
        cmd.setOrderType("subscribe");
        expectError(PaySecurityErrorCode.PAY_AUTH_TOKEN_INVALID, () -> engine.getAuthorizationService().consume(cmd));
    }

    @Test
    public void amountModeRequiresPositiveAmountAndCurrency() {
        expectError(PaySecurityErrorCode.PAY_SECURITY_AMOUNT_INVALID,
            () -> engine.getPasswordAuthService().authorizeLegacyConfirm(user, null, null, null, "JPY", appClientInfo));
        expectError(PaySecurityErrorCode.PAY_SECURITY_AMOUNT_INVALID,
            () -> engine.getPasswordAuthService().authorizeLegacyConfirm(user, null, null, BigDecimal.ZERO, "JPY", appClientInfo));
        expectError(PaySecurityErrorCode.PAY_SECURITY_AMOUNT_INVALID,
            () -> engine.getPasswordAuthService().authorizeLegacyConfirm(user, null, null, new BigDecimal("100.00"), " ",
                appClientInfo));
    }

    @Test
    public void orderBoundTokenStillRejectsDifferentOrder() {
        String token = issueLegacyToken();
        orders.register("o2", "order", new BigDecimal("100.00"), "JPY", false);
        AuthorizationConsumeCommand cmd = command(token, new BigDecimal("100.00"));
        cmd.setOrderNo("o2");
        expectError(PaySecurityErrorCode.PAY_AUTH_TOKEN_INVALID, () -> engine.getAuthorizationService().consume(cmd));
    }

    @Test
    public void passwordSetUserIsAlwaysRequiredRegardlessOfClientVersion() {
        // 未设密码：版本边界之前放行
        assertFalse(engine.getAuthorizationService().isAuthorizationRequired(user, appClientInfo));
        setPassword(PASSWORD);
        // 已设密码：不随客户端版本降级，谎报低版本也强制授权
        assertTrue(engine.getAuthorizationService().isAuthorizationRequired(user, appClientInfo));
        assertTrue(engine.getAuthorizationService()
            .isAuthorizationRequired(user, new ClientInfo("app", "ios", "1", null, null)));
        // user 缺失时退回版本边界口径（宿主扣款链路对空用户另有拦截）
        assertFalse(engine.getAuthorizationService().isAuthorizationRequired(null, appClientInfo));
        // 总开关是故障止损通道，优先级最高
        properties.setEnabled(false);
        assertFalse(engine.getAuthorizationService().isAuthorizationRequired(user, appClientInfo));
    }
}
