package com.wallet.channel.serviceImpl.support;

import com.alipay.global.api.AlipayClient;
import com.alipay.global.api.DefaultAlipayClient;
import com.alipay.global.api.model.ams.Amount;
import com.alipay.global.api.tools.WebhookTool;
import com.wallet.contract.channel.ChannelConfigService;
import com.wallet.common.error.ErrorCode;
import com.wallet.contract.channel.error.ChannelException;
import com.wallet.contract.channel.model.CallbackRequest;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Antom（支付宝国际）SDK 客户端与配置加载（渠道内共享）。
 * 各动作类经此取配置、构建/缓存 SDK client、做金额/过期/验签等公共处理。
 *
 * <p>配置从 channel_config 表读取（{@link AntomConfig}），不写 yml：
 * 每次动作取当前配置（30 秒缓存），配置变更自动重建 SDK client；未配置/停用时抛渠道异常。</p>
 */
@Component
public class AntomClient {

    public static final String CHANNEL_CODE = "ANTOM";

    private static final DateTimeFormatter ISO_8601 =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private final ChannelConfigService channelConfigs;
    private volatile CachedClient cachedClient;

    public AntomClient(ChannelConfigService channelConfigs) {
        this.channelConfigs = channelConfigs;
    }

    /** 取当前配置（channel_config 表，30 秒缓存），未启用/不完整抛渠道异常 */
    public AntomConfig config() {
        AntomConfig config = channelConfigs.requireEnabled(CHANNEL_CODE, AntomConfig.class);
        try {
            config.validate();
        } catch (IllegalStateException e) {
            throw new ChannelException(ErrorCode.CHANNEL_INVOKE_ERROR, e.getMessage());
        }
        return config;
    }

    /** SDK client 按配置键缓存，配置变更（换密钥/网关）自动重建 */
    public AlipayClient sdkClient(AntomConfig config) {
        String key = config.clientKey();
        CachedClient cached = cachedClient;
        if (cached == null || !cached.key().equals(key)) {
            synchronized (this) {
                cached = cachedClient;
                if (cached == null || !cached.key().equals(key)) {
                    cached = new CachedClient(key, new DefaultAlipayClient(config.gatewayOrDefault(),
                        config.merchantPrivateKey(), config.alipayPublicKey(), config.clientId()));
                    cachedClient = cached;
                }
            }
        }
        return cached.client();
    }

    /** 金额转换：钱包金额统一为分（币种最小单位） */
    public Amount amount(String currency, long minor) {
        Amount amount = new Amount();
        amount.setCurrency(currency);
        amount.setValue(String.valueOf(minor));
        return amount;
    }

    public String expiryTime(AntomConfig config) {
        return ZonedDateTime.now(ZoneOffset.UTC).plusMinutes(config.expiryMinutesOrDefault())
            .format(ISO_8601);
    }

    /** Antom Webhook 验签（header request-time/client-id/signature + 原始 body） */
    public void verifySignature(CallbackRequest request, AntomConfig config) {
        String requestTime = header(request, "request-time");
        String clientId = header(request, "client-id");
        String signature = header(request, "signature");
        try {
            boolean ok = WebhookTool.checkSignature(request.requestUri(), request.httpMethod(), clientId,
                requestTime, signature, request.body(), config.alipayPublicKey());
            if (!ok) {
                throw new ChannelException(ErrorCode.CALLBACK_VERIFY_FAILED, "antom 回调验签失败");
            }
        } catch (ChannelException e) {
            throw e;
        } catch (Exception e) {
            throw new ChannelException(ErrorCode.CALLBACK_VERIFY_FAILED, "antom 回调验签异常: " + e.getMessage());
        }
    }

    public String successAck() {
        return "{\"result\":{\"resultCode\":\"SUCCESS\",\"resultMessage\":\"success\",\"resultStatus\":\"S\"}}";
    }

    private String header(CallbackRequest request, String name) {
        return request.headers() == null ? null : request.headers().get(name);
    }

    /** 按配置键缓存的 SDK client */
    private record CachedClient(String key, AlipayClient client) {
    }
}
