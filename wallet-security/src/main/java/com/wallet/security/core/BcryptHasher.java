package com.wallet.security.core;

import org.springframework.security.crypto.bcrypt.BCrypt;

import java.util.regex.Pattern;

/**
 * BCrypt 慢哈希封装，行为与 {@code BCryptPasswordEncoder}（$2a 版本）等价，
 * 但不依赖 commons-logging，保证 支付安全内核 在无 Spring 运行时的宿主中可用。
 *
 * <p>与存量 {@code $2a$12$...} 哈希完全兼容。</p>
 */
public final class BcryptHasher {

    private static final Pattern BCRYPT_PATTERN = Pattern.compile("\\A\\$2(a|y|b)?\\$\\d\\d\\$[./0-9A-Za-z]{53}");

    private final int strength;

    /**
     * @param strength BCrypt log rounds，取值 4-31
     */
    public BcryptHasher(int strength) {
        this.strength = strength;
    }

    /**
     * 生成 $2a 版本 BCrypt 哈希。
     *
     * @param rawPassword 明文
     * @return BCrypt 哈希
     */
    public String encode(CharSequence rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("rawPassword cannot be null");
        }
        return BCrypt.hashpw(rawPassword.toString(), BCrypt.gensalt("$2a", strength));
    }

    /**
     * 校验明文与哈希是否匹配；哈希为空或格式非法时返回 false。
     *
     * @param rawPassword 明文
     * @param encodedPassword 存量 BCrypt 哈希
     * @return 匹配返回 true
     */
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("rawPassword cannot be null");
        }
        if (encodedPassword == null || encodedPassword.length() == 0) {
            return false;
        }
        if (!BCRYPT_PATTERN.matcher(encodedPassword).matches()) {
            return false;
        }
        return BCrypt.checkpw(rawPassword.toString(), encodedPassword);
    }
}
