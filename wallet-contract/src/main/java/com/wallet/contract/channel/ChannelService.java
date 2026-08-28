package com.wallet.contract.channel;

import com.wallet.contract.channel.enums.ActionType;
import com.wallet.contract.channel.model.CallbackRequest;
import com.wallet.contract.channel.model.ConfirmRequest;
import com.wallet.contract.channel.model.PayRequest;
import com.wallet.contract.channel.model.PayResult;
import com.wallet.contract.channel.model.QueryRequest;
import com.wallet.contract.channel.model.RefundRequest;

import java.util.Set;

/**
 * 渠道内核操作契约。由 {@code wallet-channel} 的 {@code ChannelServiceImpl} 实现，
 * 支付编排经此调用内核的支付/退款/查询/取消/确认等全部能力。
 *
 * <p>注意：内核不加锁，调用各方法前必须持有该支付单的分布式锁。</p>
 */
public interface ChannelService {

    /** 发起支付（按渠道 + 动作分发到具体渠道实现）。 */
    PayResult pay(PayRequest request);

    /** 处理渠道异步回调。返回渠道要求的 ack 报文。 */
    String callback(CallbackRequest request);

    /** 主动向渠道查证支付结果（true=已支付）。 */
    boolean query(QueryRequest request);

    /** 发起退款。 */
    boolean refund(RefundRequest request);

    /** 关单。 */
    boolean cancel(String channelCode, String orderNo, String outTradeNo);

    /** 确认（补充确认类渠道）。 */
    boolean confirm(ConfirmRequest request);

    /** 渠道是否支持某动作（可据此裁剪前端能力，如是否展示"取消支付"入口）。 */
    boolean supports(String channelCode, ActionType action);

    /** 已注册渠道编码（有序）。 */
    Set<String> channelCodes();
}
