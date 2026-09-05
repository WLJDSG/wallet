package com.wallet.security.core;

import com.wallet.security.enums.AuthorizeMethodEnum;
import com.wallet.security.token.PayAuthorizationToken;
import com.wallet.security.token.PayBiometricChallengeToken;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class JsonHelperTest {

    private final JsonHelper jsonHelper = new JsonHelper();

    @Test
    public void roundTripKeepsAllFields() {
        PayAuthorizationToken token = new PayAuthorizationToken();
        token.setUid(7L);
        token.setOrderNo("order-1");
        token.setOrderType("order");
        token.setAmount(new BigDecimal("100.00"));
        token.setCurrency("JPY");
        token.setAuthorizationMethod(AuthorizeMethodEnum.PASSWORD);
        token.setPasswordVersion(1);
        token.setSecurityVersion(2);
        token.setPolicyVersion("v1");
        token.setIssuedAt(1L);
        token.setExpiresAt(2L);

        PayAuthorizationToken parsed = jsonHelper.fromJson(jsonHelper.toJson(token), PayAuthorizationToken.class);
        assertEquals(token, parsed);
    }

    @Test
    public void bigDecimalScaleSurvivesRoundTripForChallengePayload() {
        PayBiometricChallengeToken challenge = new PayBiometricChallengeToken();
        challenge.setNonce("nonce");
        challenge.setUid(7L);
        challenge.setCredentialId("credential");
        challenge.setOrderNo("order-1");
        challenge.setOrderType("order");
        challenge.setAmount(new BigDecimal("100.00"));
        challenge.setCurrency("JPY");
        challenge.setExpiresAt(123456789L);
        String expectedPayload = PaySecurityChecks.challengePayload("challenge", challenge);

        PayBiometricChallengeToken parsed = jsonHelper.fromJson(jsonHelper.toJson(challenge),
            PayBiometricChallengeToken.class);
        assertEquals(expectedPayload, PaySecurityChecks.challengePayload("challenge", parsed));
    }

    @Test
    public void unknownFieldsAreIgnored() {
        PayAuthorizationToken parsed = jsonHelper.fromJson("{\"uid\":7,\"futureField\":\"x\"}",
            PayAuthorizationToken.class);
        assertEquals(Long.valueOf(7), parsed.getUid());
    }

    @Test
    public void legacyTypedArrayFormatAndGarbageParseToNull() {
        // 旧版宿主 Redis 序列化格式（带 FQCN 的数组包装）按过期处理
        assertNull(jsonHelper.fromJson("[\"com.zbkj.common.dto.pay.PayAuthorizationToken\",{\"uid\":7}]",
            PayAuthorizationToken.class));
        assertNull(jsonHelper.fromJson("not-json", PayAuthorizationToken.class));
        assertNull(jsonHelper.fromJson(null, PayAuthorizationToken.class));
    }
}
