package com.wallet.app.controller;

import lombok.AllArgsConstructor;
import com.wallet.app.model.RefundReq;
import com.wallet.common.result.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.wallet.pay.model.RefundCreateResult;
import com.wallet.pay.model.RefundDetail;
import com.wallet.pay.service.RefundService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 退款接口：发起退款 / 退款单详情。
 */
@Tag(name = "退款", description = "拆分退款：按 CHANNEL→MONEY→POINT 逆序分摊，券按规则返还")
@RestController
@RequestMapping("/api/refund")
@AllArgsConstructor
public class RefundController {

    private final RefundService refundService;


    @Operation(summary = "发起退款", description = "先退三方后退资产；持同一把支付单锁与支付/回调互斥")
    @PostMapping("/create")
    public ApiResult<RefundCreateResult> create(@Parameter(description = "用户ID", example = "1001") @RequestHeader("X-Uid") Long userId,
        @Valid @RequestBody RefundReq req) {
        return ApiResult.ok(refundService.create(userId, req.orderNo(), req.amount(), req.reason()));
    }

    @Operation(summary = "查退款单详情", description = "退款单 + 分摊分段")
    @GetMapping("/{refundNo}")
    public ApiResult<RefundDetail> detail(@Parameter(description = "用户ID", example = "1001") @RequestHeader("X-Uid") Long userId,
        @PathVariable String refundNo) {
        return ApiResult.ok(refundService.detail(userId, refundNo));
    }
}
