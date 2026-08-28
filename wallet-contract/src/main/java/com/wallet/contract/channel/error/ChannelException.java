package com.wallet.contract.channel.error;

import com.wallet.contract.channel.enums.PayError;

/**
 * 渠道内核统一业务异常。
 *
 * <p>宿主按 {@link #error()} 的 code 渲染文案；{@link #detail()} 为面向排查的补充信息
 * （渠道原始错误等），不应直接展示给终端用户。</p>
 */
public class ChannelException extends RuntimeException {

    private final PayError error;

    /** 补充排查信息，可为 null */
    private final String detail;

    public ChannelException(PayError error) {
        this(error, null, null);
    }

    public ChannelException(PayError error, String detail) {
        this(error, detail, null);
    }

    public ChannelException(PayError error, String detail, Throwable cause) {
        super(error.code() + (detail == null ? "" : ": " + detail), cause);
        this.error = error;
        this.detail = detail;
    }

    public PayError error() {
        return error;
    }

    public String detail() {
        return detail;
    }
}
