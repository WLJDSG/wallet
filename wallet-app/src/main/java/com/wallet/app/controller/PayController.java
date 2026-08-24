package com.wallet.app.controller;

import com.wallet.app.limit.LimitDim;
import com.wallet.app.limit.RateLimit;
import com.wallet.app.model.CreateOrderReq;
import com.wallet.app.model.SubmitReq;
import com.wallet.common.result.ApiResult;
import com.wallet.pay.model.CreateOrderResult;
import com.wallet.pay.model.OrderDetail;
import com.wallet.pay.model.SubmitResult;
import com.wallet.pay.service.PayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 支付接口：创建拆分支付单 / 提交支付 / 渠道回调 / 详情 / 主动查询 / 取消。
 */
@RestController
@RequestMapping("/api/pay")
public class PayController {

    private final PayService payService;

    public PayController(PayService payService) {
        this.payService = payService;
    }

    @PostMapping("/create")
    public ApiResult<CreateOrderResult> create(
        @RequestHeader(value = "X-App-Id", defaultValue = "DEFAULT") String appId,
        @RequestHeader("X-Uid") Long userId,
        @Valid @RequestBody CreateOrderReq req) {
        return ApiResult.ok(payService.create(appId, userId, req.toCmd()));
    }

    @RateLimit(dim = LimitDim.USER, permits = 5)  // 每用户每秒 5 次提交
    @PostMapping("/submit")
    public ApiResult<SubmitResult> submit(@RequestHeader("X-Uid") Long userId,
        @Valid @RequestBody SubmitReq req) {
        return ApiResult.ok(payService.submit(userId, req.orderNo(), req.ticket()));
    }

    /** 渠道异步回调：原始报文 + 全部请求头（验签用），返回渠道应答报文 */
    @PostMapping("/callback/{channel}/{orderNo}/{partNo}")
    public String callback(@PathVariable String channel, @PathVariable String orderNo,
        @PathVariable String partNo, @RequestBody String body,
        @RequestHeader Map<String, String> headers, HttpServletRequest request) {
        Map<String, String> lowerHeaders = new HashMap<>();
        headers.forEach((key, value) -> lowerHeaders.put(key.toLowerCase(), value));
        return payService.handleCallback(channel, orderNo, partNo, body, lowerHeaders,
            request.getMethod(), request.getRequestURI());
    }

    @GetMapping("/order/{orderNo}")
    public ApiResult<OrderDetail> detail(@RequestHeader("X-Uid") Long userId,
        @PathVariable String orderNo) {
        return ApiResult.ok(payService.detail(userId, orderNo));
    }

    @PostMapping("/query/{orderNo}")
    public ApiResult<Map<String, Boolean>> query(@RequestHeader("X-Uid") Long userId,
        @PathVariable String orderNo) {
        return ApiResult.ok(Map.of("done", payService.query(userId, orderNo)));
    }

    @PostMapping("/cancel/{orderNo}")
    public ApiResult<Map<String, String>> cancel(@RequestHeader("X-Uid") Long userId,
        @PathVariable String orderNo) {
        return ApiResult.ok(Map.of("state", payService.cancel(userId, orderNo)));
    }
}
