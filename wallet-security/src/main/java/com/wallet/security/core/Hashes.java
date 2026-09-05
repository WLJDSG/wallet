package com.wallet.security.core;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 哈希与随机票据原语。
 */
public final class Hashes {

    private Hashes() {
    }

    /**
     * 计算 UTF-8 字符串的十六进制小写 SHA-256 摘要。
     *
     * @param value 原始字符串
     * @return 64 位十六进制摘要
     */
    public static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                result.append(String.format("%02x", item));
            }
            return result.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash pay authorization token", e);
        }
    }

    /**
     * 计算 HMAC-SHA256 的十六进制小写摘要，用于低熵敏感值（如手机号）的键名脱敏。
     *
     * <p>相比无盐 SHA-256，带密钥的 HMAC 使通用彩虹表与离线枚举失效；
     * 密钥由宿主配置管理。</p>
     *
     * @param secret HMAC 密钥
     * @param value 原始字符串
     * @return 64 位十六进制摘要
     */
    public static String hmacSha256(String secret, String value) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                result.append(String.format("%02x", item));
            }
            return result.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hmac sensitive value", e);
        }
    }

    /**
     * 生成 32 字节安全随机数的 URL-safe Base64（无填充）票据字符串。
     *
     * @param secureRandom 安全随机源
     * @return 43 字符随机票据
     */
    public static String randomToken(SecureRandom secureRandom) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
