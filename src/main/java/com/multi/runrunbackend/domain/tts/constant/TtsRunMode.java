package com.multi.runrunbackend.domain.tts.constant;

import com.multi.runrunbackend.common.exception.custom.BadRequestException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;

/**
 * 러닝 모드별 TTS cue 세트를 분리하기 위한 모드 값
 */
public enum TtsRunMode {
    OFFLINE,
    ONLINE,
    GHOST,
    SOLO;

    public static TtsRunMode fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new BadRequestException(ErrorCode.INVALID_REQUEST);
        }
        try {
            return TtsRunMode.valueOf(code.trim().toUpperCase());
        } catch (Exception e) {
            throw new BadRequestException(ErrorCode.INVALID_REQUEST);
        }
    }
}


