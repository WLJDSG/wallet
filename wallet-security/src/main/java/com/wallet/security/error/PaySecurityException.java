package com.wallet.security.error;

import lombok.Getter;

/**
 * 支付安全领域异常。
 *
 * <p>{@code data} 仅在 {@link PaySecurityErrorCode#PAY_PASSWORD_ERROR} 时携带剩余可尝试次数，
 * 供宿主拼装带参数的错误提示；其余错误码 data 为 null。</p>
 */
public class PaySecurityException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    @Getter
    private final PaySecurityErrorCode errorCode;
    @Getter
    private final transient Object data;

    private PaySecurityException(PaySecurityErrorCode errorCode, Object data) {
        super(errorCode.getCode());
        this.errorCode = errorCode;
        this.data = data;
    }

    public static PaySecurityException of(PaySecurityErrorCode errorCode) {
        return new PaySecurityException(errorCode, null);
    }

    public static PaySecurityException of(PaySecurityErrorCode errorCode, Object data) {
        return new PaySecurityException(errorCode, data);
    }

}
