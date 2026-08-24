package com.wallet.app.controller;

import com.wallet.app.model.CouponTakeReq;
import com.wallet.app.model.PointAddReq;
import com.wallet.app.model.RechargeReq;
import com.wallet.asset.entity.UserCoupon;
import com.wallet.asset.model.AssetSummary;
import com.wallet.asset.service.AccountService;
import com.wallet.asset.service.CouponService;
import com.wallet.asset.service.MoneyService;
import com.wallet.asset.service.PointService;
import com.wallet.common.util.IdMaker;
import com.wallet.common.result.ApiResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 资产接口：总览 / 模拟充值 / 模拟发积分 / 领券。
 * 用户识别用请求头 X-Uid，无鉴权体系（联调用）。
 */
@RestController
@RequestMapping("/api/asset")
public class AssetController {

    private final AccountService accountService;
    private final MoneyService moneyService;
    private final PointService pointService;
    private final CouponService couponService;

    public AssetController(AccountService accountService, MoneyService moneyService, PointService pointService,
        CouponService couponService) {
        this.accountService = accountService;
        this.moneyService = moneyService;
        this.pointService = pointService;
        this.couponService = couponService;
    }

    @GetMapping("/summary")
    public ApiResult<AssetSummary> summary(@RequestHeader("X-Uid") Long userId) {
        return ApiResult.ok(accountService.summary(userId));
    }

    @PostMapping("/recharge")
    public ApiResult<Map<String, Long>> recharge(@RequestHeader("X-Uid") Long userId,
        @Valid @RequestBody RechargeReq req) {
        long after = moneyService.recharge(userId, req.amount(), IdMaker.next("M"), "模拟充值");
        return ApiResult.ok(Map.of("money", after));
    }

    @PostMapping("/point/add")
    public ApiResult<Map<String, Long>> addPoint(@RequestHeader("X-Uid") Long userId,
        @Valid @RequestBody PointAddReq req) {
        long after = pointService.add(userId, req.count(), IdMaker.next("J"), "模拟发放");
        return ApiResult.ok(Map.of("point", after));
    }

    @PostMapping("/coupon/take")
    public ApiResult<UserCoupon> take(@RequestHeader("X-Uid") Long userId, @Valid @RequestBody CouponTakeReq req) {
        return ApiResult.ok(couponService.take(userId, req.couponId()));
    }
}
