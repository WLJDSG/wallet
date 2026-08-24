package com.wallet.pay.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 支付分段类型。DB 存 name()（VARCHAR），JSON 出入参亦为 name()——
 * 入参大小写不敏感（JacksonConfig），非法值直接 400。
 */
@Schema(description = "支付分段类型")
public enum PayType {

    /** 优惠券抵扣段（不折现，退款时仅按规则返还） */
    COUPON,

    /** 积分段 */
    POINT,

    /** 余额段 */
    MONEY,

    /** 三方渠道段（一个支付单至多一段，异步等回调） */
    CHANNEL;

    /** 是否资产段（券/积分/余额，本地事务同步扣减；三方段走渠道异步） */
    public boolean isAsset() {
        return this != CHANNEL;
    }
}
