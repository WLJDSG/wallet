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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
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
@Tag(name = "资产", description = "余额/积分/优惠券总览与联调造数")
@RestController
@RequestMapping("/api/asset")
@AllArgsConstructor
public class AssetController {

    private final AccountService accountService;
    private final MoneyService moneyService;
    private final PointService pointService;
    private final CouponService couponService;


    @Operation(summary = "资产总览", description = "余额、积分与可用券列表")
    @GetMapping("/summary")
    public ApiResult<AssetSummary> summary(@Parameter(description = "用户ID", example = "1001") @RequestHeader("X-Uid") Long userId) {
        return ApiResult.ok(accountService.summary(userId));
    }

    @Operation(summary = "模拟充值余额")
    @PostMapping("/recharge")
    public ApiResult<Map<String, Long>> recharge(@Parameter(description = "用户ID", example = "1001") @RequestHeader("X-Uid") Long userId,
        @Valid @RequestBody RechargeReq req) {
        long after = moneyService.recharge(userId, req.amount(), IdMaker.next("M"), "模拟充值");
        return ApiResult.ok(Map.of("money", after));
    }

    @Operation(summary = "模拟发积分")
    @PostMapping("/point/add")
    public ApiResult<Map<String, Long>> addPoint(@Parameter(description = "用户ID", example = "1001") @RequestHeader("X-Uid") Long userId,
        @Valid @RequestBody PointAddReq req) {
        long after = pointService.add(userId, req.count(), IdMaker.next("J"), "模拟发放");
        return ApiResult.ok(Map.of("point", after));
    }

    @Operation(summary = "领券", description = "按券模板领取，CAS 防超发")
    @PostMapping("/coupon/take")
    public ApiResult<UserCoupon> take(@Parameter(description = "用户ID", example = "1001") @RequestHeader("X-Uid") Long userId, @Valid @RequestBody CouponTakeReq req) {
        return ApiResult.ok(couponService.take(userId, req.couponId()));
    }
}
