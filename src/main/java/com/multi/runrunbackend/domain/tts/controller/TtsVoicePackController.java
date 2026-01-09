package com.multi.runrunbackend.domain.tts.controller;

import com.multi.runrunbackend.common.response.ApiResponse;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.tts.constant.TtsRunMode;
import com.multi.runrunbackend.domain.tts.dto.req.TtsUpdateMyVoicePackReqDto;
import com.multi.runrunbackend.domain.tts.dto.res.TtsMyVoicePackResDto;
import com.multi.runrunbackend.domain.tts.dto.res.TtsPresignBatchResDto;
import com.multi.runrunbackend.domain.tts.dto.res.TtsVoicePackResDto;
import com.multi.runrunbackend.domain.tts.service.TtsVoicePackService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : TtsVoicePackController
 * @since : 2025. 12. 24. Wednesday
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tts")
public class TtsVoicePackController {

    private final TtsVoicePackService ttsVoicePackService;

    @GetMapping("/voice-packs")
    public ApiResponse<List<TtsVoicePackResDto>> voicePacks(
        @AuthenticationPrincipal CustomUser pricipal
    ) {
        return ApiResponse.success("TTS 보이스팩 목록 조회 성공", ttsVoicePackService.getVoicePacks());
    }


    @PostMapping("/presigned/batch/me")
    public ApiResponse<TtsPresignBatchResDto> presignBatchForMe(
        @AuthenticationPrincipal CustomUser principal,
        @RequestParam TtsRunMode mode
    ) {
        return ApiResponse.success(
            "TTS presigned URL 배치 발급 성공",
            ttsVoicePackService.presignBatchForMe(principal, mode, true)
        );
    }

    @GetMapping("/me/voice-pack")
    public ApiResponse<TtsMyVoicePackResDto> myVoicePack(
        @AuthenticationPrincipal CustomUser principal
    ) {
        return ApiResponse.success("내 TTS 보이스팩 조회 성공",
            ttsVoicePackService.getMyVoicePack(principal));
    }

    @PatchMapping("/me/voice-pack")
    public ApiResponse<TtsMyVoicePackResDto> updateMyVoicePack(
        @AuthenticationPrincipal CustomUser principal,
        @RequestBody TtsUpdateMyVoicePackReqDto req
    ) {

        return ApiResponse.success(
            "내 TTS 보이스팩 설정 변경 성공",
            ttsVoicePackService.updateMyVoicePack(principal, req)
        );
    }
}