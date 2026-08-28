package com.wallet.pay.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import com.wallet.pay.state.PartState;
import com.wallet.contract.pay.enums.PayType;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 支付分段。pay_type：COUPON 券 / POINT 积分 / MONEY 余额 / CHANNEL 三方渠道。
 * 三方段的 part_no 即渠道交易号 outTradeNo。
 */
@TableName("pay_part")
@Schema(description = "支付分段")
public class PayPart {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键")
    private Long id;
    @Schema(description = "分段号（三方段即渠道 outTradeNo）")
    private String partNo;
    @Schema(description = "所属支付单号")
    private String orderNo;
    @Schema(description = "用户ID")
    private Long userId;
    @Schema(description = "分段类型")
    private PayType payType;
    @Schema(description = "本段抵扣金额，单位分")
    private Long amount;
    @Schema(description = "积分段：消耗积分数")
    private Long pointCount;
    @Schema(description = "券段：用户券 ID")
    private Long userCouponId;
    @Schema(description = "三方段：渠道编码")
    private String channelCode;
    @Schema(description = "三方段：渠道侧交易号")
    private String thirdNo;
    @Schema(description = "三方段：渠道支付参数")
    private String channelPayload;
    @Schema(description = "分段状态")
    private PartState state;
    @Schema(description = "本段已退金额，单位分")
    private Long refundedAmount;
    @Schema(description = "支付完成时间")
    private LocalDateTime payTime;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPartNo() {
        return partNo;
    }

    public void setPartNo(String partNo) {
        this.partNo = partNo;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public PayType getPayType() {
        return payType;
    }

    public void setPayType(PayType payType) {
        this.payType = payType;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public Long getPointCount() {
        return pointCount;
    }

    public void setPointCount(Long pointCount) {
        this.pointCount = pointCount;
    }

    public Long getUserCouponId() {
        return userCouponId;
    }

    public void setUserCouponId(Long userCouponId) {
        this.userCouponId = userCouponId;
    }

    public String getChannelCode() {
        return channelCode;
    }

    public void setChannelCode(String channelCode) {
        this.channelCode = channelCode;
    }

    public String getThirdNo() {
        return thirdNo;
    }

    public void setThirdNo(String thirdNo) {
        this.thirdNo = thirdNo;
    }

    public String getChannelPayload() {
        return channelPayload;
    }

    public void setChannelPayload(String channelPayload) {
        this.channelPayload = channelPayload;
    }

    public PartState getState() {
        return state;
    }

    public void setState(PartState state) {
        this.state = state;
    }

    public Long getRefundedAmount() {
        return refundedAmount;
    }

    public void setRefundedAmount(Long refundedAmount) {
        this.refundedAmount = refundedAmount;
    }

    public LocalDateTime getPayTime() {
        return payTime;
    }

    public void setPayTime(LocalDateTime payTime) {
        this.payTime = payTime;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
