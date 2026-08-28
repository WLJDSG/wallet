package com.wallet.pay.entity;

import com.wallet.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import com.wallet.common.enums.OrderState;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 支付主单。
 */
@TableName("pay_order")
@Schema(description = "支付主单")
public class PayOrder extends BaseEntity {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键")
    private Long id;
    @Schema(description = "钱包支付单号")
    private String orderNo;
    /** 来源商城/接入方（多商城预留，缺省 DEFAULT） */
    @Schema(description = "来源商城/接入方")
    private String appId;
    @Schema(description = "外部业务单号")
    private String bizOrderNo;
    @Schema(description = "用户ID")
    private Long userId;
    @Schema(description = "应付总额，单位分")
    private Long totalAmount;
    @Schema(description = "币种")
    private String currency;
    @Schema(description = "主单状态")
    private OrderState state;
    @Schema(description = "超时关单时间")
    private LocalDateTime expireTime;
    @Schema(description = "支付完成时间")
    private LocalDateTime payTime;
    @Schema(description = "关闭时间")
    private LocalDateTime closeTime;
    @Schema(description = "剩余可退金额，单位分")
    private Long refundableAmount;
    @Schema(description = "已退金额，单位分")
    private Long refundedAmount;
    @Schema(description = "失败原因")
    private String failReason;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getBizOrderNo() {
        return bizOrderNo;
    }

    public void setBizOrderNo(String bizOrderNo) {
        this.bizOrderNo = bizOrderNo;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public OrderState getState() {
        return state;
    }

    public void setState(OrderState state) {
        this.state = state;
    }

    public LocalDateTime getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(LocalDateTime expireTime) {
        this.expireTime = expireTime;
    }

    public LocalDateTime getPayTime() {
        return payTime;
    }

    public void setPayTime(LocalDateTime payTime) {
        this.payTime = payTime;
    }

    public LocalDateTime getCloseTime() {
        return closeTime;
    }

    public void setCloseTime(LocalDateTime closeTime) {
        this.closeTime = closeTime;
    }

    public Long getRefundableAmount() {
        return refundableAmount;
    }

    public void setRefundableAmount(Long refundableAmount) {
        this.refundableAmount = refundableAmount;
    }

    public Long getRefundedAmount() {
        return refundedAmount;
    }

    public void setRefundedAmount(Long refundedAmount) {
        this.refundedAmount = refundedAmount;
    }

    public String getFailReason() {
        return failReason;
    }

    public void setFailReason(String failReason) {
        this.failReason = failReason;
    }
}
