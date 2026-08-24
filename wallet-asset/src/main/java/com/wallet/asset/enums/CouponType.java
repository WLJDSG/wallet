package com.wallet.asset.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 券类型（DB 存 name()）。新增券类型 = 新增枚举值 + 一个 {@code CouponRule} 实现，其余不动。
 */
@Schema(description = "券类型")
public enum CouponType {

    /** 满减券：满 min_amount 减 face_amount */
    FULL_CUT,

    /** 折扣券：按 discount_rate 打折（85=八五折，抵扣订单额的 15%），受 max_deduct_amount 封顶 */
    DISCOUNT
}
