package com.wallet.contract.pay.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 退款主单视图（跨模块数据模型）。只读快照，state 用 String（枚举名），不暴露持久化实体。
 *
 * @param id            主键
 * @param refundNo      退款单号
 * @param orderNo       原支付单号
 * @param userId        用户ID
 * @param refundAmount  退款金额，单位分
 * @param refundPoint   退款积分
 * @param couponBack    券是否返还（1/0）
 * @param state         退款单状态（RefundOrderState 枚举名）
 * @param reason        退款原因
 * @param finishTime    完成时间
 * @param createTime    创建时间
 * @param updateTime    更新时间
 */
@Schema(description = "退款主单视图")
public record RefundOrderView(
    @Schema(description = "主键") Long id,
    @Schema(description = "退款单号") String refundNo,
    @Schema(description = "原支付单号") String orderNo,
    @Schema(description = "用户ID") Long userId,
    @Schema(description = "退款金额，单位分") Long refundAmount,
    @Schema(description = "退款积分") Long refundPoint,
    @Schema(description = "券是否返还（1/0）") Integer couponBack,
    @Schema(description = "退款单状态（RefundOrderState 枚举名）") String state,
    @Schema(description = "退款原因") String reason,
    @Schema(description = "完成时间") LocalDateTime finishTime,
    @Schema(description = "创建时间") LocalDateTime createTime,
    @Schema(description = "更新时间") LocalDateTime updateTime) {
}
