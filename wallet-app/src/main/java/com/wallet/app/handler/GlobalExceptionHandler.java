package com.wallet.app.handler;

import com.baomidou.lock.exception.LockFailureException;
import com.wallet.contract.channel.error.ChannelException;
import com.wallet.common.error.CommonException;
import com.wallet.common.error.ErrorCode;
import com.wallet.common.result.ApiResult;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常 → 统一返回体。
 *
 * <p>HTTP 状态码约定（影响调用方重试语义，尤其渠道回调）：</p>
 * <ul>
 *   <li>业务失败（CommonException/ChannelException）：200 + 错误 code——结果已确定，重试无意义；</li>
 *   <li>参数/报文问题：400——调用方的错，重试无意义；</li>
 *   <li>锁冲突（LOCK_FAILED）：429——瞬时竞争，<b>渠道回调会按非 2xx 重试</b>，不丢通知；</li>
 *   <li>系统异常：500——渠道回调同样会重试。</li>
 * </ul>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CommonException.class)
    public ApiResult<Void> handleBiz(CommonException e) {
        return ApiResult.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(ChannelException.class)
    public ApiResult<Void> handleChannel(ChannelException e) {
        return ApiResult.fail(e.error().code(), e.getMessage());
    }

    /** @Valid 请求体校验失败 */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResult<Void> handleBodyValidation(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String detail = fieldError == null ? ErrorCode.BAD_PARAM.message()
            : fieldError.getField() + " " + fieldError.getDefaultMessage();
        return ApiResult.fail(ErrorCode.BAD_PARAM.code(), detail);
    }

    /** 请求体解析失败（JSON 语法错误、枚举/时间格式非法等） */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResult<Void> handleUnreadableBody(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return ApiResult.fail(ErrorCode.BAD_PARAM.code(), "请求体格式不正确");
    }

    /** @Validated 方法参数（路径变量/查询参数）校验失败 */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResult<Void> handleParamValidation(ConstraintViolationException e) {
        return ApiResult.fail(ErrorCode.BAD_PARAM.code(), e.getMessage());
    }

    /** @Lock4j 等锁超时（同一支付单并发操作）：429，渠道回调据此重试 */
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    @ExceptionHandler(LockFailureException.class)
    public ApiResult<Void> handleLockFailure(LockFailureException e) {
        return ApiResult.fail(ErrorCode.LOCK_FAILED.code(), ErrorCode.LOCK_FAILED.message());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ApiResult<Void> handleMissingHeader(MissingRequestHeaderException e) {
        return ApiResult.fail(ErrorCode.BAD_PARAM.code(), "缺少请求头 " + e.getHeaderName());
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleOther(Exception e) {
        log.error("系统异常", e);
        return ApiResult.fail(ErrorCode.SYSTEM_ERROR.code(), ErrorCode.SYSTEM_ERROR.message());
    }
}
