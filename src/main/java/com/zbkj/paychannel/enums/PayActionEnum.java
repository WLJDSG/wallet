package com.zbkj.paychannel.enums;

/**
 * 支付动作类型。
 *
 * <p>渠道通过实现对应的 Provider 接口声明自己支持哪些动作；
 * 渠道未实现某动作时，注册表会在调用处抛出明确异常而非 NPE。</p>
 */
public enum PayActionEnum {

    /** 发起支付（必须实现） */
    PAY,

    /** 主动查询支付结果（未实现的渠道不参与轮询兜底） */
    QUERY,

    /** 退款 */
    REFUND,

    /** 关闭/取消未支付交易 */
    CANCEL,

    /** 异步回调解析与验签 */
    CALLBACK,

    /** 二段式扣款（如 PayPal execute payment） */
    EXECUTE_PAYMENT
}
