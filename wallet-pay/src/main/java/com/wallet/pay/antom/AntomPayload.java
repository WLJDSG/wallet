package com.wallet.pay.antom;

/**
 * Antom 下单返回（前端拉起支付参数）。
 *
 * @param normalUrl 通用收银台 URL（H5/PC）
 * @param schemeUrl APP scheme（拉起支付宝 App）
 * @param appLinkUrl AppLink URL
 */
public record AntomPayload(String normalUrl, String schemeUrl, String appLinkUrl) {
}
