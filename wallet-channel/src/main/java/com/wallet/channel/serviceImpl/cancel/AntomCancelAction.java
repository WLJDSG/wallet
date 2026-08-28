package com.wallet.channel.serviceImpl.cancel;
import com.wallet.channel.serviceImpl.support.AntomClient;
import com.wallet.channel.serviceImpl.support.AbstractAntomAction;

import com.wallet.contract.channel.action.CancelAction;
import com.wallet.contract.channel.model.CancelRequest;
import org.springframework.stereotype.Component;

/**
 * Antom 关单动作（CANCEL）：Antom 无远程关单接口，支付到点自动过期，本地直接视为关闭成功。
 */
@Component
public class AntomCancelAction extends AbstractAntomAction implements CancelAction {

    public AntomCancelAction(AntomClient client) {
        super(client);
    }

    @Override
    public boolean cancel(CancelRequest request) {
        return true;
    }
}
