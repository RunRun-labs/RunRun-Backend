package com.multi.runrunbackend.domain.tts.dto.res;

import lombok.Builder;
import lombok.Getter;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : TtsVoicePackResDto
 * @since : 2025. 12. 24. Wednesday
 */
@Getter
@Builder
public class TtsVoicePackResDto {

    private Long id;
    private String voiceType;
    private String displayName;
    private String lang;
    private String s3Bucket;
    private String baseS3Prefix;
}

