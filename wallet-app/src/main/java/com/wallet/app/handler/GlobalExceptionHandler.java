package com.wallet.app.handler;

import com.wallet.channel.error.ChannelException;
import com.wallet.common.error.BizException;
import com.wallet.common.result.ApiResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常 → 统一返回体。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public ApiResult<Void> handleBiz(BizException e) {
        return ApiResult.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(ChannelException.class)
    public ApiResult<Void> handleChannel(ChannelException e) {
        return ApiResult.fail(e.error().code(), e.getMessage());
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ApiResult<Void> handleMissingHeader(MissingRequestHeaderException e) {
        return ApiResult.fail("BAD_PARAM", "缺少请求头 " + e.getHeaderName());
    }

    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleOther(Exception e) {
        log.error("系统异常", e);
        return ApiResult.fail("SYSTEM_ERROR", "系统繁忙，请稍后再试");
    }
}
