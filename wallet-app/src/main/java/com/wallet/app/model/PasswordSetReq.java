package com.wallet.app.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 设置/重置支付密码请求。
 *
 * @param password    新密码
 * @param oldPassword 旧密码（已设置过时必填，条件校验在服务层）
 */
@Schema(description = "设置/重置支付密码请求")
public record PasswordSetReq(
    @Schema(description = "新密码", example = "123456")
    @NotBlank(message = "密码不能为空") String password,
    @Schema(description = "旧密码（已设置过时必填）") String oldPassword) {
}
