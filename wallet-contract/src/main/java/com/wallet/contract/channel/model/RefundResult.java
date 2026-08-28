package com.wallet.contract.channel.model;

/**
 * 渠道退款结果。
 *
 * <p>渠道实现应尽量以 {@code success=false + failReason} 表达业务性失败
 * （如信用审查未通过），而不是抛异常——编排层会把两者都落为退款单 FAIL，
 * 但结构化结果能保留失败原因供调用方展示。</p>
 *
 * @param success         渠道是否受理/完成退款
 * @param channelRefundNo 渠道退款流水号，可为 null
 * @param failReason      失败原因（success=false 时填写）
 */
public record RefundResult(boolean success, String channelRefundNo, String failReason) {

    public static RefundResult ok(String channelRefundNo) {
        return new RefundResult(true, channelRefundNo, null);
    }

    public static RefundResult fail(String failReason) {
        return new RefundResult(false, null, failReason);
    }
}
