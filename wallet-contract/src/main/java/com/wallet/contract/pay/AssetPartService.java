package com.wallet.contract.pay;

import com.wallet.contract.pay.model.PayOrderView;
import com.wallet.contract.pay.model.PayPartView;
import com.wallet.contract.pay.model.RefundEntry;

import java.util.List;

/**
 * 资产分段的扣减与补偿回滚——事务边界契约。
 *
 * <p>由 {@code wallet-pay} 的 {@code AssetPartServiceImpl} 实现（@Transactional 行为在实现侧）。
 * 跨模块只传 DTO 视图（{@link PayPartView}/{@link PayOrderView}/{@link RefundEntry}），不暴露持久化实体。</p>
 */
public interface AssetPartService {

    /** 扣资产段：一个本地事务内按 券→积分→余额 顺序扣减，任一失败整体回滚。 */
    void deductAssetParts(Long userId, List<PayPartView> parts, String orderNo);

    /** 补偿回滚已扣资产段（SUCCESS→ROLLBACK + 逆向流水 + 还券）。 */
    void rollbackAssetParts(String orderNo, Long orderUserId);

    /** 资产退款 + 券返还 + 扣主单可退（一个本地事务；三方分段已由内核处理，此处跳过）。 */
    void refundAssets(Long userId, PayOrderView order, List<RefundEntry> entries, String refundNo);
}
