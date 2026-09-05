package com.wallet.account.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wallet.account.entity.PaySecurityAuditEntity;
import org.apache.ibatis.annotations.Mapper;

/** 支付安全审计数据访问。 */
@Mapper
public interface PaySecurityAuditMapper extends BaseMapper<PaySecurityAuditEntity> {
}
