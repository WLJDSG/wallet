package com.wallet.security.core;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PayBiometricCryptoTest {

    @Test
    public void verifiesBase64DerP256SignatureAndRejectsTampering() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair pair = generator.generateKeyPair();
        String payload = "v1|challenge|nonce|7|credential|install|order|ORDER|100|JPY|123";
        Signature signer = Signature.getInstance("SHA256withECDSA");
        signer.initSign(pair.getPrivate());
        signer.update(payload.getBytes(StandardCharsets.UTF_8));
        String signature = Base64.getEncoder().encodeToString(signer.sign());
        String publicKey = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());

        assertTrue(PayBiometricCrypto.verifyDerSignature(
            PayBiometricCrypto.parseP256PublicKey(publicKey), payload, signature));
        assertFalse(PayBiometricCrypto.verifyDerSignature(
            PayBiometricCrypto.parseP256PublicKey(publicKey), payload + "-tampered", signature));
        assertFalse(PayBiometricCrypto.verifyDerSignature(
            PayBiometricCrypto.parseP256PublicKey(publicKey), payload, "not-base64"));
    }

    @Test
    public void rejectsMalformedPublicKey() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
            () -> PayBiometricCrypto.parseP256PublicKey("not-base64"));
    }
}
