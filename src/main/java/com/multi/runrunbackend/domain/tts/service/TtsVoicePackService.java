package com.multi.runrunbackend.domain.tts.service;

import com.multi.runrunbackend.common.exception.custom.BadRequestException;
import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.tts.constant.TtsCue;
import com.multi.runrunbackend.domain.tts.constant.TtsRunMode;
import com.multi.runrunbackend.domain.tts.constant.TtsVoiceType;
import com.multi.runrunbackend.domain.tts.dto.req.TtsUpdateMyVoicePackReqDto;
import com.multi.runrunbackend.domain.tts.dto.res.TtsMyVoicePackResDto;
import com.multi.runrunbackend.domain.tts.dto.res.TtsPresignBatchResDto;
import com.multi.runrunbackend.domain.tts.dto.res.TtsVoicePackResDto;
import com.multi.runrunbackend.domain.tts.entity.TtsVoicePack;
import com.multi.runrunbackend.domain.tts.repository.TtsVoicePackRepository;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    private final UserRepository userRepository;
    @Value("${tts.presign.ttl-seconds}")
    private long ttlSeconds;


    public List<TtsVoicePackResDto> getVoicePacks() {
        return voicePackRepository.findAll().stream()
            .map(v -> TtsVoicePackResDto.builder()
                .id(v.getId())
                .voiceType(v.getVoiceType().name())
                .displayName(v.getDisplayName())
                .lang(v.getLang())
                .s3Bucket(v.getS3Bucket())
                .baseS3Prefix(v.getBaseS3Prefix())
                .build())
            .toList();
    }

    /**
     * 로그인 유저의 ttsVoicePack 설정 조회
     */
    public TtsMyVoicePackResDto getMyVoicePack(CustomUser principal) {
        User user = getUserOrThrow(principal);
        TtsVoicePack pack = resolveUserVoicePackOrDefault(user);
        return toMyVoicePackRes(pack);
    }

    /**
     * 로그인 유저의 ttsVoicePack 설정 변경 - voicePackId 또는 voiceType 중 하나로 선택 가능
     */
    @Transactional
    public TtsMyVoicePackResDto updateMyVoicePack(CustomUser principal,
        TtsUpdateMyVoicePackReqDto req) {
        User user = getUserOrThrow(principal);
        Long voicePackId = req != null ? req.getVoicePackId() : null;
        TtsVoiceType voiceType = req != null ? req.getVoiceType() : null;
        boolean hasId = voicePackId != null;
        boolean hasType = voiceType != null;
        if (hasId == hasType) { // 둘 다 있거나 둘 다 없음
            throw new BadRequestException(ErrorCode.INVALID_REQUEST);
        }

        TtsVoicePack selected;
        if (hasId) {
            selected = voicePackRepository.findById(voicePackId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.TTSVOICE_NOT_FOUND));
        } else {
            selected = voicePackRepository.findFirstByVoiceType(voiceType)
                .orElseThrow(() -> new NotFoundException(ErrorCode.TTSVOICE_NOT_FOUND));
        }

        user.updateTTS(selected);
        return toMyVoicePackRes(selected);
    }

    public TtsPresignBatchResDto presignBatchForMe(CustomUser principal, TtsRunMode mode,
        boolean includePace) {
        User user = getUserOrThrow(principal);
        TtsVoicePack pack = resolveUserVoicePackOrDefault(user);
        List<TtsCue> cues = cuesForMode(mode, includePace);

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

    private User getUserOrThrow(CustomUser principal) {
        if (principal == null || principal.getUserId() == null) {
            throw new NotFoundException(ErrorCode.USER_NOT_FOUND);
        }
        return userRepository.findById(principal.getUserId())
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
    }

    private TtsVoicePack resolveUserVoicePackOrDefault(User user) {
        if (user != null && user.getTtsVoicePack() != null) {
            return user.getTtsVoicePack();
        }
        // 기본값: 남자 보이스팩 1개
        return voicePackRepository.findFirstByVoiceType(TtsVoiceType.MALE)
            .orElseThrow(() -> new NotFoundException(ErrorCode.TTSVOICE_NOT_FOUND));
    }

    private TtsMyVoicePackResDto toMyVoicePackRes(TtsVoicePack pack) {
        if (pack == null) {
            return TtsMyVoicePackResDto.builder()
                .voicePackId(null)
                .voiceType(null)
                .displayName(null)
                .lang(null)
                .build();
        }
        return TtsMyVoicePackResDto.builder()
            .voicePackId(pack.getId())
            .voiceType(pack.getVoiceType())
            .displayName(pack.getDisplayName())
            .lang(pack.getLang())
            .build();
    }

    private List<TtsCue> cuesForMode(TtsRunMode mode, boolean includePace) {
        // 기본(오프라인/솔로/고스트/온라인 공통): 시작/종료/카운트다운/거리/경고/WS
        List<TtsCue> base = new java.util.ArrayList<>(List.of(
            TtsCue.START_RUN,
            TtsCue.COUNTDOWN_3,
            TtsCue.COUNTDOWN_2,
            TtsCue.COUNTDOWN_1,
            TtsCue.MOTIVATE_GOOD_JOB,
            TtsCue.ARRIVED_DESTINATION,
            TtsCue.END_RUN,

            // 누적 거리
            TtsCue.DIST_DONE_1KM,
            TtsCue.DIST_DONE_2KM,
            TtsCue.DIST_DONE_3KM,
            TtsCue.DIST_DONE_4KM,
            TtsCue.DIST_DONE_5KM,
            TtsCue.DIST_DONE_6KM,
            TtsCue.DIST_DONE_7KM,
            TtsCue.DIST_DONE_8KM,
            TtsCue.DIST_DONE_9KM,
            TtsCue.DIST_DONE_10KM,

            // 남은 거리
            TtsCue.DIST_REMAIN_500M,
            TtsCue.DIST_REMAIN_300M,
            TtsCue.DIST_REMAIN_100M,

            // 경고
            TtsCue.OFF_ROUTE,
            TtsCue.BACK_ON_ROUTE,
            TtsCue.SPEED_TOO_FAST,
            TtsCue.GPS_WEAK,

            // WS 상태
            TtsCue.WEB_SOCKET_DISCONNECTED,
            TtsCue.WEB_SOCKET_RECONNECTED,
            TtsCue.WEB_SOCKET_RECONNECTING,
            TtsCue.HOST_SIGNAL_LOST,
            TtsCue.HOST_SIGNAL_RESUMED
        ));

        if (includePace) {
            base.addAll(List.of(
                TtsCue.PACE_1M00,
                TtsCue.PACE_1M30,
                TtsCue.PACE_2M00,
                TtsCue.PACE_2M30,
                TtsCue.PACE_3M00,
                TtsCue.PACE_3M30,
                TtsCue.PACE_4M00,
                TtsCue.PACE_4M30,
                TtsCue.PACE_5M00,
                TtsCue.PACE_5M30,
                TtsCue.PACE_6M00,
                TtsCue.PACE_6M30,
                TtsCue.PACE_7M00,
                TtsCue.PACE_7M30,
                TtsCue.PACE_8M00,
                TtsCue.PACE_8M30,
                TtsCue.PACE_9M30,
                TtsCue.PACE_10M_PLUS
            ));
        }

        if (mode == null) {
            return base;
        }

        switch (mode) {
            case ONLINE -> base.addAll(List.of(
                TtsCue.OPPONENT_AHEAD,
                TtsCue.YOU_AHEAD,
                TtsCue.WIN,
                TtsCue.RANK_2,
                TtsCue.RANK_3,
                TtsCue.RANK_LAST
            ));
            case GHOST -> base.addAll(List.of(
                TtsCue.GHOST_AHEAD,
                TtsCue.GHOST_BEHIND
            ));
            case OFFLINE, SOLO -> {
                // no extra
            }
        }
        return base;
    }
}

