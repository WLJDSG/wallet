package com.wallet.pay.entity;

import com.wallet.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import com.wallet.common.enums.RefundOrderState;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 退款主单。
 */
@TableName("refund_order")
@Schema(description = "退款主单")
public class RefundOrder extends BaseEntity {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键")
    private Long id;
    @Schema(description = "退款单号")
    private String refundNo;
    @Schema(description = "原支付单号")
    private String orderNo;
    @Schema(description = "用户ID")
    private Long userId;
    @Schema(description = "申请退款总额，单位分")
    private Long refundAmount;
    @Schema(description = "退还积分数")
    private Long refundPoint;
    @Schema(description = "是否返还券：1 是 0 否")
    private Integer couponBack;
    @Schema(description = "退款单状态")
    private RefundOrderState state;
    @Schema(description = "退款原因")
    private String reason;
    @Schema(description = "完成时间")
    private LocalDateTime finishTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRefundNo() {
        return refundNo;
    }

    public void setRefundNo(String refundNo) {
        this.refundNo = refundNo;
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

    public Long getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(Long refundAmount) {
        this.refundAmount = refundAmount;
    }

    public Long getRefundPoint() {
        return refundPoint;
    }

    public void setRefundPoint(Long refundPoint) {
        this.refundPoint = refundPoint;
    }

    public Integer getCouponBack() {
        return couponBack;
    }

    public void setCouponBack(Integer couponBack) {
        this.couponBack = couponBack;
    }

    public RefundOrderState getState() {
        return state;
    }

    public void setState(RefundOrderState state) {
        this.state = state;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(LocalDateTime finishTime) {
        this.finishTime = finishTime;
    }
}
