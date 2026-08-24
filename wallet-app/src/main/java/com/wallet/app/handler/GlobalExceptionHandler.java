package com.wallet.app.handler;

import com.baomidou.lock.exception.LockFailureException;
import com.wallet.channel.error.ChannelException;
import com.wallet.common.error.BizException;
import com.wallet.common.error.CommonError;
import com.wallet.common.result.ApiResult;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常 → 统一返回体。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ApiResult<Void> handleBiz(BizException e) {
        return ApiResult.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(ChannelException.class)
    public ApiResult<Void> handleChannel(ChannelException e) {
        return ApiResult.fail(e.error().code(), e.getMessage());
    }

    /** @Valid 请求体校验失败 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResult<Void> handleBodyValidation(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String detail = fieldError == null ? CommonError.BAD_PARAM.message()
            : fieldError.getField() + " " + fieldError.getDefaultMessage();
        return ApiResult.fail(CommonError.BAD_PARAM.code(), detail);
    }

    /** 请求体解析失败（JSON 语法错误、枚举/时间格式非法等） */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResult<Void> handleUnreadableBody(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return ApiResult.fail(CommonError.BAD_PARAM.code(), "请求体格式不正确");
    }

    /** @Validated 方法参数（路径变量/查询参数）校验失败 */
    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResult<Void> handleParamValidation(ConstraintViolationException e) {
        return ApiResult.fail(CommonError.BAD_PARAM.code(), e.getMessage());
    }

    /** @Lock4j 等锁超时（同一支付单并发操作） */
    @ExceptionHandler(LockFailureException.class)
    public ApiResult<Void> handleLockFailure(LockFailureException e) {
        return ApiResult.fail(CommonError.LOCK_FAILED.code(), CommonError.LOCK_FAILED.message());
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
