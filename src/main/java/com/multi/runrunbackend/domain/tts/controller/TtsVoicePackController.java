package com.multi.runrunbackend.domain.tts.controller;

import com.multi.runrunbackend.common.response.ApiResponse;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.tts.constant.TtsCue;
import com.multi.runrunbackend.domain.tts.dto.req.TtsPresignBatchReqDto;
import com.multi.runrunbackend.domain.tts.dto.res.TtsPresignBatchResDto;
import com.multi.runrunbackend.domain.tts.dto.res.TtsVoicePackResDto;
import com.multi.runrunbackend.domain.tts.repository.TtsVoicePackRepository;
import com.multi.runrunbackend.domain.tts.service.TtsVoicePackService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
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

    private final TtsVoicePackRepository voicePackRepository;
    private final TtsVoicePackService presignService;

    @GetMapping("/voice-packs")
    public ApiResponse<List<TtsVoicePackResDto>> voicePacks(
        @AuthenticationPrincipal CustomUser pricipal
    ) {
        List<TtsVoicePackResDto> rows = voicePackRepository.findAll().stream()
            .map(v -> TtsVoicePackResDto.builder()
                .id(v.getId())
                .voiceType(v.getVoiceType().name())
                .displayName(v.getDisplayName())
                .lang(v.getLang())
                .s3Bucket(v.getS3Bucket())
                .baseS3Prefix(v.getBaseS3Prefix())
                .build())
            .toList();

        return ApiResponse.success(rows);
    }

    @GetMapping("/presigned/one")
    public ApiResponse<Map<String, Object>> presignOne(
        @RequestParam Long voicePackId,
        @RequestParam TtsCue cue
    ) {
        String url = presignService.presignOne(voicePackId, cue);
        return ApiResponse.success(Map.of(
            "voicePackId", voicePackId,
            "cue", cue.name(),
            "url", url
        ));
    }

    @PostMapping("/presigned/batch")
    public ApiResponse<TtsPresignBatchResDto> presignBatch(@RequestBody TtsPresignBatchReqDto req) {
        return ApiResponse.success(
            presignService.presignBatch(req.getVoicePackId(), req.getCues()));
    }
}