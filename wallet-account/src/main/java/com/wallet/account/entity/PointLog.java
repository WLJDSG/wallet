package com.wallet.account.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 积分流水。biz_no + type 唯一，写流水是幂等操作。
 */
@TableName("point_log")
public class PointLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String bizNo;
    private String type;
    private Long changeCount;
    private Long afterCount;
    private String orderNo;
    private String remark;
    private LocalDateTime createTime;

    public PointLog() {
    }

    public PointLog(Long userId, String bizNo, String type, Long changeCount, Long afterCount, String orderNo,
        String remark) {
        this.userId = userId;
        this.bizNo = bizNo;
        this.type = type;
        this.changeCount = changeCount;
        this.afterCount = afterCount;
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

    public Long getChangeCount() {
        return changeCount;
    }

    public void setChangeCount(Long changeCount) {
        this.changeCount = changeCount;
    }

    public Long getAfterCount() {
        return afterCount;
    }

    public void setAfterCount(Long afterCount) {
        this.afterCount = afterCount;
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
