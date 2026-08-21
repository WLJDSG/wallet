package com.wallet.pay.antom;

import com.alipay.global.api.AlipayClient;
import com.alipay.global.api.DefaultAlipayClient;
import com.alipay.global.api.model.ResultStatusType;
import com.alipay.global.api.model.ams.Amount;
import com.alipay.global.api.model.ams.Order;
import com.alipay.global.api.model.ams.ProductCodeType;
import com.alipay.global.api.model.ams.TransactionStatusType;
import com.alipay.global.api.request.ams.pay.AlipayPayQueryRequest;
import com.alipay.global.api.request.ams.pay.AlipayPayRequest;
import com.alipay.global.api.request.ams.pay.AlipayRefundRequest;
import com.alipay.global.api.response.ams.pay.AlipayPayQueryResponse;
import com.alipay.global.api.response.ams.pay.AlipayPayResponse;
import com.alipay.global.api.response.ams.pay.AlipayRefundResponse;
import com.alipay.global.api.tools.WebhookTool;
import com.wallet.channel.action.CallbackAction;
import com.wallet.channel.action.CancelAction;
import com.wallet.channel.action.PayAction;
import com.wallet.channel.action.QueryAction;
import com.wallet.channel.action.RefundAction;
import com.wallet.channel.enums.PayError;
import com.wallet.channel.error.ChannelException;
import com.wallet.channel.model.CallbackRequest;
import com.wallet.channel.model.CallbackResult;
import com.wallet.channel.model.CancelRequest;
import com.wallet.channel.model.ChannelRefundRequest;
import com.wallet.channel.model.PayRequest;
import com.wallet.channel.model.QueryRequest;
import com.wallet.channel.model.QueryResult;
import com.wallet.channel.model.RefundResult;
import com.wallet.channel.model.TradeInfo;
import com.wallet.pay.config.AntomProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Antom（支付宝国际）真实渠道示范。
 *
 * <p>覆盖 PAY / QUERY / REFUND / CANCEL / CALLBACK 五个动作：</p>
 * <ul>
 *   <li>PAY：收银台支付（CASHIER_PAYMENT），paymentRequestId = 钱包分段号 partNo；</li>
 *   <li>CALLBACK：WebhookTool 验签（header request-time/client-id/signature + 原始 body），
 *       声明已支付时 reQueryRequired=true 以主动查询为准；</li>
 *   <li>QUERY：按 paymentRequestId 查渠道侧状态；</li>
 *   <li>REFUND：按 paymentId 原路退，金额为币种最小单位（分）；</li>
 *   <li>CANCEL：Antom 无远程关单接口，支付到点自动过期，本地直接返回 true。</li>
 * </ul>
 *
 * <p>需在 application.yml 配置 {@code wallet.antom.enabled=true} 及商户密钥，否则本渠道不注册。</p>
 */
@Component
@ConditionalOnProperty(prefix = "wallet.antom", name = "enabled", havingValue = "true")
public class AntomChannel implements PayAction, QueryAction, RefundAction, CancelAction, CallbackAction {

    private static final Logger log = LoggerFactory.getLogger(AntomChannel.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final DateTimeFormatter ISO_8601 =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private final AntomProperties props;
    private volatile AlipayClient client;

    public AntomChannel(AntomProperties props) {
        this.props = props;
        if (props.getClientId() == null || props.getClientId().trim().isEmpty()
            || props.getMerchantPrivateKey() == null || props.getMerchantPrivateKey().trim().isEmpty()
            || props.getAlipayPublicKey() == null || props.getAlipayPublicKey().trim().isEmpty()) {
            throw new IllegalStateException(
                "wallet.antom.enabled=true 但 clientId/merchantPrivateKey/alipayPublicKey 未配置");
        }
    }

    @Override
    public String code() {
        return "ANTOM";
    }

    @Override
    public Object pay(PayRequest request, TradeInfo trade) {
        try {
            AlipayPayRequest payRequest = new AlipayPayRequest();
            payRequest.setProductCode(ProductCodeType.CASHIER_PAYMENT);
            payRequest.setPaymentRequestId(trade.outTradeNo());

            Amount amount = amount(request.currency(), request.amount());
            payRequest.setPaymentAmount(amount);

            Order order = new Order();
            order.setReferenceOrderId(trade.outTradeNo());
            order.setOrderDescription("wallet order " + request.orderNo());
            order.setOrderAmount(amount);
            payRequest.setOrder(order);

            payRequest.setPaymentExpiryTime(expiryTime());
            payRequest.setPaymentNotifyUrl(props.getBaseUrl()
                + "/api/pay/callback/ANTOM/" + request.orderNo() + "/" + trade.outTradeNo());
            payRequest.setPaymentRedirectUrl(props.getBaseUrl() + "/wallet/pay/result/" + request.orderNo());

            AlipayPayResponse response = client().execute(payRequest);
            if (!ResultStatusType.S.equals(response.getResult().getResultStatus())
                && !(ResultStatusType.U.equals(response.getResult().getResultStatus())
                    && "PAYMENT_IN_PROCESS".equals(response.getResult().getResultCode()))) {
                throw new ChannelException(PayError.CHANNEL_INVOKE_ERROR,
                    "antom 下单失败: " + response.getResult().getResultMessage());
            }
            return new AntomPayload(response.getNormalUrl(), response.getSchemeUrl(), response.getApplinkUrl());
        } catch (ChannelException e) {
            throw e;
        } catch (Exception e) {
            log.error("antom 下单异常, outTradeNo={}", trade.outTradeNo(), e);
            throw new ChannelException(PayError.CHANNEL_INVOKE_ERROR, "antom 下单异常: " + e.getMessage());
        }
    }

    @Override
    public QueryResult query(QueryRequest request) {
        try {
            AlipayPayQueryRequest queryRequest = new AlipayPayQueryRequest();
            queryRequest.setPaymentRequestId(request.outTradeNo());
            AlipayPayQueryResponse response = client().execute(queryRequest);
            if (!ResultStatusType.S.equals(response.getResult().getResultStatus())) {
                throw new ChannelException(PayError.CHANNEL_INVOKE_ERROR,
                    "antom 查询失败: " + response.getResult().getResultMessage());
            }
            boolean paid = TransactionStatusType.SUCCESS.equals(response.getPaymentStatus());
            return new QueryResult(paid, paid ? response.getPaymentId() : null);
        } catch (ChannelException e) {
            throw e;
        } catch (Exception e) {
            log.error("antom 查询异常, outTradeNo={}", request.outTradeNo(), e);
            throw new ChannelException(PayError.CHANNEL_INVOKE_ERROR, "antom 查询异常: " + e.getMessage());
        }
    }

    @Override
    public RefundResult refund(ChannelRefundRequest request) {
        try {
            AlipayRefundRequest refundRequest = new AlipayRefundRequest();
            refundRequest.setRefundRequestId(request.refundOrderNo());
            refundRequest.setReferenceRefundId(request.refundOrderNo());
            refundRequest.setPaymentId(request.thirdOutTradeNo());
            refundRequest.setRefundReason("用户申请退款");
            refundRequest.setRefundAmount(amount(request.currency(), request.amount()));
            AlipayRefundResponse response = client().execute(refundRequest);
            if (!ResultStatusType.S.equals(response.getResult().getResultStatus())) {
                return RefundResult.fail(response.getResult().getResultMessage());
            }
            return RefundResult.ok(response.getRefundId());
        } catch (Exception e) {
            log.error("antom 退款异常, refundOrderNo={}", request.refundOrderNo(), e);
            throw new ChannelException(PayError.CHANNEL_INVOKE_ERROR, "antom 退款异常: " + e.getMessage());
        }
    }

    @Override
    public boolean cancel(CancelRequest request) {
        // Antom 无远程关单接口，支付到点自动过期，本地侧直接视为关闭成功
        return true;
    }

    @Override
    public CallbackResult onCallback(CallbackRequest request) {
        verifySignature(request);
        try {
            JsonNode root = MAPPER.readTree(request.body());
            String notifyType = root.path("notifyType").asText();
            String paymentId = root.path("paymentId").asText();
            String resultStatus = root.path("result").path("resultStatus").asText();
            boolean paid = "PAYMENT_RESULT".equals(notifyType)
                && "S".equals(resultStatus);
            if (paid) {
                return CallbackResult.builder()
                    .paid(true)
                    .thirdOutTradeNo(paymentId)
                    .reQueryRequired(true) // 回调报文不可全信，以主动查询为准
                    .ackBody(successAck())
                    .build();
            }
            return CallbackResult.builder().paid(false).ackBody(successAck()).build();
        } catch (ChannelException e) {
            throw e;
        } catch (Exception e) {
            log.error("antom 回调解析异常, body={}", request.body(), e);
            throw new ChannelException(PayError.CALLBACK_VERIFY_FAILED, "antom 回调报文解析失败");
        }
    }

    private void verifySignature(CallbackRequest request) {
        String requestTime = header(request, "request-time");
        String clientId = header(request, "client-id");
        String signature = header(request, "signature");
        try {
            boolean ok = WebhookTool.checkSignature(request.requestUri(), request.httpMethod(), clientId,
                requestTime, signature, request.body(), props.getAlipayPublicKey());
            if (!ok) {
                log.error("antom 验签失败, uri={}, clientId={}, requestTime={}", request.requestUri(), clientId,
                    requestTime);
                throw new ChannelException(PayError.CALLBACK_VERIFY_FAILED, "antom 回调验签失败");
            }
        } catch (ChannelException e) {
            throw e;
        } catch (Exception e) {
            log.error("antom 验签异常, uri={}", request.requestUri(), e);
            throw new ChannelException(PayError.CALLBACK_VERIFY_FAILED, "antom 回调验签异常: " + e.getMessage());
        }
    }

    private String header(CallbackRequest request, String name) {
        return request.headers() == null ? null : request.headers().get(name);
    }

    private Amount amount(String currency, long minor) {
        Amount amount = new Amount();
        amount.setCurrency(currency);
        amount.setValue(String.valueOf(minor)); // 钱包金额统一为分（币种最小单位）
        return amount;
    }

    private String expiryTime() {
        return ZonedDateTime.now(ZoneOffset.UTC).plusMinutes(props.getExpiryMinutes())
            .format(ISO_8601);
    }

    private String successAck() {
        return "{\"result\":{\"resultCode\":\"SUCCESS\",\"resultMessage\":\"success\",\"resultStatus\":\"S\"}}";
    }

    private AlipayClient client() {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    client = new DefaultAlipayClient(props.getGateway(), props.getMerchantPrivateKey(),
                        props.getAlipayPublicKey(), props.getClientId());
                }
            }
        }
        return client;
    }
}
