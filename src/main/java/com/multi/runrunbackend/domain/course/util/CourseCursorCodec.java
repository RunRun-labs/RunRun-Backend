package com.multi.runrunbackend.domain.course.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.multi.runrunbackend.domain.course.constant.CourseSortType;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : CourseCursorCodec
 * @since : 2025. 12. 19. Friday
 */
@Component
@RequiredArgsConstructor
public class CourseCursorCodec {

    private final ObjectMapper objectMapper;

    @Data
    public static class CursorPayload {

        private CourseSortType sortType;

        private OffsetDateTime createdAt;
        private Long id;

        private Long count;   // LIKE/FAVORITE용
        private Double distM; // DISTANCE용
    }

    public CursorPayload decodeOrNull(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(cursor);
            String json = new String(decoded, StandardCharsets.UTF_8);
            return objectMapper.readValue(json, CursorPayload.class);
        } catch (Exception e) {
            return null; // 커서가 깨졌으면 null 처리(첫 페이지처럼)
        }
    }

    public String encode(CursorPayload payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encode cursor", e);
        }
    }
}

