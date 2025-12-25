package com.multi.runrunbackend.domain.tts.service;

import com.multi.runrunbackend.common.exception.custom.BadRequestException;
import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.tts.constant.TtsCue;
import com.multi.runrunbackend.domain.tts.dto.res.TtsPresignBatchResDto;
import com.multi.runrunbackend.domain.tts.entity.TtsVoicePack;
import com.multi.runrunbackend.domain.tts.repository.TtsVoicePackRepository;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : TtsVoicePackService
 * @since : 2025. 12. 24. Wednesday
 */
@Service
@RequiredArgsConstructor
public class TtsVoicePackService {

    private final S3Presigner presigner;
    private final TtsVoicePackRepository voicePackRepository;
    @Value("${tts.presign.ttl-seconds}")
    private long ttlSeconds;


    public String presignOne(Long voicePackId, TtsCue cue) {
        if (voicePackId == null) {
            throw new BadRequestException(ErrorCode.INVALID_REQUEST);
        }
        TtsVoicePack pack = voicePackRepository.findById(voicePackId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.TTSVOICE_NOT_FOUND));

        Duration ttl = ttl();
        return presign(pack, cue, ttl);
    }

    public TtsPresignBatchResDto presignBatch(Long voicePackId, List<TtsCue> cuesOrNull) {
        if (voicePackId == null) {
            throw new BadRequestException(ErrorCode.INVALID_REQUEST);
        }
        TtsVoicePack pack = voicePackRepository.findById(voicePackId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.TTSVOICE_NOT_FOUND));

        List<TtsCue> cues = (cuesOrNull == null || cuesOrNull.isEmpty())
            ? List.of(TtsCue.values())   // 비어있으면 전체 내려줌
            : cuesOrNull;

        Duration ttl = ttl();
        Map<TtsCue, String> urls = new EnumMap<>(TtsCue.class);
        for (TtsCue cue : cues) {
            urls.put(cue, presign(pack, cue, ttl));
        }

        OffsetDateTime now = OffsetDateTime.now();
        return TtsPresignBatchResDto.builder()
            .voicePackId(pack.getId())
            .expiresInSeconds(ttl.getSeconds())
            .expiresAt(now.plusSeconds(ttl.getSeconds()))
            .urls(urls)
            .build();
    }

    private String presign(TtsVoicePack pack, TtsCue cue, Duration ttl) {
        // 네 엔티티 메서드 그대로 사용
        String key = pack.buildKey(cue);

        GetObjectRequest getReq = GetObjectRequest.builder()
            .bucket(pack.getS3Bucket())
            .key(key)
            .build();

        GetObjectPresignRequest presignReq = GetObjectPresignRequest.builder()
            .signatureDuration(ttl)   // ✅ 여기서 만료시간이 "서명에 포함"됨
            .getObjectRequest(getReq)
            .build();

        return presigner.presignGetObject(presignReq).url().toString();
    }

    private Duration ttl() {
        return Duration.ofSeconds(ttlSeconds);
    }
}

