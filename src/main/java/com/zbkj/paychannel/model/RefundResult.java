package com.zbkj.paychannel.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 渠道退款结果。
 *
 * <p>渠道 Provider 应尽量以 {@code success=false + failReason} 表达业务性失败
 * （如信用审查未通过），而不是抛异常——编排层会把两者都落为退款单 FAIL，
 * 但结构化结果能保留失败原因供宿主展示。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundResult {

    /** 渠道是否受理/完成退款 */
    private boolean success;

    /** 渠道退款流水号，可为 null */
    private String channelRefundNo;

    /** 失败原因（success=false 时填写） */
    private String failReason;
}
