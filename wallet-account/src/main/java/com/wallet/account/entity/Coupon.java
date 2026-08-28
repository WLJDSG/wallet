package com.wallet.account.entity;

import com.wallet.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import com.wallet.common.enums.CouponType;

import java.time.LocalDateTime;

/**
 * 优惠券模板（满减券）。
 */
@TableName("coupon")
public class Coupon extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    /** 券类型：FULL_CUT 满减 / DISCOUNT 折扣 */
    private CouponType type;
    /** 满减券面额分；折扣券为 0 */
    private Long faceAmount;
    private Long minAmount;
    /** 折扣券：折扣率百分比（85=八五折），满减券为 0 */
    private Integer discountRate;
    /** 最高抵扣分，0 不限 */
    private Long maxDeductAmount;
    private Integer totalCount;
    private Integer takenCount;
    private LocalDateTime expireTime;
    private Integer status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public Integer getTakenCount() {
        return takenCount;
    }

    public void setTakenCount(Integer takenCount) {
        this.takenCount = takenCount;
    }

    public LocalDateTime getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(LocalDateTime expireTime) {
        this.expireTime = expireTime;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
