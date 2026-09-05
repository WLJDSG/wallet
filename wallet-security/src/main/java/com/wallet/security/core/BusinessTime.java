package com.wallet.security.core;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 业务时区的“当日/次日零点”计算，用于每日计数过期与锁定截止。
 */
public final class BusinessTime {

    private BusinessTime() {
    }

    /**
     * @param zone 业务时区
     * @return 业务时区下的当前日期
     */
    public static LocalDate today(ZoneId zone) {
        return LocalDate.now(zone);
    }

    /**
     * @param zone 业务时区
     * @return 业务时区下次日零点的毫秒时间戳
     */
    public static long startOfTomorrowMillis(ZoneId zone) {
        return LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli();
    }

    /**
     * @param zone 业务时区
     * @return 距业务时区次日零点的剩余秒数，至少为 1
     */
    public static long secondsUntilTomorrow(ZoneId zone) {
        return Math.max(1L, (startOfTomorrowMillis(zone) - System.currentTimeMillis()) / 1000);
    }
}
