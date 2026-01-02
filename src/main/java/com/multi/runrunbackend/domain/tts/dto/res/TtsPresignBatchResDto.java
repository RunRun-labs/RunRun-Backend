package com.multi.runrunbackend.domain.tts.dto.res;

import com.multi.runrunbackend.domain.tts.constant.TtsCue;
import java.time.OffsetDateTime;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : TtsPresignBatchResDto
 * @since : 2025. 12. 24. Wednesday
 */
@Getter
@Builder
public class TtsPresignBatchResDto {

    private Long voicePackId;
    private long expiresInSeconds;
    private OffsetDateTime expiresAt;
    private Map<TtsCue, String> urls;
}
