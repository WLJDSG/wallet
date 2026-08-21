package com.wallet.pay.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Antom（支付宝国际）渠道配置（application.yml 的 wallet.antom.*）。
 * enabled=false（默认）时渠道不注册，应用照常启动；配好密钥后置 true 即启用。
 */
@ConfigurationProperties(prefix = "wallet.antom")
public class AntomProperties {

    /** 是否启用 Antom 渠道（需配好商户密钥） */
    private boolean enabled = false;

    /** 网关地址（如 https://globalapi.alipay.com 或沙箱） */
    private String gateway = "https://globalapi.alipay.com";

    /** 商户 clientId */
    private String clientId;

    /** 商户私钥（RSA PKCS1 PEM） */
    private String merchantPrivateKey;

    /** 支付宝公钥（用于回调验签） */
    private String alipayPublicKey;

    /** 应用公网访问基地址（拼回调 notifyUrl 用，如 https://pay.example.com） */
    private String baseUrl = "http://localhost:8080";

    /** 支付过期分钟数（渠道侧） */
    private int expiryMinutes = 10;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getMerchantPrivateKey() {
        return merchantPrivateKey;
    }

    public void setMerchantPrivateKey(String merchantPrivateKey) {
        this.merchantPrivateKey = merchantPrivateKey;
    }

    public String getAlipayPublicKey() {
        return alipayPublicKey;
    }

    public void setAlipayPublicKey(String alipayPublicKey) {
        this.alipayPublicKey = alipayPublicKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int getExpiryMinutes() {
        return expiryMinutes;
    }

    public void setExpiryMinutes(int expiryMinutes) {
        this.expiryMinutes = expiryMinutes;
    }
}
