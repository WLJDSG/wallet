package com.wallet.account.entity;

import com.wallet.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import com.wallet.common.enums.CouponType;

import java.time.LocalDateTime;

/**
 * 用户券。核销 / 返还全部走条件更新（CAS），修正 CRMEB 无 CAS 的问题。
 * status：0 未使用 / 1 已使用 / 2 已失效。
 */
@TableName("user_coupon")
@Schema(description = "用户券")
public class UserCoupon extends BaseEntity {

    @TableId(type = IdType.AUTO)
    @Schema(description = "用户券 ID")
    private Long id;
    @Schema(description = "用户ID")
    private Long userId;
    @Schema(description = "券模板 ID")
    private Long couponId;
    @Schema(description = "券名称（快照）")
    private String name;
    @Schema(description = "券类型：FULL_CUT 满减 / DISCOUNT 折扣（快照）")
    private CouponType type;
    @Schema(description = "满减券面额，单位分；折扣券为 0（快照）")
    private Long faceAmount;
    @Schema(description = "使用门槛，单位分（快照）")
    private Long minAmount;
    @Schema(description = "折扣券折扣率百分比，85=八五折（快照）")
    private Integer discountRate;
    @Schema(description = "最高抵扣，单位分，0 不限（快照）")
    private Long maxDeductAmount;
    @Schema(description = "状态：0 未用 1 已用 2 失效")
    private Integer status;
    @Schema(description = "核销支付单号")
    private String useOrderNo;
    @Schema(description = "核销时间")
    private LocalDateTime useTime;
    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getCouponId() {
        return couponId;
    }

    public void setCouponId(Long couponId) {
        this.couponId = couponId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CouponType getType() {
        return type;
    }

    public void setType(CouponType type) {
        this.type = type;
    }

    public Integer getDiscountRate() {
        return discountRate;
    }

    public void setDiscountRate(Integer discountRate) {
        this.discountRate = discountRate;
    }

    public Long getMaxDeductAmount() {
        return maxDeductAmount;
    }

    public void setMaxDeductAmount(Long maxDeductAmount) {
        this.maxDeductAmount = maxDeductAmount;
    }

    public Long getFaceAmount() {
        return faceAmount;
    }

    public void setFaceAmount(Long faceAmount) {
        this.faceAmount = faceAmount;
    }

    public Long getMinAmount() {
        return minAmount;
    }

    public void setMinAmount(Long minAmount) {
        this.minAmount = minAmount;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getUseOrderNo() {
        return useOrderNo;
    }

    public void setUseOrderNo(String useOrderNo) {
        this.useOrderNo = useOrderNo;
    }

    public LocalDateTime getUseTime() {
        return useTime;
    }

    public void setUseTime(LocalDateTime useTime) {
        this.useTime = useTime;
    }

    public LocalDateTime getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(LocalDateTime expireTime) {
        this.expireTime = expireTime;
    }
}
