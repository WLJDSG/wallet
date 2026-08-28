package com.wallet.contract.pay.model;

import com.wallet.common.enums.PayType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 支付分段视图（跨模块数据模型）。只读快照，state 用 String（枚举名），不暴露持久化实体。
 *
 * @param id              主键
 * @param partNo          分段号
 * @param orderNo         支付单号
 * @param userId          用户ID
 * @param payType         分段类型
 * @param amount          本段金额，单位分
 * @param pointCount      积分段消耗积分数
 * @param userCouponId    券段用户券 ID
 * @param channelCode     三方段渠道编码
 * @param thirdNo         渠道侧交易号
 * @param channelPayload  渠道支付参数（前端拉起支付用）
 * @param state           分段状态（PartState 枚举名）
 * @param refundedAmount  已退金额，单位分
 * @param payTime         支付时间
 * @param createTime      创建时间
 * @param updateTime      更新时间
 */
@Schema(description = "支付分段视图")
public record PayPartView(
    @Schema(description = "主键") Long id,
    @Schema(description = "分段号") String partNo,
    @Schema(description = "支付单号") String orderNo,
    @Schema(description = "用户ID") Long userId,
    @Schema(description = "分段类型") PayType payType,
    @Schema(description = "本段金额，单位分") Long amount,
    @Schema(description = "积分段消耗积分数") Long pointCount,
    @Schema(description = "券段用户券 ID") Long userCouponId,
    @Schema(description = "三方段渠道编码") String channelCode,
    @Schema(description = "渠道侧交易号") String thirdNo,
    @Schema(description = "渠道支付参数（前端拉起支付用）") String channelPayload,
    @Schema(description = "分段状态（PartState 枚举名）") String state,
    @Schema(description = "已退金额，单位分") Long refundedAmount,
    @Schema(description = "支付时间") LocalDateTime payTime,
    @Schema(description = "创建时间") LocalDateTime createTime,
    @Schema(description = "更新时间") LocalDateTime updateTime) {
}
