package com.multi.runrunbackend.domain.coupon.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.multi.runrunbackend.domain.coupon.constant.CouponIssueSortType;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : CouponIssueCursorCodec
 * @since : 2025. 12. 29. Monday
 */
@Component
@RequiredArgsConstructor
public class CouponIssueCursorCodec {

    private final ObjectMapper objectMapper;

    @Data
    public static class CursorPayload {

        private CouponIssueSortType sortType;

        private Instant startAt;
        private Instant endAt;
        private Integer benefitValue;
        
        private Long issueId;
    }

    public CursorPayload decodeOrNull(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            byte[] raw = Base64.getUrlDecoder().decode(cursor);
            String json = new String(raw, StandardCharsets.UTF_8);
            return objectMapper.readValue(json, CursorPayload.class);
        } catch (Exception e) {
            return null;
        }
    }

    public String encode(CursorPayload payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("CouponIssueCursor encode failed", e);
        }
    }
}
