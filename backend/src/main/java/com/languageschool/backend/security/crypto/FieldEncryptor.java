package com.languageschool.backend.security.crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public class FieldEncryptor {

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    public FieldEncryptor(String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalArgumentException("INVALID_KEY");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64Key);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("INVALID_KEY");
        }
        if (!(bytes.length == 16 || bytes.length == 24 || bytes.length == 32)) {
            throw new IllegalArgumentException("INVALID_KEY_LENGTH");
        }
        this.key = new SecretKeySpec(bytes, "AES");
    }

    public String encrypt(String plain) {
        if (plain == null || plain.isEmpty()) return plain;
        try {
            byte[] iv = new byte[12];
            random.nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
            byte[] ct = c.doFinal(plain.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("ENCRYPT_ERROR", e);
        }
    }

    public String decrypt(String enc) {
        if (enc == null || enc.isEmpty()) return enc;
        try {
            byte[] in = Base64.getDecoder().decode(enc);
            byte[] iv = new byte[12];
            byte[] ct = new byte[in.length - 12];
            System.arraycopy(in, 0, iv, 0, 12);
            System.arraycopy(in, 12, ct, 0, ct.length);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
            byte[] pt = c.doFinal(ct);
            return new String(pt, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("DECRYPT_ERROR", e);
        }
    }
}
