package com.wallet.app.model;

import jakarta.validation.constraints.NotBlank;

/**
 * 提交支付请求。
 *
 * @param orderNo 支付单号
 * @param ticket  支付密码授权票据（含余额/积分/券分段时必填，条件校验在服务层）
 */
public record SubmitReq(@NotBlank(message = "支付单号不能为空") String orderNo, String ticket) {
}
