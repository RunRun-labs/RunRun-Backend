package com.multi.runrunbackend.domain.tts.dto.req;

import com.multi.runrunbackend.domain.tts.constant.TtsVoiceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 로그인 유저의 TTS 보이스팩 설정 변경 요청
 * - voicePackId 또는 voiceType 중 하나만 제공
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TtsUpdateMyVoicePackReqDto {
    private Long voicePackId;
    private TtsVoiceType voiceType;
}


