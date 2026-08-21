package com.wallet.pay.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wallet.pay.entity.ChannelLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 渠道调用日志 Mapper。
 */
@Mapper
public interface ChannelLogMapper extends BaseMapper<ChannelLog> {
}
