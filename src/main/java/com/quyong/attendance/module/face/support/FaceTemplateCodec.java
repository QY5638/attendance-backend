package com.quyong.attendance.module.face.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quyong.attendance.module.face.config.FaceEngineProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Component
public class FaceTemplateCodec {

    private static final String PREFIX = "enc:v1:";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;

    private final ObjectMapper objectMapper;
    private final FaceEngineProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public FaceTemplateCodec(ObjectMapper objectMapper, FaceEngineProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public String encode(FaceTemplateMetadata metadata) {
        if (metadata == null) {
            return null;
        }
        try {
            byte[] plainBytes = objectMapper.writeValueAsBytes(metadata);
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, buildSecretKey(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(plainBytes);
            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (Exception exception) {
            throw new IllegalStateException("人脸模板加密失败", exception);
        }
    }

    public FaceTemplateMetadata decode(String payload) {
        if (!StringUtils.hasText(payload) || !payload.startsWith(PREFIX)) {
            return null;
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(payload.substring(PREFIX.length()));
            if (bytes.length <= IV_LENGTH) {
                return null;
            }
            byte[] iv = Arrays.copyOfRange(bytes, 0, IV_LENGTH);
            byte[] encrypted = Arrays.copyOfRange(bytes, IV_LENGTH, bytes.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, buildSecretKey(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] plainBytes = cipher.doFinal(encrypted);
            return objectMapper.readValue(plainBytes, FaceTemplateMetadata.class);
        } catch (Exception exception) {
            return null;
        }
    }

    private SecretKeySpec buildSecretKey() throws Exception {
        String secret = requireText(properties.getMetadataSecret(), "人脸模板加密密钥未配置");
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] key = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(Arrays.copyOf(key, 16), "AES");
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(message);
        }
        return value.trim();
    }
}
