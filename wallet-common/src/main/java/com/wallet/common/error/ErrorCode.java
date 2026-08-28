package com.wallet.common.error;

/**
 * 错误码契约：各模块用自己的枚举实现（如 AccountError、OrderError），
 * code 全局唯一，message 为默认文案。
 */
public interface ErrorCode {

    String code();

    String message();
}
