package com.wallet.account.coupon;

import com.wallet.account.entity.UserCoupon;
import com.wallet.contract.account.enums.CouponType;
import com.wallet.account.error.AccountError;
import com.wallet.account.service.coupon.CouponRuleEngine;
import com.wallet.account.service.coupon.rule.DeductBoundsRule;
import com.wallet.account.service.coupon.rule.DiscountDeductRule;
import com.wallet.account.service.coupon.rule.FullCutDeductRule;
import com.wallet.account.service.coupon.rule.MaxDeductCapRule;
import com.wallet.account.service.coupon.rule.MinAmountRule;
import com.wallet.common.error.BizException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 券规则引擎测试：满减/折扣计算、最低消费门槛、最高抵扣封顶、不超订单额、零抵扣拒绝。
 */
class CouponRuleEngineTest {

    private final CouponRuleEngine engine = new CouponRuleEngine(List.of(
        new MinAmountRule(), new FullCutDeductRule(), new DiscountDeductRule(),
        new MaxDeductCapRule(), new DeductBoundsRule()));

    private UserCoupon coupon(CouponType type, long face, long min, int rate, long maxDeduct) {
        UserCoupon coupon = new UserCoupon();
        coupon.setType(type);
        coupon.setFaceAmount(face);
        coupon.setMinAmount(min);
        coupon.setDiscountRate(rate);
        coupon.setMaxDeductAmount(maxDeduct);
        return coupon;
    }

    @Test
    void fullCutDeductsFaceAmount() {
        // 满100减10：订单 150 元 → 抵 10 元
        assertEquals(1000, engine.calcDeduct(coupon(CouponType.FULL_CUT, 1000, 10000, 0, 0), 15000));
    }

    @Test
    void minAmountRejected() {
        // 满100减10：订单 80 元 → 未达门槛
        BizException e = assertThrows(BizException.class,
            () -> engine.calcDeduct(coupon(CouponType.FULL_CUT, 1000, 10000, 0, 0), 8000));
        assertEquals(AccountError.COUPON_NOT_MATCH.code(), e.getCode());
    }

    @Test
    void discountCalculates() {
        // 9折券：订单 100 元 → 抵 10 元
        assertEquals(1000, engine.calcDeduct(coupon(CouponType.DISCOUNT, 0, 5000, 90, 0), 10000));
    }

    @Test
    void discountCappedByMaxDeduct() {
        // 9折券最高抵 20 元：订单 300 元按率应抵 30 元 → 封顶 20 元
        assertEquals(2000, engine.calcDeduct(coupon(CouponType.DISCOUNT, 0, 5000, 90, 2000), 30000));
    }

    @Test
    void deductNeverExceedsOrderAmount() {
        // 满 1 元减 50 元的极端券：订单 3 元 → 抵扣被压到订单额 3 元
        assertEquals(300, engine.calcDeduct(coupon(CouponType.FULL_CUT, 5000, 100, 0, 0), 300));
    }

    @Test
    void zeroDeductRejected() {
        // 非法折扣率（100）→ 抵扣 0 → 拒绝
        BizException e = assertThrows(BizException.class,
            () -> engine.calcDeduct(coupon(CouponType.DISCOUNT, 0, 0, 100, 0), 10000));
        assertEquals(AccountError.COUPON_NOT_MATCH.code(), e.getCode());
    }

    @Test
    void legacyCouponWithoutTypeTreatedAsFullCut() {
        // 历史数据无 type → 按满减处理
        UserCoupon legacy = coupon(null, 500, 0, 0, 0);
        assertEquals(500, engine.calcDeduct(legacy, 10000));
    }
}
