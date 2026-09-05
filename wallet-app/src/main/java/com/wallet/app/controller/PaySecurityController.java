package com.wallet.app.controller;

import com.wallet.common.result.ApiResult;
import com.wallet.app.security.UserPhoneResolver;
import com.wallet.security.PaySecurityEngine;
import com.wallet.security.enums.BiometricPlatformEnum;
import com.wallet.security.enums.IdentityPurposeEnum;
import com.wallet.security.enums.SignAlgorithmEnum;
import com.wallet.security.model.AuthorizationResult;
import com.wallet.security.model.BiometricChallenge;
import com.wallet.security.model.BiometricCredentialInfo;
import com.wallet.security.model.BiometricRegistrationOptions;
import com.wallet.security.model.ClientInfo;
import com.wallet.security.model.SecurityStatus;
import com.wallet.security.model.UserIdentity;
import com.wallet.security.error.PaySecurityErrorCode;
import com.wallet.security.error.PaySecurityException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 支付密码、短信身份验证、生物识别及一次性余额支付授权入口。 */
@Tag(name = "支付安全")
@RestController
@RequestMapping("/api/pay/security")
@AllArgsConstructor
public class PaySecurityController {

    private final PaySecurityEngine security;
    private final UserPhoneResolver userPhoneResolver;

    @GetMapping("/status")
    @Operation(summary = "查询支付安全状态")
    public ApiResult<SecurityStatus> status(@RequestHeader("X-Uid") Long uid,
        @RequestParam(required = false) String credentialId) {
        return ApiResult.ok(security.getPasswordAuthService().getStatus(user(uid, null), credentialId));
    }

    @PostMapping("/password/authorize")
    @Operation(summary = "支付密码授权")
    public ApiResult<AuthorizationResult> authorizePassword(@RequestHeader("X-Uid") Long uid,
        @Valid @RequestBody PasswordAuthorizeRequest request, HttpServletRequest http) {
        return ApiResult.ok(security.getPasswordAuthService().authorizePassword(user(uid, null), request.orderNo(),
            "WALLET", request.password(), client(http)));
    }

    @PostMapping("/password/sms/send")
    @Operation(summary = "发送设置或重置支付密码的短信验证码")
    public ApiResult<Void> sendSms(@RequestHeader("X-Uid") Long uid, @Valid @RequestBody SmsRequest request) {
        security.getIdentityService().sendIdentityCode(smsUser(uid), request.purpose());
        return ApiResult.ok();
    }

    @PostMapping("/password/identity/verify")
    @Operation(summary = "短信验证码换一次性身份票据")
    public ApiResult<String> verifyIdentity(@RequestHeader("X-Uid") Long uid,
        @Valid @RequestBody IdentityVerifyRequest request, HttpServletRequest http) {
        return ApiResult.ok(security.getIdentityService().verifyIdentityCode(smsUser(uid), request.purpose(),
            request.code(), client(http)));
    }

    @PostMapping("/legacy-confirm/authorize")
    @Operation(summary = "未设置支付密码用户的二次确认授权")
    public ApiResult<AuthorizationResult> authorizeLegacyConfirm(@RequestHeader("X-Uid") Long uid,
        @Valid @RequestBody LegacyConfirmRequest request, HttpServletRequest http) {
        return ApiResult.ok(security.getPasswordAuthService().authorizeLegacyConfirm(user(uid, null),
            request.orderNo(), "WALLET", client(http)));
    }

    @PostMapping("/password/identity/authorize")
    @Operation(summary = "当前密码换修改密码或生物注册身份票据")
    public ApiResult<String> authorizeIdentity(@RequestHeader("X-Uid") Long uid,
        @Valid @RequestBody PasswordIdentityRequest request, HttpServletRequest http) {
        return ApiResult.ok(security.getIdentityService().authorizePasswordIdentity(user(uid, null), request.purpose(),
            request.password(), request.orderNo(), request.orderNo() == null ? null : "WALLET", client(http)));
    }

    @PostMapping("/password/update")
    @Transactional
    @Operation(summary = "设置、修改或重置支付密码")
    public ApiResult<Void> updatePassword(@RequestHeader("X-Uid") Long uid,
        @Valid @RequestBody PasswordUpdateRequest request, HttpServletRequest http) {
        security.getIdentityService().updatePassword(user(uid, null), request.purpose(), request.identityToken(),
            request.password(), request.confirmPassword(), client(http));
        return ApiResult.ok();
    }

    @PostMapping("/biometric/registration/options")
    @Operation(summary = "创建生物凭证注册会话")
    public ApiResult<BiometricRegistrationOptions> registration(@RequestHeader("X-Uid") Long uid,
        @Valid @RequestBody RegistrationRequest request) {
        return ApiResult.ok(security.getBiometricService().createRegistration(user(uid, null),
            request.biometricEnrollmentToken(), request.platform()));
    }

    @PostMapping("/biometric/credentials")
    @Transactional
    @Operation(summary = "注册设备生物支付公钥")
    public ApiResult<BiometricCredentialInfo> registerCredential(@RequestHeader("X-Uid") Long uid,
        @Valid @RequestBody CredentialRequest request, HttpServletRequest http) {
        return ApiResult.ok(security.getBiometricService().registerCredential(user(uid, null), request.registrationId(),
            request.publicKey(), request.algorithm(), request.signature(), client(http)));
    }

    @GetMapping("/biometric/credentials")
    @Operation(summary = "查询当前用户生物支付凭证")
    public ApiResult<List<BiometricCredentialInfo>> credentials(@RequestHeader("X-Uid") Long uid) {
        return ApiResult.ok(security.getBiometricService().listCredentials(user(uid, null)));
    }

    @PostMapping("/biometric/challenges")
    @Operation(summary = "创建订单绑定的生物签名挑战")
    public ApiResult<BiometricChallenge> challenge(@RequestHeader("X-Uid") Long uid,
        @Valid @RequestBody ChallengeRequest request) {
        return ApiResult.ok(security.getBiometricService().createChallenge(user(uid, null), request.credentialId(),
            request.orderNo(), "WALLET"));
    }

    @PostMapping("/biometric/authorize")
    @Operation(summary = "验证生物签名并签发支付授权")
    public ApiResult<AuthorizationResult> authorizeBiometric(@RequestHeader("X-Uid") Long uid,
        @Valid @RequestBody BiometricAuthorizeRequest request, HttpServletRequest http) {
        return ApiResult.ok(security.getBiometricService().authorize(user(uid, null), request.challengeId(),
            request.credentialId(), request.signature(), client(http)));
    }

    @GetMapping("/biometric/credentials/{credentialId}")
    @Operation(summary = "查询生物支付凭证状态")
    public ApiResult<BiometricCredentialInfo> credential(@RequestHeader("X-Uid") Long uid,
        @PathVariable String credentialId) {
        return ApiResult.ok(security.getBiometricService().getCredentialStatus(user(uid, null), credentialId));
    }

    @DeleteMapping("/biometric/credentials/{credentialId}")
    @Transactional
    @Operation(summary = "撤销指定生物支付凭证")
    public ApiResult<Void> revoke(@RequestHeader("X-Uid") Long uid, @PathVariable String credentialId,
        HttpServletRequest http) {
        security.getBiometricService().revokeCredential(user(uid, null), credentialId, client(http));
        return ApiResult.ok();
    }

    @PostMapping("/biometric/credentials/revoke-all")
    @Transactional
    @Operation(summary = "撤销全部生物支付凭证并使历史票据失效")
    public ApiResult<Void> revokeAll(@RequestHeader("X-Uid") Long uid, HttpServletRequest http) {
        security.getBiometricService().revokeAll(user(uid, null), client(http));
        return ApiResult.ok();
    }

    private UserIdentity user(Long uid, String phone) { return UserIdentity.of(uid, phone, true); }

    private UserIdentity smsUser(Long uid) {
        String phone = userPhoneResolver.resolve(uid);
        if (phone == null || phone.isBlank()) {
            throw PaySecurityException.of(PaySecurityErrorCode.PAY_SECURITY_USER_INVALID);
        }
        return user(uid, phone);
    }

    private ClientInfo client(HttpServletRequest request) {
        return new ClientInfo(request.getHeader("X-Platform"), request.getHeader("X-System"),
            request.getHeader("X-App-Version"), request.getRemoteAddr(), request.getHeader("User-Agent"));
    }

    public record PasswordAuthorizeRequest(@Pattern(regexp = "\\d{6}") String password,
                                           @NotBlank String orderNo) { }
    public record LegacyConfirmRequest(@NotBlank String orderNo) { }
    public record SmsRequest(@NotNull IdentityPurposeEnum purpose) { }
    public record IdentityVerifyRequest(@NotNull IdentityPurposeEnum purpose,
                                        @Pattern(regexp = "\\d{6}") String code) { }
    public record PasswordIdentityRequest(@NotNull IdentityPurposeEnum purpose,
                                          @Pattern(regexp = "\\d{6}") String password,
                                          String orderNo) { }
    public record PasswordUpdateRequest(@NotNull IdentityPurposeEnum purpose, @NotBlank String identityToken,
                                        @Pattern(regexp = "\\d{6}") String password,
                                        @Pattern(regexp = "\\d{6}") String confirmPassword) { }
    public record RegistrationRequest(@NotBlank String biometricEnrollmentToken,
                                      @NotNull BiometricPlatformEnum platform) { }
    public record CredentialRequest(@NotBlank String registrationId, @NotBlank String publicKey,
                                    @NotNull SignAlgorithmEnum algorithm, @NotBlank String signature) { }
    public record ChallengeRequest(@NotBlank String credentialId, @NotBlank String orderNo) { }
    public record BiometricAuthorizeRequest(@NotBlank String challengeId, @NotBlank String credentialId,
                                            @NotBlank String signature) { }
}
