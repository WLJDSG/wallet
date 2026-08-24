package com.wallet.app.controller;

import com.wallet.app.model.PasswordSetReq;
import com.wallet.app.model.PasswordVerifyReq;
import com.wallet.app.model.VerifyResult;
import com.wallet.asset.service.password.PasswordService;
import com.wallet.common.result.ApiResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 支付密码接口：设置 / 校验签发票据。
 */
@RestController
@RequestMapping("/api/password")
public class PasswordController {

    private final PasswordService passwordService;

    public PasswordController(PasswordService passwordService) {
        this.passwordService = passwordService;
    }

    @PostMapping("/set")
    public ApiResult<Void> set(@RequestHeader("X-Uid") Long userId, @Valid @RequestBody PasswordSetReq req) {
        passwordService.set(userId, req.password(), req.oldPassword());
        return ApiResult.ok();
    }

    @PostMapping("/verify")
    public ApiResult<VerifyResult> verify(@RequestHeader("X-Uid") Long userId,
        @Valid @RequestBody PasswordVerifyReq req) {
        String ticket = passwordService.verifyAndIssue(userId, req.password(), req.orderNo(), req.amount());
        return ApiResult.ok(new VerifyResult(ticket));
    }
}
