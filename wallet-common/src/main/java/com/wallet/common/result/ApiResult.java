package com.wallet.common.result;

/**
 * 统一接口返回体。
 *
 * @param code    "0" 表示成功，其他为错误码
 * @param message 提示信息
 * @param data    业务数据，可为 null
 */
public record ApiResult<T>(String code, String message, T data) {

    public static final String OK_CODE = "0";

    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(OK_CODE, "ok", data);
    }

    public static ApiResult<Void> ok() {
        return new ApiResult<>(OK_CODE, "ok", null);
    }

    public static <T> ApiResult<T> fail(String code, String message) {
        return new ApiResult<>(code, message, null);
    }
}
