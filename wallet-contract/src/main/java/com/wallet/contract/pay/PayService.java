package com.wallet.contract.pay;

import com.wallet.contract.pay.model.CreateOrderCmd;
import com.wallet.contract.pay.model.CreateOrderResult;
import com.wallet.contract.pay.model.OrderDetail;
import com.wallet.contract.pay.model.SubmitResult;

import java.util.Map;

/**
 * 支付编排契约。由 {@code wallet-pay} 的 {@code PayServiceImpl} 实现，Web 层经此调用。
 * 覆盖创建拆分支付单 / 提交 / 渠道回调 / 详情 / 主动查证 / 取消。
 */
public interface PayService {

    /** 创建拆分支付单：校验分段合法性与金额勾稽；同 appId 同 bizOrderNo 幂等返回既有单。 */
    CreateOrderResult create(String appId, Long userId, CreateOrderCmd cmd);

    /** 提交支付：含资产段必须携带密码票据；扣资产段（事务）+ 发起三方（事务外）。 */
    SubmitResult submit(Long userId, String orderNo, String ticket);

    /** 处理渠道异步回调（持单锁，内核不重复加锁），返回渠道要求的应答报文。 */
    String handleCallback(String channelCode, String orderNo, String partNo, String body,
        Map<String, String> headers, String httpMethod, String requestUri);

    /** 查支付单详情（主单 + 全部分段）。 */
    OrderDetail detail(Long userId, String orderNo);

    /** 主动向渠道查证（持单锁），查证已支付会顺势结单。 */
    boolean query(Long userId, String orderNo);

    /** 取消支付：未支付→关渠道+回滚资产+关单；已实付→补单完成并提示已支付。 */
    String cancel(Long userId, String orderNo);
}
