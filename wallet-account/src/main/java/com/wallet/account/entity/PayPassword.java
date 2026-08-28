package com.wallet.account.entity;

import com.wallet.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 支付密码。password_hash 为 BCrypt 慢哈希，不存明文。
 * status：ENABLED / DISABLED；version 改密自增（可用于踢旧票据）。
 */
@TableName("pay_password")
public class PayPassword extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String passwordHash;
    private String status;
    private Integer version;

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

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
