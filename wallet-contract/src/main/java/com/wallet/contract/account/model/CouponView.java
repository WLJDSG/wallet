package com.wallet.contract.account.model;

import com.wallet.common.enums.CouponType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 用户券快照（跨模块数据模型）。与持久化实体 {@code UserCoupon} 字段一一对应，
 * 但只是只读 record，供支付编排与 Web 层使用，不暴露 MyBatis 实体。
 *
 * @param id             用户券 ID
 * @param userId         用户ID
 * @param couponId       券模板 ID
 * @param name           券名称（快照）
 * @param type           券类型：FULL_CUT 满减 / DISCOUNT 折扣（快照）
 * @param faceAmount     满减券面额，单位分；折扣券为 0（快照）
 * @param minAmount      使用门槛，单位分（快照）
 * @param discountRate   折扣券折扣率百分比，85=八五折（快照）
 * @param maxDeductAmount 最高抵扣，单位分，0 不限（快照）
 * @param status         状态：0 未用 1 已用 2 失效
 * @param useOrderNo     核销支付单号
 * @param useTime        核销时间
 * @param expireTime     过期时间
 * @param createTime     领取时间
 */
@Schema(description = "用户券")
public record CouponView(
    @Schema(description = "用户券 ID") Long id,
    @Schema(description = "用户ID") Long userId,
    @Schema(description = "券模板 ID") Long couponId,
    @Schema(description = "券名称（快照）") String name,
    @Schema(description = "券类型：FULL_CUT 满减 / DISCOUNT 折扣（快照）") CouponType type,
    @Schema(description = "满减券面额，单位分；折扣券为 0（快照）") Long faceAmount,
    @Schema(description = "使用门槛，单位分（快照）") Long minAmount,
    @Schema(description = "折扣券折扣率百分比，85=八五折（快照）") Integer discountRate,
    @Schema(description = "最高抵扣，单位分，0 不限（快照）") Long maxDeductAmount,
    @Schema(description = "状态：0 未用 1 已用 2 失效") Integer status,
    @Schema(description = "核销支付单号") String useOrderNo,
    @Schema(description = "核销时间") LocalDateTime useTime,
    @Schema(description = "过期时间") LocalDateTime expireTime,
    @Schema(description = "领取时间") LocalDateTime createTime) {
}
