package com.wallet.common.error;

import lombok.Getter;

/**
 * 通用业务异常：携带统一错误码 {@link ErrorCode}，接口层统一转成 ApiResult。
 */
@Getter
public class CommonException extends RuntimeException {

    private final String code;

    public CommonException(ErrorCode error) {
        super(error.message());
        this.code = error.code();
    }

    /** detail 会拼在默认文案后面，方便排查（如订单号、余额差值） */
    public CommonException(ErrorCode error, String detail) {
        super(error.message() + ": " + detail);
        this.code = error.code();
    }
}
