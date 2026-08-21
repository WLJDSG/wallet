package com.wallet.common.error;

/** 通用错误码。 */
public enum CommonError implements ErrorCode {

    BAD_PARAM("BAD_PARAM", "参数不正确"),
    LOCK_FAILED("LOCK_FAILED", "操作太频繁，请稍后再试"),
    DATA_NOT_FOUND("DATA_NOT_FOUND", "数据不存在"),
    SYSTEM_ERROR("SYSTEM_ERROR", "系统繁忙，请稍后再试");

    private final String code;
    private final String message;

    CommonError(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
