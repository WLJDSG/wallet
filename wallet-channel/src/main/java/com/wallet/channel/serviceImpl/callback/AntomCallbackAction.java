package com.wallet.channel.serviceImpl.callback;
import com.wallet.channel.serviceImpl.support.AntomConfig;
import com.wallet.channel.serviceImpl.support.AntomClient;
import com.wallet.channel.serviceImpl.support.AbstractAntomAction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.contract.channel.action.CallbackAction;
import com.wallet.common.error.ErrorCode;
import com.wallet.contract.channel.error.ChannelException;
import com.wallet.contract.channel.model.CallbackRequest;
import com.wallet.contract.channel.model.CallbackResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Antom 回调动作（CALLBACK）：WebhookTool 验签 + 解析报文；
 * 声明已支付时 reQueryRequired=true 以主动查询为准。
 */
@Slf4j
@Component
public class AntomCallbackAction extends AbstractAntomAction implements CallbackAction {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public AntomCallbackAction(AntomClient client) {
        super(client);
    }

    @Override
    public CallbackResult onCallback(CallbackRequest request) {
        AntomConfig config = client.config();
        client.verifySignature(request, config);
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
                    .ackBody(client.successAck())
                    .build();
            }
            return CallbackResult.builder().paid(false).ackBody(client.successAck()).build();
        } catch (ChannelException e) {
            throw e;
        } catch (Exception e) {
            log.error("antom 回调解析异常, body={}", request.body(), e);
            throw new ChannelException(ErrorCode.CALLBACK_VERIFY_FAILED, "antom 回调报文解析失败");
        }
    }
}
