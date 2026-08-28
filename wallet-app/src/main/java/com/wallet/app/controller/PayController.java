package com.wallet.app.controller;

import lombok.AllArgsConstructor;
import com.wallet.app.limit.LimitDim;
import com.wallet.app.limit.RateLimit;
import com.wallet.app.model.CreateOrderReq;
import com.wallet.app.model.SubmitReq;
import com.wallet.common.result.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.wallet.contract.pay.model.CreateOrderResult;
import com.wallet.contract.pay.model.OrderDetail;
import com.wallet.contract.pay.model.SubmitResult;
import com.wallet.contract.pay.PayService;
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
@Tag(name = "支付", description = "拆分支付：创建/提交/回调/查证/取消")
@RestController
@RequestMapping("/api/pay")
@AllArgsConstructor
public class PayController {

    private final PayService payService;


    @Operation(summary = "创建拆分支付单", description = "校验分段合法性与金额勾稽；同 X-App-Id 同 bizOrderNo 幂等返回既有单")
    @PostMapping("/create")
    public ApiResult<CreateOrderResult> create(
        @Parameter(description = "来源商城/接入方，缺省 DEFAULT") @RequestHeader(value = "X-App-Id", defaultValue = "DEFAULT") String appId,
        @Parameter(description = "用户ID", example = "1001") @RequestHeader("X-Uid") Long userId,
        @Valid @RequestBody CreateOrderReq req) {
        return ApiResult.ok(payService.create(appId, userId, req.toCmd()));
    }

    @Operation(summary = "提交支付", description = "含资产段必须携带密码票据；扣资产段（事务）+ 发起三方（事务外）")
    @RateLimit(dim = LimitDim.USER, permits = 5)  // 每用户每秒 5 次提交
    @PostMapping("/submit")
    public ApiResult<SubmitResult> submit(@Parameter(description = "用户ID", example = "1001") @RequestHeader("X-Uid") Long userId,
        @Valid @RequestBody SubmitReq req) {
        return ApiResult.ok(payService.submit(userId, req.orderNo(), req.ticket()));
    }

    /** 渠道异步回调：原始报文 + 全部请求头（验签用），返回渠道应答报文 */
    @Operation(summary = "渠道异步回调", description = "验签在渠道实现内完成；返回渠道要求的应答报文，非 2xx 渠道会重试")
    @PostMapping("/callback/{channel}/{orderNo}/{partNo}")
    public String callback(@PathVariable String channel, @PathVariable String orderNo,
        @PathVariable String partNo, @RequestBody String body,
        @RequestHeader Map<String, String> headers, HttpServletRequest request) {
        Map<String, String> lowerHeaders = new HashMap<>();
        headers.forEach((key, value) -> lowerHeaders.put(key.toLowerCase(), value));
        return payService.handleCallback(channel, orderNo, partNo, body, lowerHeaders,
            request.getMethod(), request.getRequestURI());
    }

    @Operation(summary = "查支付单详情", description = "主单 + 全部分段")
    @GetMapping("/order/{orderNo}")
    public ApiResult<OrderDetail> detail(@Parameter(description = "用户ID", example = "1001") @RequestHeader("X-Uid") Long userId,
        @PathVariable String orderNo) {
        return ApiResult.ok(payService.detail(userId, orderNo));
    }

    @Operation(summary = "主动向渠道查证", description = "查证已支付会顺势结单")
    @PostMapping("/query/{orderNo}")
    public ApiResult<Map<String, Boolean>> query(@Parameter(description = "用户ID", example = "1001") @RequestHeader("X-Uid") Long userId,
        @PathVariable String orderNo) {
        return ApiResult.ok(Map.of("done", payService.query(userId, orderNo)));
    }

    @Operation(summary = "取消支付", description = "未支付→关渠道+回滚资产+关单；已实付→补单完成并提示已支付")
    @PostMapping("/cancel/{orderNo}")
    public ApiResult<Map<String, String>> cancel(@Parameter(description = "用户ID", example = "1001") @RequestHeader("X-Uid") Long userId,
        @PathVariable String orderNo) {
        return ApiResult.ok(Map.of("state", payService.cancel(userId, orderNo)));
    }
}
