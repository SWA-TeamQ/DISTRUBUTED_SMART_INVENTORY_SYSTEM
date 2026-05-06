package com.auction.server.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * SHA-256 password hashing utility. No salt (acceptable for university demo).
 */
public final class SecurityUtil {
    private SecurityUtil() {}

    /**
     * Hash a plaintext password using SHA-256.
     * @param password plaintext password
     * @return hex-encoded hash string
     */
    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
