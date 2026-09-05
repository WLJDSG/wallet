package com.wallet.security.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 生物凭证注册平台（原生系统归一化取值）。
 *
 * <p>取值随 pay_biometric_credential.platform 持久化，属于跨端协议的一部分，
 * 修改取值等同协议升级。私钥分别保存在 Android Keystore 与 iOS Secure Enclave。</p>
 */
@Getter
@AllArgsConstructor
public enum BiometricPlatformEnum {

    /** 安卓，私钥位于 Android Keystore。 */
    ANDROID("安卓"),

    /** 苹果，私钥位于 iOS Secure Enclave。 */
    IOS("苹果"),

    ;

    /** 平台说明。 */
    private final String description;

}
