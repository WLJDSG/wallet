package com.wallet.app.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import com.wallet.security.enums.IdentityPurposeEnum;

/**
 * 设置/重置支付密码请求。
 *
 * @param password    新密码
 * @param confirmPassword 确认密码
 * @param identityToken 短信或当前密码认证换取的一次性身份票据
 * @param purpose 设置、修改或重置用途
 */
@Schema(description = "设置/重置支付密码请求")
public record PasswordSetReq(
    @Schema(description = "新密码", example = "123456")
    @Pattern(regexp = "\\d{6}", message = "密码必须为6位数字") String password,
    @Schema(description = "确认新密码", example = "123456")
    @Pattern(regexp = "\\d{6}", message = "确认密码必须为6位数字") String confirmPassword,
    @Schema(description = "一次性身份票据") @NotBlank String identityToken,
    @Schema(description = "PASSWORD_SET/PASSWORD_CHANGE/PASSWORD_RESET") @NotNull IdentityPurposeEnum purpose) {
}
