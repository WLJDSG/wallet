package com.wallet.contract.pay.model;

import com.wallet.common.enums.PayType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 退款分摊分段视图（跨模块数据模型）。只读快照，state 用 String（枚举名），不暴露持久化实体。
 *
 * @param id              主键
 * @param refundPartNo    退款分段号
 * @param refundNo        退款单号
 * @param partNo          原支付分段号
 * @param payType         分段类型
 * @param amount          退款金额，单位分
 * @param pointCount      退款积分
 * @param channelRefundNo 渠道侧退款号
 * @param state           退款分段状态（RefundState 枚举名）
 * @param createTime      创建时间
 * @param updateTime      更新时间
 */
@Schema(description = "退款分摊分段视图")
public record RefundPartView(
    @Schema(description = "主键") Long id,
    @Schema(description = "退款分段号") String refundPartNo,
    @Schema(description = "退款单号") String refundNo,
    @Schema(description = "原支付分段号") String partNo,
    @Schema(description = "分段类型") PayType payType,
    @Schema(description = "退款金额，单位分") Long amount,
    @Schema(description = "退款积分") Long pointCount,
    @Schema(description = "渠道侧退款号") String channelRefundNo,
    @Schema(description = "退款分段状态（RefundState 枚举名）") String state,
    @Schema(description = "创建时间") LocalDateTime createTime,
    @Schema(description = "更新时间") LocalDateTime updateTime) {
}
