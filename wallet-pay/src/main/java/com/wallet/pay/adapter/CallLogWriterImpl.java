package com.wallet.pay.adapter;

import com.wallet.channel.model.CallLog;
import com.wallet.channel.spi.CallLogWriter;
import com.wallet.common.trace.TraceIds;
import com.wallet.pay.entity.ChannelLog;
import com.wallet.pay.mapper.ChannelLogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 渠道调用日志适配：落 channel_log 表。
 * 序列化用宿主 Jackson 2（内核不依赖 JSON 库）。
 */
@Component
public class CallLogWriterImpl implements CallLogWriter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ChannelLogMapper channelLogMapper;

    public CallLogWriterImpl(ChannelLogMapper channelLogMapper) {
        this.channelLogMapper = channelLogMapper;
    }

    @Override
    public void write(CallLog log) {
        ChannelLog row = new ChannelLog();
        row.setChannelCode(log.channelCode());
        row.setAction(log.action().name());
        row.setOrderNo(log.orderNo());
        row.setOutTradeNo(log.outTradeNo());
        row.setRequestJson(toJson(log.request()));
        row.setResponseJson(toJson(log.response()));
        row.setErrorMsg(log.error());
        row.setCostMs((int) log.costMs());
        row.setTraceId(TraceIds.current());
        row.setCreateTime(LocalDateTime.now());
        channelLogMapper.insert(row);
    }

    private String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            return String.valueOf(obj);
        }
    }
}
