package com.wallet.app.model;

/**
 * 设置/重置支付密码请求。
 *
 * @param password    新密码
 * @param oldPassword 旧密码（已设置过时必填）
 */
public record PasswordSetReq(String password, String oldPassword) {
}
