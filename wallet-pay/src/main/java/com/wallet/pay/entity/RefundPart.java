package com.wallet.pay.entity;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "退款分段（按支付分段分摊）")
public class RefundPart {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键")
    private Long id;
    @Schema(description = "退款分段号（兼资产流水幂等号）")
    private String refundPartNo;
    @Schema(description = "所属退款单号")
    private String refundNo;
    @Schema(description = "对应支付分段号")
    private String partNo;
    @Schema(description = "分段类型")
    private PayType payType;
    @Schema(description = "本段退款金额，单位分")
    private Long amount;
    @Schema(description = "退还积分数")
    private Long pointCount;
    @Schema(description = "渠道侧退款单号")
    private String channelRefundNo;
    @Schema(description = "退款分段状态")
    private RefundState state;
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
