package com.wallet.security.core;

import com.wallet.security.token.PayBiometricChallengeToken;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 支付安全的纯规则与规范化协议，保持无外部依赖以便对跨端协议做确定性测试。
 */
public final class PaySecurityChecks {

    private PaySecurityChecks() {
    }

    /**
     * 校验新支付密码的长度、字符集与弱口令规则。
     *
     * @param password 待校验的明文，仅可在当前请求内短暂使用
     * @return 恰为 6 位数字且不属于弱口令时返回 true
     */
    public static boolean isValidPassword(String password) {
        return password != null && password.matches("^\\d{6}$") && !isWeakPassword(password);
    }

    /**
     * 判断 6 位密码是否为全重复、严格连续递增或严格连续递减序列。
     *
     * @param password 待判断密码
     * @return 长度不是 6 或命中任一弱口令模式时返回 true
     */
    public static boolean isWeakPassword(String password) {
        if (password == null || password.length() != 6) {
            return true;
        }
        boolean same = true;
        boolean ascending = true;
        boolean descending = true;
        for (int i = 1; i < password.length(); i++) {
            int previous = password.charAt(i - 1) - '0';
            int current = password.charAt(i) - '0';
            same &= previous == current;
            ascending &= current - previous == 1;
            descending &= current - previous == -1;
        }
        return same || ascending || descending;
    }

    /**
     * 按协议 v1 的固定字段顺序构建跨端签名载荷。
     *
     * <p>任何字段增删或顺序变化都属于签名协议升级，需同时升级版本并更新 App 实现。
     * 金额授权挑战（无订单号）的订单号、类型两段规范化为空串，字段位置保持不变。</p>
     *
     * @param challengeId 一次性挑战ID
     * @param challenge 服务端保存的挑战快照
     * @return 使用竖线分隔的规范化 UTF-8 签名文本
     */
    public static String challengePayload(String challengeId, PayBiometricChallengeToken challenge) {
        String orderNo = challenge.getOrderNo() == null ? "" : challenge.getOrderNo();
        String orderType = challenge.getOrderType() == null ? "" : challenge.getOrderType();
        return "v1|" + challengeId + "|" + challenge.getNonce() + "|" + challenge.getUid() + "|"
            + challenge.getCredentialId() + "|" + orderNo + "|" + orderType + "|"
            + challenge.getAmount().toPlainString() + "|" + challenge.getCurrency() + "|" + challenge.getExpiresAt();
    }

    /**
     * 使用常量时间字节比较校验短信验证码，降低时序信息泄露。
     *
     * @param left 服务端保存值
     * @param right 客户端提交值
     * @return 两个非空 UTF-8 字节序列完全相等时返回 true
     */
    public static boolean constantTimeEquals(String left, String right) {
        return left != null && right != null && MessageDigest.isEqual(left.getBytes(StandardCharsets.UTF_8),
            right.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 根据服务端固定发布边界判断客户端是否必须使用支付授权。
     *
     * @param enforceVersion 首个强制版本，-1 表示尚未开始强制
     * @param clientVersion 客户端十进制 build 号
     * @return 已开始强制且客户端版本到达边界时返回 true
     * @throws IllegalArgumentException 客户端版本为空或非数字时抛出
     */
    public static boolean shouldEnforce(int enforceVersion, String clientVersion) {
        try {
            int client = Integer.parseInt(clientVersion);
            return enforceVersion >= 0 && client >= enforceVersion;
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("invalid security version", exception);
        }
    }
}
