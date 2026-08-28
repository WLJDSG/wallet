package com.wallet.account.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 余额流水。biz_no + type 唯一，写流水是幂等操作。
 */
@TableName("money_log")
public class MoneyLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String bizNo;
    private String type;
    private Long changeAmount;
    private Long afterAmount;
    private String orderNo;
    private String remark;
    private LocalDateTime createTime;

    public MoneyLog() {
    }

    public MoneyLog(Long userId, String bizNo, String type, Long changeAmount, Long afterAmount, String orderNo,
        String remark) {
        this.userId = userId;
        this.bizNo = bizNo;
        this.type = type;
        this.changeAmount = changeAmount;
        this.afterAmount = afterAmount;
        this.orderNo = orderNo;
        this.remark = remark;
    }

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

    public String getBizNo() {
        return bizNo;
    }

    public void setBizNo(String bizNo) {
        this.bizNo = bizNo;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getChangeAmount() {
        return changeAmount;
    }

    public void setChangeAmount(Long changeAmount) {
        this.changeAmount = changeAmount;
    }

    public Long getAfterAmount() {
        return afterAmount;
    }

    public void setAfterAmount(Long afterAmount) {
        this.afterAmount = afterAmount;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
