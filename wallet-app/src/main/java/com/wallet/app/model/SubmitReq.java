package com.wallet.app.model;

/**
 * 提交支付请求。
 *
 * @param orderNo 支付单号
 * @param ticket  支付密码授权票据（含余额/积分/券分段时必填）
 */
public record SubmitReq(String orderNo, String ticket) {
}
