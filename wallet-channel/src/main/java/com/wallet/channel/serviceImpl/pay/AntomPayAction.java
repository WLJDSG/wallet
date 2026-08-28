package com.wallet.channel.serviceImpl.pay;
import com.wallet.channel.serviceImpl.support.AntomPayload;
import com.wallet.channel.serviceImpl.support.AntomConfig;
import com.wallet.channel.serviceImpl.support.AntomClient;
import com.wallet.channel.serviceImpl.support.AbstractAntomAction;

import com.alipay.global.api.model.ams.Amount;
import com.alipay.global.api.model.ams.Order;
import com.alipay.global.api.model.ams.ProductCodeType;
import com.alipay.global.api.model.ResultStatusType;
import com.alipay.global.api.request.ams.pay.AlipayPayRequest;
import com.alipay.global.api.response.ams.pay.AlipayPayResponse;
import com.wallet.contract.channel.action.PayAction;
import com.wallet.common.error.ErrorCode;
import com.wallet.contract.channel.error.ChannelException;
import com.wallet.contract.channel.model.PayRequest;
import com.wallet.contract.channel.model.TradeInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Antom 支付动作（PAY）：收银台支付（CASHIER_PAYMENT），paymentRequestId = 钱包分段号。
 */
@Slf4j
@Component
public class AntomPayAction extends AbstractAntomAction implements PayAction {

    public AntomPayAction(AntomClient client) {
        super(client);
    }

    @Override
    public Object pay(PayRequest request, TradeInfo trade) {
        AntomConfig config = client.config();
        try {
            AlipayPayRequest payRequest = new AlipayPayRequest();
            payRequest.setProductCode(ProductCodeType.CASHIER_PAYMENT);
            payRequest.setPaymentRequestId(trade.outTradeNo());

            Amount amount = client.amount(request.currency(), request.amount());
            payRequest.setPaymentAmount(amount);

            Order order = new Order();
            order.setReferenceOrderId(trade.outTradeNo());
            order.setOrderDescription("wallet order " + request.orderNo());
            order.setOrderAmount(amount);
            payRequest.setOrder(order);

            payRequest.setPaymentExpiryTime(client.expiryTime(config));
            payRequest.setPaymentNotifyUrl(config.baseUrl()
                + "/api/pay/callback/ANTOM/" + request.orderNo() + "/" + trade.outTradeNo());
            payRequest.setPaymentRedirectUrl(config.baseUrl() + "/wallet/pay/result/" + request.orderNo());

            AlipayPayResponse response = client.sdkClient(config).execute(payRequest);
            if (!ResultStatusType.S.equals(response.getResult().getResultStatus())
                && !(ResultStatusType.U.equals(response.getResult().getResultStatus())
                    && "PAYMENT_IN_PROCESS".equals(response.getResult().getResultCode()))) {
                throw new ChannelException(ErrorCode.CHANNEL_INVOKE_ERROR,
                    "antom 下单失败: " + response.getResult().getResultMessage());
            }
            return new AntomPayload(response.getNormalUrl(), response.getSchemeUrl(), response.getApplinkUrl());
        } catch (ChannelException e) {
            throw e;
        } catch (Exception e) {
            log.error("antom 下单异常, outTradeNo={}", trade.outTradeNo(), e);
            throw new ChannelException(ErrorCode.CHANNEL_INVOKE_ERROR, "antom 下单异常: " + e.getMessage());
        }
    }
}
