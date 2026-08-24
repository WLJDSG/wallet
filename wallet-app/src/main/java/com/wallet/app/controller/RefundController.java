package com.wallet.app.controller;

import com.wallet.app.model.RefundReq;
import com.wallet.common.result.ApiResult;
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
@RestController
@RequestMapping("/api/refund")
public class RefundController {

    private final RefundService refundService;

    public RefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    @PostMapping("/create")
    public ApiResult<RefundCreateResult> create(@RequestHeader("X-Uid") Long userId,
        @Valid @RequestBody RefundReq req) {
        return ApiResult.ok(refundService.create(userId, req.orderNo(), req.amount(), req.reason()));
    }

    @GetMapping("/{refundNo}")
    public ApiResult<RefundDetail> detail(@RequestHeader("X-Uid") Long userId,
        @PathVariable String refundNo) {
        return ApiResult.ok(refundService.detail(userId, refundNo));
    }
}
