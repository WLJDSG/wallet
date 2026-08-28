package com.wallet.channel.serviceImpl.refund;
import com.wallet.channel.serviceImpl.support.AntomConfig;
import com.wallet.channel.serviceImpl.support.AntomClient;
import com.wallet.channel.serviceImpl.support.AbstractAntomAction;

import com.alipay.global.api.model.ResultStatusType;
import com.alipay.global.api.request.ams.pay.AlipayRefundRequest;
import com.alipay.global.api.response.ams.pay.AlipayRefundResponse;
import com.wallet.contract.channel.action.RefundAction;
import com.wallet.common.error.ErrorCode;
import com.wallet.contract.channel.error.ChannelException;
import com.wallet.contract.channel.model.ChannelRefundRequest;
import com.wallet.contract.channel.model.RefundResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Antom 退款动作（REFUND）：按 paymentId 原路退，金额为币种最小单位（分）。
 */
@Slf4j
@Component
public class AntomRefundAction extends AbstractAntomAction implements RefundAction {

    public AntomRefundAction(AntomClient client) {
        super(client);
    }

    @Override
    public RefundResult refund(ChannelRefundRequest request) {
        AntomConfig config = client.config();
        try {
            AlipayRefundRequest refundRequest = new AlipayRefundRequest();
            refundRequest.setRefundRequestId(request.refundOrderNo());
            refundRequest.setReferenceRefundId(request.refundOrderNo());
            refundRequest.setPaymentId(request.thirdOutTradeNo());
            refundRequest.setRefundReason("用户申请退款");
            refundRequest.setRefundAmount(client.amount(request.currency(), request.amount()));
            AlipayRefundResponse response = client.sdkClient(config).execute(refundRequest);
            if (!ResultStatusType.S.equals(response.getResult().getResultStatus())) {
                return RefundResult.fail(response.getResult().getResultMessage());
            }
            return RefundResult.ok(response.getRefundId());
        } catch (Exception e) {
            log.error("antom 退款异常, refundOrderNo={}", request.refundOrderNo(), e);
            throw new ChannelException(ErrorCode.CHANNEL_INVOKE_ERROR, "antom 退款异常: " + e.getMessage());
        }
    }
}
