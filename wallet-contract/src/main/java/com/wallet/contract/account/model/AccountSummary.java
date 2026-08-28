package com.wallet.contract.account.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 资产总览（跨模块数据模型）。
 *
 * @param money         余额，单位分
 * @param point         积分数量
 * @param usableCoupons 可用券列表
 */
@Schema(description = "资产总览")
public record AccountSummary(
    @Schema(description = "余额，单位分") long money,
    @Schema(description = "积分数量") long point,
    @Schema(description = "可用券列表") List<CouponView> usableCoupons) {
}
