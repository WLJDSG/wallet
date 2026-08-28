package com.wallet.pay.entity;

import com.wallet.common.entity.BaseEntity;
import java.time.LocalDateTime;

/**
 * 支付渠道配置（channel_config）：商户密钥等敏感配置落库，改库即生效（缓存 TTL 30 秒）。
 */
public class ChannelConfig extends BaseEntity {

    private Long id;
    private String channelCode;
    /** 1 启用 0 停用 */
    private Integer enabled;
    /** 渠道自定义配置 JSON（字段由各渠道的 *Config record 定义） */
    private String configJson;
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getChannelCode() {
        return channelCode;
    }

    public void setChannelCode(String channelCode) {
        this.channelCode = channelCode;
    }

    public Integer getEnabled() {
        return enabled;
    }

    public void setEnabled(Integer enabled) {
        this.enabled = enabled;
    }

    public String getConfigJson() {
        return configJson;
    }

    public void setConfigJson(String configJson) {
        this.configJson = configJson;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
