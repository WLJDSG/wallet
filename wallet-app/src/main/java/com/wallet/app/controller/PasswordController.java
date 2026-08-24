package com.wallet.app.controller;

import lombok.AllArgsConstructor;
import com.wallet.app.model.PasswordSetReq;
import com.wallet.app.model.PasswordVerifyReq;
import com.wallet.app.model.VerifyResult;
import com.wallet.app.limit.LimitDim;
import com.wallet.app.limit.RateLimit;
import com.wallet.asset.service.password.PasswordService;
import com.wallet.common.result.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 支付密码接口：设置 / 校验签发票据。
 */
@Tag(name = "支付密码", description = "设置/校验支付密码并签发一次性授权票据")
@RestController
@RequestMapping("/api/password")
@AllArgsConstructor
public class PasswordController {

    private final PasswordService passwordService;


    @Operation(summary = "设置/重置支付密码", description = "已设置过时必须携带旧密码")
    @PostMapping("/set")
    public ApiResult<Void> set(@Parameter(description = "用户ID", example = "1001") @RequestHeader("X-Uid") Long userId, @Valid @RequestBody PasswordSetReq req) {
        passwordService.set(userId, req.password(), req.oldPassword());
        return ApiResult.ok();
    }

    @Operation(summary = "校验密码换票据", description = "校验通过签发一次性票据（TTL 300 秒），提交支付时消费；连续错 5 次/当日 10 次锁 10 分钟")
    @RateLimit(dim = LimitDim.USER, permits = 3)  // 防密码爆破：每用户每秒 3 次
    @RateLimit(dim = LimitDim.IP, permits = 10)
    @PostMapping("/verify")
    public ApiResult<VerifyResult> verify(@Parameter(description = "用户ID", example = "1001") @RequestHeader("X-Uid") Long userId,
        @Valid @RequestBody PasswordVerifyReq req) {
        String ticket = passwordService.verifyAndIssue(userId, req.password(), req.orderNo(), req.amount());
        return ApiResult.ok(new VerifyResult(ticket));
    }
}
