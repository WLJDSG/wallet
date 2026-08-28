package com.wallet.contract.pay.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 支付主单视图（跨模块数据模型）。只读快照，state 用 String（枚举名），不暴露持久化实体。
 *
 * @param id               主键
 * @param orderNo          钱包支付单号
 * @param appId            来源商城/接入方
 * @param bizOrderNo       外部业务单号
 * @param userId           用户ID
 * @param totalAmount      应付总额，单位分
 * @param currency         币种
 * @param state            主单状态（OrderState 枚举名）
 * @param expireTime       过期时间
 * @param payTime          支付时间
 * @param closeTime        关闭时间
 * @param refundableAmount 可退金额，单位分
 * @param refundedAmount   已退金额，单位分
 * @param failReason       失败原因
 * @param createTime       创建时间
 * @param updateTime       更新时间
 */
@Schema(description = "支付主单视图")
public record PayOrderView(
    @Schema(description = "主键") Long id,
    @Schema(description = "钱包支付单号") String orderNo,
    @Schema(description = "来源商城/接入方") String appId,
    @Schema(description = "外部业务单号") String bizOrderNo,
    @Schema(description = "用户ID") Long userId,
    @Schema(description = "应付总额，单位分") Long totalAmount,
    @Schema(description = "币种") String currency,
    @Schema(description = "主单状态（OrderState 枚举名）") String state,
    @Schema(description = "过期时间") LocalDateTime expireTime,
    @Schema(description = "支付时间") LocalDateTime payTime,
    @Schema(description = "关闭时间") LocalDateTime closeTime,
    @Schema(description = "可退金额，单位分") Long refundableAmount,
    @Schema(description = "已退金额，单位分") Long refundedAmount,
    @Schema(description = "失败原因") String failReason,
    @Schema(description = "创建时间") LocalDateTime createTime,
    @Schema(description = "更新时间") LocalDateTime updateTime) {
}
