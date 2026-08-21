package com.wallet.common.util;

/**
 * 金额工具：全工程金额一律用 long 存分，只有展示时才转元。
 */
public final class MoneyUtil {

    private MoneyUtil() {
    }

    /** 分 → 元字符串，如 12345 → "123.45" */
    public static String toYuan(long cent) {
        long abs = Math.abs(cent);
        String sign = cent < 0 ? "-" : "";
        return sign + (abs / 100) + "." + String.format("%02d", abs % 100);
    }
}
