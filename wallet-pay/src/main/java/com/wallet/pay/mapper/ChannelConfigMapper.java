package com.wallet.pay.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wallet.pay.entity.ChannelConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 渠道配置 Mapper。
 */
@Mapper
public interface ChannelConfigMapper extends BaseMapper<ChannelConfig> {

    default ChannelConfig findByChannelCode(String channelCode) {
        return selectOne(new LambdaQueryWrapper<ChannelConfig>()
            .eq(ChannelConfig::getChannelCode, channelCode)
            .last("LIMIT 1"));
    }
}
