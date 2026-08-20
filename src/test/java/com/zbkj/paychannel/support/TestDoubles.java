package com.zbkj.paychannel.support;

import com.zbkj.paychannel.spi.PayEventListener;
import com.zbkj.paychannel.spi.PayLockManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 测试替身集合。
 */
public final class TestDoubles {

    private TestDoubles() {
    }

    /** 直通锁：单测无并发，直接执行 */
    public static class DirectLockManager implements PayLockManager {
        @Override
        public <T> T withLock(String lockKey, Supplier<T> action) {
            return action.get();
        }
    }

    /** 记录型事件监听 */
    public static class RecordingEventListener implements PayEventListener {

        public final List<String> paySuccess = new ArrayList<>();
        public final List<String> refundSuccess = new ArrayList<>();

        @Override
        public void onPaySuccess(String channelCode, String orderNo, String outTradeNo) {
            paySuccess.add(outTradeNo);
        }

        @Override
        public void onRefundSuccess(String channelCode, String orderNo, String refundOrderNo,
            BigDecimal refundAmount) {
            refundSuccess.add(refundOrderNo);
        }
    }
}
