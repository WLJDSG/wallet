package com.wallet.app.model;

import jakarta.validation.constraints.NotBlank;

/**
 * 设置/重置支付密码请求。
 *
 * @param password    新密码
 * @param oldPassword 旧密码（已设置过时必填，条件校验在服务层）
 */
public record PasswordSetReq(@NotBlank(message = "密码不能为空") String password, String oldPassword) {
}
