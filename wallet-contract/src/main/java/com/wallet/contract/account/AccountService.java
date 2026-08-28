package com.wallet.contract.account;

import com.wallet.contract.account.model.AccountSummary;

/**
 * 账户能力契约。由 {@code wallet-account} 的 {@code AccountService} 实现，Web 层经此查询资产总览。
 */
public interface AccountService {

    /** 资产总览：余额、积分与可用券列表。 */
    AccountSummary summary(Long userId);
}
