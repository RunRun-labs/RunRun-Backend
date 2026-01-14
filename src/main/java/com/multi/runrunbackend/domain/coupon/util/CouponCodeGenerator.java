package com.multi.runrunbackend.domain.coupon.util;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : CouponCodeGenerator
 * @since : 2025. 12. 29. Monday
 */
@Component
public class CouponCodeGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

    public String generate(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("length must be positive");
        }
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CODE_CHARS[RANDOM.nextInt(CODE_CHARS.length)]);
        }
        return sb.toString();
    }
}


