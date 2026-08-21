package com.wallet.channel.support;

import com.wallet.channel.spi.PayListener;

import java.util.ArrayList;
import java.util.List;

/**
 * 测试替身集合。
 */
public final class TestHelpers {

    private TestHelpers() {
    }

    /** 记录型事件监听 */
    public static class RecordingListener implements PayListener {

        public final List<String> paySuccess = new ArrayList<>();
        public final List<String> refundSuccess = new ArrayList<>();

        @Override
        public void onPaySuccess(String channelCode, String orderNo, String outTradeNo) {
            paySuccess.add(outTradeNo);
        }

        @Override
        public void onRefundSuccess(String channelCode, String orderNo, String refundOrderNo, long amount) {
            refundSuccess.add(refundOrderNo);
        }
    }
}
