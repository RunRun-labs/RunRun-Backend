package com.multi.runrunbackend.domain.tts.entity;

import com.multi.runrunbackend.common.exception.custom.BadRequestException;
import com.multi.runrunbackend.common.exception.custom.BusinessException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.tts.constant.TtsCue;
import com.multi.runrunbackend.domain.tts.constant.TtsVoiceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * @author : kyungsoo
 * @description : 학습 시킨 tts 엔티티 목록 관리를 위해 존재한다(ElvenLabs API에서 받은 id를 넣어줘야한다)
 * @filename : Tts
 * @since : 2025. 12. 17. Wednesday
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@EntityListeners(AuditingEntityListener.class)
public class TtsVoicePack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "voice_type", nullable = false, length = 30)
    private TtsVoiceType voiceType;

    @Column(name = "display_name", nullable = false, length = 50)
    private String displayName;

    @Column(name = "lang", nullable = false, length = 20)
    @Builder.Default
    private String lang = "ko-KR";

    @Column(name = "s3_bucket", nullable = false, length = 100)
    private String s3Bucket;

    @Column(name = "base_s3_prefix", nullable = false, length = 300)
    private String baseS3Prefix;

    public String buildKey(TtsCue cue) {
        if (cue == null) {
            throw new BadRequestException(ErrorCode.TTS_CUE_CODE_INVALID);
        }
        if (baseS3Prefix == null || baseS3Prefix.isBlank()) {
            throw new BusinessException(ErrorCode.TTS_VOICE_PACK_PREFIX_INVALID);
        }

        String prefix = baseS3Prefix;
        while (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }

        return prefix + "/" + cue.name() + ".mp3";
    }
}