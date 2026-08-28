package com.wallet.channel.serviceImpl.query;
import com.wallet.channel.serviceImpl.support.AntomConfig;
import com.wallet.channel.serviceImpl.support.AntomClient;
import com.wallet.channel.serviceImpl.support.AbstractAntomAction;

import com.alipay.global.api.model.ResultStatusType;
import com.alipay.global.api.model.ams.TransactionStatusType;
import com.alipay.global.api.request.ams.pay.AlipayPayQueryRequest;
import com.alipay.global.api.response.ams.pay.AlipayPayQueryResponse;
import com.wallet.contract.channel.action.QueryAction;
import com.wallet.common.error.ErrorCode;
import com.wallet.contract.channel.error.ChannelException;
import com.wallet.contract.channel.model.QueryRequest;
import com.wallet.contract.channel.model.QueryResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Antom 查询动作（QUERY）：按 paymentRequestId 查渠道侧状态。
 */
@Slf4j
@Component
public class AntomQueryAction extends AbstractAntomAction implements QueryAction {

    public AntomQueryAction(AntomClient client) {
        super(client);
    }

    @Override
    public QueryResult query(QueryRequest request) {
        AntomConfig config = client.config();
        try {
            AlipayPayQueryRequest queryRequest = new AlipayPayQueryRequest();
            queryRequest.setPaymentRequestId(request.outTradeNo());
            AlipayPayQueryResponse response = client.sdkClient(config).execute(queryRequest);
            if (!ResultStatusType.S.equals(response.getResult().getResultStatus())) {
                throw new ChannelException(ErrorCode.CHANNEL_INVOKE_ERROR,
                    "antom 查询失败: " + response.getResult().getResultMessage());
            }
            boolean paid = TransactionStatusType.SUCCESS.equals(response.getPaymentStatus());
            return new QueryResult(paid, paid ? response.getPaymentId() : null);
        } catch (ChannelException e) {
            throw e;
        } catch (Exception e) {
            log.error("antom 查询异常, outTradeNo={}", request.outTradeNo(), e);
            throw new ChannelException(ErrorCode.CHANNEL_INVOKE_ERROR, "antom 查询异常: " + e.getMessage());
        }
    }
}
