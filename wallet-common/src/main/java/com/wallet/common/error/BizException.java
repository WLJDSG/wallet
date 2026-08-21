package com.wallet.common.error;

/** 业务异常：携带错误码，接口层统一转成 ApiResult。 */
public class BizException extends RuntimeException {

    private final String code;

    public BizException(ErrorCode error) {
        super(error.message());
        this.code = error.code();
    }

    /** detail 会拼在默认文案后面，方便排查（如订单号、余额差值） */
    public BizException(ErrorCode error, String detail) {
        super(error.message() + ": " + detail);
        this.code = error.code();
    }

    public String getCode() {
        return code;
    }
}
