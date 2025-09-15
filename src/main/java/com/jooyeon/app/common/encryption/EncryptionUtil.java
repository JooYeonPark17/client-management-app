package com.jooyeon.app.common.encryption;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class EncryptionUtil {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;

    private static EncryptionUtil INSTANCE;

    @Value("${app.encryption.key:AbcDefGhijkLmnoPqrsTuvWxYz123456}")
    private String encryptionKey;

    private SecretKeySpec secretKey;

    @PostConstruct
    public void init() {
        byte[] keyBytes = encryptionKey.getBytes(StandardCharsets.UTF_8);
        if (!(keyBytes.length == 16 || keyBytes.length == 24 || keyBytes.length == 32)) {
            throw new IllegalArgumentException("AES key length must be 16/24/32 bytes, but was " + keyBytes.length);
        }
        this.secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
        INSTANCE = this;
    }

    public static EncryptionUtil getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("EncryptionUtil not initialized");
        }
        return INSTANCE;
    }

    public String encrypt(String plainText) {
        try {
            if (plainText == null || plainText.isEmpty()) {
                return plainText;
            }

            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] encryptedData = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] encryptedWithIv = new byte[iv.length + encryptedData.length];
            System.arraycopy(iv, 0, encryptedWithIv, 0, iv.length);
            System.arraycopy(encryptedData, 0, encryptedWithIv, iv.length, encryptedData.length);

            return Base64.getEncoder().encodeToString(encryptedWithIv);
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting data", e);
        }
    }

    public String decrypt(String encryptedText) {
        try {
            if (encryptedText == null || encryptedText.isEmpty()) {
                return encryptedText;
            }

            byte[] decodedData = Base64.getDecoder().decode(encryptedText);

            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(decodedData, 0, iv, 0, iv.length);

            byte[] encryptedData = new byte[decodedData.length - GCM_IV_LENGTH];
            System.arraycopy(decodedData, GCM_IV_LENGTH, encryptedData, 0, encryptedData.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] decryptedData = cipher.doFinal(encryptedData);
            return new String(decryptedData, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting data", e);
        }
    }
}
