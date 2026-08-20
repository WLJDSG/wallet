package com.zbkj.paychannel.exception;

import com.zbkj.paychannel.enums.PayErrorCode;

/**
 * SDK 统一业务异常。
 *
 * <p>宿主按 {@link #getErrorCode()} 的 code（即 i18n 资源键）渲染文案；
 * {@link #getDetail()} 为面向排查的补充信息（渠道原始错误等），不应直接展示给终端用户。</p>
 */
public class PayChannelException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final PayErrorCode errorCode;

    /** 补充排查信息，可为 null */
    private final String detail;

    public PayChannelException(PayErrorCode errorCode) {
        this(errorCode, null, null);
    }

    public PayChannelException(PayErrorCode errorCode, String detail) {
        this(errorCode, detail, null);
    }

    public PayChannelException(PayErrorCode errorCode, String detail, Throwable cause) {
        super(errorCode.getCode() + (detail == null ? "" : ": " + detail), cause);
        this.errorCode = errorCode;
        this.detail = detail;
    }

    public PayErrorCode getErrorCode() {
        return errorCode;
    }

    public String getDetail() {
        return detail;
    }
}
