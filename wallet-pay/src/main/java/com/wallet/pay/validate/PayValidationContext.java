package com.wallet.pay.validate;

import com.wallet.pay.entity.PayOrder;
import com.wallet.pay.entity.PayPart;
import com.wallet.contract.pay.model.CreateOrderCmd;

import java.util.List;

/**
 * 支付域校验上下文：按场景携带所需数据（不适用的字段为 null），由静态工厂构造。
 *
 * @param scene        校验场景
 * @param appId        来源商城/接入方（CREATE）
 * @param userId       用户ID
 * @param orderNo      支付单号（除 CREATE 外）
 * @param cmd          创建命令（CREATE）
 * @param order        支付主单（可为 null=不存在，由归属校验器判定）
 * @param parts        支付分段（SUBMIT）
 * @param ticket       密码、生物识别或二次确认签发的支付授权票据（SUBMIT）
 * @param refundAmount 申请退款金额（REFUND_CREATE）
 */
public record PayValidationContext(PayScene scene, String appId, Long userId, String orderNo,
                                   CreateOrderCmd cmd, PayOrder order, List<PayPart> parts,
                                   String ticket, Long refundAmount) {

    public static PayValidationContext forCreate(String appId, Long userId, CreateOrderCmd cmd) {
        return new PayValidationContext(PayScene.CREATE, appId, userId, null, cmd, null, null, null, null);
    }

    public static PayValidationContext forSubmit(Long userId, String orderNo, PayOrder order,
        List<PayPart> parts, String ticket) {
        return new PayValidationContext(PayScene.SUBMIT, null, userId, orderNo, null, order, parts,
            ticket, null);
    }

    /** DETAIL / QUERY / CANCEL 等只需归属校验的场景 */
    public static PayValidationContext forOrder(PayScene scene, Long userId, String orderNo, PayOrder order) {
        return new PayValidationContext(scene, null, userId, orderNo, null, order, null, null, null);
    }

    public static PayValidationContext forRefund(Long userId, String orderNo, PayOrder order,
        long refundAmount) {
        return new PayValidationContext(PayScene.REFUND_CREATE, null, userId, orderNo, null, order, null,
            null, refundAmount);
    }
}
