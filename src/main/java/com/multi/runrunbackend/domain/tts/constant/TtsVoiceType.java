package com.multi.runrunbackend.domain.tts.constant;

import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import lombok.Getter;

@Getter
public enum TtsVoiceType {
    MALE("남성"),
    FEMALE("여성"),
    KID("아이");

    private final String displayName;

    TtsVoiceType(String displayName) {
        this.displayName = displayName;
    }

    public static TtsVoiceType fromCode(String code) {
        if (code == null) {
            throw new NotFoundException(ErrorCode.TTSVOICE_NOT_FOUND);
        }
        try {
            return TtsVoiceType.valueOf(code.trim().toUpperCase());
        } catch (Exception e) {
            throw new NotFoundException(ErrorCode.TTSVOICE_NOT_FOUND);
        }
    }
}