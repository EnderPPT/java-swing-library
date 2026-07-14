package edu.training.library.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public final class PasswordHasher {
    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH = 256;
    private PasswordHasher() {}

    public static Credentials hash(String password) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return new Credentials(derive(password, salt), Base64.getEncoder().encodeToString(salt));
    }

    public static boolean verify(String password, String expectedHash, String encodedSalt) {
        byte[] salt = Base64.getDecoder().decode(encodedSalt);
        return MessageDigest.isEqual(Base64.getDecoder().decode(expectedHash),
                Base64.getDecoder().decode(derive(password, salt)));
    }

    private static String derive(String password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            byte[] key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            spec.clearPassword();
            return Base64.getEncoder().encodeToString(key);
        } catch (Exception e) {
            throw new IllegalStateException("密码加密失败", e);
        }
    }

    public record Credentials(String hash, String salt) {}
}
