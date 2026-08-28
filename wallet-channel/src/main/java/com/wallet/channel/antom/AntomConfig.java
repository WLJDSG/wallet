package com.wallet.channel.antom;

/**
 * Antom 渠道配置（channel_config 表 ANTOM 行的 config_json 结构）。
 *
 * @param gateway            网关地址（如 https://globalapi.alipay.com 或沙箱）
 * @param clientId           商户 clientId
 * @param merchantPrivateKey 商户私钥（RSA PKCS1 PEM）
 * @param alipayPublicKey    支付宝公钥（回调验签用）
 * @param baseUrl            应用公网基地址（拼回调 notifyUrl）
 * @param expiryMinutes      渠道侧支付过期分钟数
 */
public record AntomConfig(String gateway, String clientId, String merchantPrivateKey,
                          String alipayPublicKey, String baseUrl, Integer expiryMinutes) {

    private static final String DEFAULT_GATEWAY = "https://globalapi.alipay.com";

    /** 必填项校验（缺失说明库里模板没填完） */
    public void validate() {
        if (isBlank(clientId) || isBlank(merchantPrivateKey) || isBlank(alipayPublicKey) || isBlank(baseUrl)) {
            throw new IllegalStateException(
                "channel_config 的 ANTOM 配置不完整：clientId/merchantPrivateKey/alipayPublicKey/baseUrl 必填");
        }
    }

    public String gatewayOrDefault() {
        return isBlank(gateway) ? DEFAULT_GATEWAY : gateway;
    }

    public int expiryMinutesOrDefault() {
        return expiryMinutes == null || expiryMinutes <= 0 ? 10 : expiryMinutes;
    }

    /** SDK client 缓存键：配置变了要重建 client */
    public String clientKey() {
        return gatewayOrDefault() + '|' + clientId + '|'
            + (merchantPrivateKey == null ? 0 : merchantPrivateKey.hashCode()) + '|'
            + (alipayPublicKey == null ? 0 : alipayPublicKey.hashCode());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
