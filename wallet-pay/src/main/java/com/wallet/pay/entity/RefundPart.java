package com.wallet.pay.entity;

import com.wallet.pay.enums.PayType;
import com.wallet.channel.enums.RefundState;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 退款分段明细。refund_part_no 同时是资产流水幂等号（biz_no）。
 * 三方段状态 INIT→REFUNDING→SUCCESS/FAIL；资产段 INIT→SUCCESS/FAIL。
 */
@TableName("refund_part")
public class RefundPart {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String refundPartNo;
    private String refundNo;
    private String partNo;
    private PayType payType;
    private Long amount;
    private Long pointCount;
    private String channelRefundNo;
    private RefundState state;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRefundPartNo() {
        return refundPartNo;
    }

    public void setRefundPartNo(String refundPartNo) {
        this.refundPartNo = refundPartNo;
    }

    public String getRefundNo() {
        return refundNo;
    }

    public void setRefundNo(String refundNo) {
        this.refundNo = refundNo;
    }

    public String getPartNo() {
        return partNo;
    }

    public void setPartNo(String partNo) {
        this.partNo = partNo;
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

    public String getChannelRefundNo() {
        return channelRefundNo;
    }

    public void setChannelRefundNo(String channelRefundNo) {
        this.channelRefundNo = channelRefundNo;
    }

    public RefundState getState() {
        return state;
    }

    public void setState(RefundState state) {
        this.state = state;
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
