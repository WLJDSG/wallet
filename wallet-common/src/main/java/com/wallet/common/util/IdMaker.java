package com.wallet.common.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 单号生成：前缀 + 时间（毫秒）+ 4 位随机数，总长 = 前缀长度 + 21。
 * 唯一性最终靠数据库唯一索引兜底，这里只保证极低的碰撞概率。
 *
 * <p>约定前缀：P=支付单 T=分段 R=退款单 RT=退款分段 M=余额流水 J=积分流水</p>
 */
public final class IdMaker {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private IdMaker() {
    }

    public static String next(String prefix) {
        int random = ThreadLocalRandom.current().nextInt(0, 10000);
        return prefix + LocalDateTime.now().format(TIME) + String.format("%04d", random);
    }
}
