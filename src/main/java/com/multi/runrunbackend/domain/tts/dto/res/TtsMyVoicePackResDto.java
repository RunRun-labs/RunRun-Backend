package com.multi.runrunbackend.domain.tts.dto.res;

import com.multi.runrunbackend.domain.tts.constant.TtsVoiceType;
import lombok.Builder;
import lombok.Getter;

/**
 * 로그인 유저의 현재 TTS 보이스팩 설정 응답
 */
@Getter
@Builder
public class TtsMyVoicePackResDto {
    private Long voicePackId;
    private TtsVoiceType voiceType;
    private String displayName;
    private String lang;
}


