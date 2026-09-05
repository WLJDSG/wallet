package com.wallet.security.core;

/**
 * 字符串空白判断，与 hutool StrUtil.isBlank 语义一致（按 Unicode 空白字符判断）。
 */
public final class Texts {

    private Texts() {
    }

    public static boolean isBlank(CharSequence value) {
        if (value == null || value.length() == 0) {
            return true;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isWhitespace(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isNotBlank(CharSequence value) {
        return !isBlank(value);
    }
}
