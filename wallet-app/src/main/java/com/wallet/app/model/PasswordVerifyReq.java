package com.wallet.app.model;

/**
 * 校验支付密码并签发票据请求。
 *
 * @param password 支付密码
 * @param orderNo  要支付的支付单号
 * @param amount   支付金额，单位分（票据将绑定这二者）
 */
public record PasswordVerifyReq(String password, String orderNo, long amount) {
}
