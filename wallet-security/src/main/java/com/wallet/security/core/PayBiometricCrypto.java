package com.wallet.security.core;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * EC P-256 / ECDSA-SHA256 跨端签名协议实现。
 *
 * <p>该类只负责密码学格式解析与验签，不负责用户、设备、订单或挑战一次性校验。
 * 这些业务绑定必须由域服务在调用本类前后完成。</p>
 */
public final class PayBiometricCrypto {

    private PayBiometricCrypto() {
    }

    /**
     * 解析并从严校验 X.509 SubjectPublicKeyInfo 格式的 P-256 公钥。
     *
     * @param encodedPublicKey Base64 编码的 X.509 EC 公钥
     * @return 曲线字段长度为 256 位的 EC 公钥
     * @throws IllegalArgumentException Base64、X.509、密钥类型或曲线参数不符合要求时抛出
     */
    public static PublicKey parseP256PublicKey(String encodedPublicKey) {
        try {
            PublicKey key = KeyFactory.getInstance("EC")
                .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(encodedPublicKey)));
            if (!(key instanceof ECPublicKey)
                || ((ECPublicKey) key).getParams().getCurve().getField().getFieldSize() != 256) {
                throw new IllegalArgumentException("public key must be EC P-256");
            }
            return key;
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("invalid P-256 public key", exception);
        }
    }

    /**
     * 校验客户端生成的 ASN.1 DER ECDSA-SHA256 签名。
     *
     * @param publicKey 已通过 P-256 校验的凭证公钥
     * @param payload 服务端生成的规范化签名文本
     * @param encodedSignature Base64 编码的 DER 签名
     * @return 签名合法时返回 true，格式错误或验签异常时统一返回 false
     */
    public static boolean verifyDerSignature(PublicKey publicKey, String payload, String encodedSignature) {
        try {
            Signature verifier = Signature.getInstance("SHA256withECDSA");
            verifier.initVerify(publicKey);
            verifier.update(payload.getBytes(StandardCharsets.UTF_8));
            return verifier.verify(Base64.getDecoder().decode(encodedSignature));
        } catch (Exception exception) {
            return false;
        }
    }
}
