package com.multi.runrunbackend.domain.running.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.challenge.service.ChallengeProgressService;
import com.multi.runrunbackend.domain.match.constant.RunStatus;
import com.multi.runrunbackend.domain.match.constant.RunningType;
import com.multi.runrunbackend.domain.match.entity.MatchSession;
import com.multi.runrunbackend.domain.match.entity.RunningResult;
import com.multi.runrunbackend.domain.match.entity.SessionUser;
import com.multi.runrunbackend.domain.match.repository.MatchSessionRepository;
import com.multi.runrunbackend.domain.match.repository.RunningResultRepository;
import com.multi.runrunbackend.domain.match.repository.SessionUserRepository;
import com.multi.runrunbackend.domain.running.dto.GPSDataDTO;
import com.multi.runrunbackend.domain.running.dto.RunningStatsDTO;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 런닝 추적 서비스 - 실시간 GPS 데이터 처리 - 통계 계산 - 런닝 결과 저장
 *
 * @author : chang
 * @since : 2024-12-23
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RunningTrackingService {

    private final RedisTemplate<String, String> gpsRedisTemplate;
    private final ObjectMapper objectMapper;
    private final MatchSessionRepository sessionRepository;
    private final SessionUserRepository sessionUserRepository;
    private final RunningResultRepository runningResultRepository;
    private final UserRepository userRepository;
    private final ChallengeProgressService challengeProgressService;


    /**
     * GPS 데이터 처리 및 통계 계산 - 1초마다 호출됨 - Redis List에 GPS 데이터 추가 - 실시간 통계 계산 후 반환
     *
     * @param gpsData GPS 데이터 (방장)
     * @return 런닝 통계 (모든 참여자에게 브로드캐스트)
     */
    public RunningStatsDTO processGPSData(GPSDataDTO gpsData) {

        Long sessionId = gpsData.getSessionId();
        Long userId = gpsData.getUserId();

        log.debug("📡 GPS 처리: sessionId={}, userId={}, distance={}km, time={}초",
                sessionId, userId, gpsData.getTotalDistance(), gpsData.getRunningTime());

        // 1. Redis List에 GPS 데이터 추가 (계속 누적)
        saveUserGPSData(gpsData);

        // 2. km 도달 시간 기록 (1km, 2km, 3km...)
        recordKilometerMarks(gpsData);

        // 3. 세션 정보 조회 (목표 거리)
        MatchSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.SESSION_NOT_FOUND));

        Double targetDistance = session.getTargetDistance();

        // 4. 통계 계산
        return calculateStats(gpsData, targetDistance);
    }

    /**
     * 오프라인 런닝 종료 - Redis 데이터 → PostgreSQL 저장 - 모든 참여자에게 동일한 기록 저장 - 세션 상태를 COMPLETED로 변경
     *
     * @param sessionId 세션 ID
     * @param loginId   방장 loginId
     */
    @Transactional
    public void finishOfflineRunning(Long sessionId, String loginId) {

        log.info("🏁 오프라인 런닝 종료: sessionId={}, loginId={}", sessionId, loginId);

        // 0. loginId로 User 조회
        User hostUser = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

        Long hostUserId = hostUser.getId();

        // 1. 세션 조회 및 상태 업데이트
        MatchSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.SESSION_NOT_FOUND));

        // 세션 상태를 COMPLETED로 변경
        session.updateStatus(com.multi.runrunbackend.domain.match.constant.SessionStatus.COMPLETED);
        sessionRepository.save(session);

        log.info("✅ 세션 상태 업데이트: sessionId={}, status=COMPLETED", sessionId);

        // 2. Redis에서 방장 GPS 데이터 조회
        String trackKey = String.format("running:%d:user:%d:track", sessionId, hostUserId);
        List<String> rawTrack = gpsRedisTemplate.opsForList().range(trackKey, 0, -1);

        if (rawTrack == null || rawTrack.isEmpty()) {
            throw new NotFoundException(ErrorCode.SESSION_NOT_FOUND);
        }

        List<GPSDataDTO> allGPS = new ArrayList<>();
        for (String json : rawTrack) {
            try {
                GPSDataDTO gps = objectMapper.readValue(json, GPSDataDTO.class);
                allGPS.add(gps);
            } catch (Exception e) {
                log.error("❌ GPS JSON 파싱 실패: {}", e.getMessage());
            }
        }

        log.info("📊 총 GPS 포인트: {}개", allGPS.size());

        // 3. 마지막 GPS로 최종 거리, 시간 확인
        GPSDataDTO finalGPS = allGPS.get(allGPS.size() - 1);

        // 4. 평균 페이스 계산
        BigDecimal avgPace = calculateAveragePace(
                finalGPS.getTotalDistance(),
                finalGPS.getRunningTime()
        );

        // 5. split_pace JSON 생성
        List<Map<String, Object>> splitPace = createSplitPace(sessionId, hostUserId);

        // 6. 모든 참여자 조회
        List<SessionUser> participants = sessionUserRepository.findActiveUsersBySessionId(sessionId);

        log.info("👥 참여자 수: {}", participants.size());

        // 7. 시작 시간 계산
        LocalDateTime startedAt = LocalDateTime.now().minusSeconds(finalGPS.getRunningTime());

        // 8. 모든 참여자에게 동일한 RunningResult 저장
        for (SessionUser participant : participants) {
            RunningResult result = RunningResult.builder()
                    .user(participant.getUser())
                    .totalDistance(BigDecimal.valueOf(finalGPS.getTotalDistance()))
                    .totalTime(finalGPS.getRunningTime())
                    .avgPace(avgPace)
                    .splitPace(splitPace)
                    .startedAt(startedAt)
                    .runStatus(RunStatus.COMPLETED)
                    .runningType(RunningType.OFFLINE)
                    .build();

            runningResultRepository.save(result);
            // 추가 : 챌린지 진행도 반영
            challengeProgressService.applyRunningResult(result);

            log.info("✅ 기록 저장: userId={}, distance={}km, time={}초, pace={}분/km",
                    participant.getUser().getId(),
                    finalGPS.getTotalDistance(),
                    finalGPS.getRunningTime(),
                    avgPace);
        }

        // 9. Redis 데이터 삭제
        cleanupRedisData(sessionId, hostUserId);

        log.info("🏁 오프라인 런닝 종료 완료: sessionId={}", sessionId);
    }

    // ===== Redis 헬퍼 메서드 =====

    /**
     * Redis List에 GPS 데이터 추가 (계속 누적)
     */
    private void saveUserGPSData(GPSDataDTO gpsData) {
        try {
            String trackKey = String.format("running:%d:user:%d:track",
                    gpsData.getSessionId(),
                    gpsData.getUserId());

            // GPS 데이터를 JSON 문자열로 변환
            String json = objectMapper.writeValueAsString(gpsData);

            // List 오른쪽에 추가 (append)
            gpsRedisTemplate.opsForList().rightPush(trackKey, json);

            // TTL 설정 (2시간)
            gpsRedisTemplate.expire(trackKey, Duration.ofHours(2));

            Long size = gpsRedisTemplate.opsForList().size(trackKey);
            log.trace("📍 GPS 추가: key={}, total={}", trackKey, size);
        } catch (Exception e) {
            log.error("❌ GPS 저장 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * km 도달 시간 기록 (1km, 2km, 3km...) - 최초 1회만 기록 (중복 방지)
     */
    private void recordKilometerMarks(GPSDataDTO gpsData) {
        Long sessionId = gpsData.getSessionId();
        Long userId = gpsData.getUserId();
        Double distance = gpsData.getTotalDistance();
        Integer time = gpsData.getRunningTime();

        // 1km ~ 10km까지 체크
        for (int km = 1; km <= 10; km++) {
            String kmKey = String.format("running:%d:user:%d:km:%d", sessionId, userId, km);

            // 조건: 아직 기록 안 됐고 && 해당 거리 도달
            if (Boolean.FALSE.equals(gpsRedisTemplate.hasKey(kmKey)) && distance >= km) {
                gpsRedisTemplate.opsForValue().set(kmKey, String.valueOf(time), Duration.ofHours(2));

                log.info("🎯 {}km 도달: sessionId={}, userId={}, time={}초 ({}분)",
                        km, sessionId, userId, time, String.format("%.1f", time / 60.0));
            }
        }
    }

    /**
     * 특정 km 도달 시간 조회
     */
    private Integer getTimeAtDistance(Long sessionId, Long userId, int km) {
        String key = String.format("running:%d:user:%d:km:%d", sessionId, userId, km);
        String value = gpsRedisTemplate.opsForValue().get(key);
        return value != null ? Integer.parseInt(value) : null;
    }

    /**
     * Redis 데이터 삭제
     */
    private void cleanupRedisData(Long sessionId, Long userId) {
        String pattern = String.format("running:%d:user:%d:*", sessionId, userId);
        Set<String> keys = gpsRedisTemplate.keys(pattern);

        if (keys != null && !keys.isEmpty()) {
            gpsRedisTemplate.delete(keys);
            log.info("🗑️ Redis 데이터 삭제: sessionId={}, userId={}, count={}",
                    sessionId, userId, keys.size());
        }
    }

    // ===== 통계 계산 메서드 =====

    /**
     * 실시간 통계 계산
     */
    private RunningStatsDTO calculateStats(GPSDataDTO gpsData, Double targetDistance) {

        Long sessionId = gpsData.getSessionId();
        Long userId = gpsData.getUserId();
        Double totalDistance = gpsData.getTotalDistance();
        Integer runningTime = gpsData.getRunningTime();

        // 팀 평균 페이스 (분/km)
        Double avgPace = null;
        if (totalDistance > 0 && runningTime > 0) {
            avgPace = (runningTime / 60.0) / totalDistance;
        }

        // 남은 거리
        Double remainingDistance = Math.max(0, targetDistance - totalDistance);

        // 구간별 페이스
        Map<Integer, Double> segmentPaces = calculateSegmentPaces(sessionId, userId);

        // 새로 도달한 km 감지 (1km 도달 시 알림용)
        Integer kmReached = detectKmReached(sessionId, userId, totalDistance);

        // 목표 거리 완주 여부 (자동 종료용)
        boolean isCompleted = totalDistance >= targetDistance;

        return RunningStatsDTO.builder()
                .sessionId(sessionId)
                .teamAveragePace(avgPace)
                .totalDistance(totalDistance)
                .remainingDistance(remainingDistance)
                .totalRunningTime(runningTime)
                .segmentPaces(segmentPaces)
                .kmReached(kmReached)
                .isCompleted(isCompleted)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 평균 페이스 계산 (DB 저장용)
     */
    private BigDecimal calculateAveragePace(Double distance, Integer time) {
        if (distance == null || distance <= 0) {
            return BigDecimal.ZERO;
        }

        double pace = (time / 60.0) / distance;
        return BigDecimal.valueOf(Math.round(pace * 100.0) / 100.0);
    }

    /**
     * 구간별 페이스 계산 (0~1km, 0~2km, 0~3km...)
     */
    private Map<Integer, Double> calculateSegmentPaces(Long sessionId, Long userId) {
        Map<Integer, Double> segmentPaces = new LinkedHashMap<>();

        // 1km ~ 10km
        for (int km = 1; km <= 10; km++) {
            Integer timeAtKm = getTimeAtDistance(sessionId, userId, km);

            if (timeAtKm != null && timeAtKm > 0) {
                // 페이스 = 시간(분) / 거리(km)
                Double pace = (timeAtKm / 60.0) / km;
                segmentPaces.put(km, Math.round(pace * 100.0) / 100.0);
            }
        }

        return segmentPaces;
    }

    /**
     * 새로 도달한 km 감지 - 이전에 기록되지 않았던 km에 방금 도달했는지 체크 - Redis에
     * "running:{sessionId}:user:{userId}:km:{km}:notified" 키가 없으면 새로 도달한 것
     */
    private Integer detectKmReached(Long sessionId, Long userId, Double totalDistance) {
        // 1km ~ 10km까지 체크
        for (int km = 1; km <= 10; km++) {
            // 해당 km에 도달했고
            if (totalDistance >= km) {
                // km 도달 시간이 기록되어 있고
                String kmKey = String.format("running:%d:user:%d:km:%d", sessionId, userId, km);
                if (Boolean.TRUE.equals(gpsRedisTemplate.hasKey(kmKey))) {
                    // 아직 알림을 보내지 않았다면
                    String notifiedKey = String.format("running:%d:user:%d:km:%d:notified", sessionId, userId,
                            km);
                    if (Boolean.FALSE.equals(gpsRedisTemplate.hasKey(notifiedKey))) {
                        // 알림 보냄 표시 (TTL 2시간)
                        gpsRedisTemplate.opsForValue().set(notifiedKey, "true", Duration.ofHours(2));
                        return km;  // 새로 도달한 km 반환
                    }
                }
            }
        }
        return null;  // 새로 도달한 km 없음
    }

    /**
     * split_pace JSON 생성 (DB 저장용)
     */
    private List<Map<String, Object>> createSplitPace(Long sessionId, Long userId) {
        List<Map<String, Object>> splitPace = new ArrayList<>();

        // 1km ~ 10km
        for (int km = 1; km <= 10; km++) {
            Integer timeAtKm = getTimeAtDistance(sessionId, userId, km);

            if (timeAtKm != null && timeAtKm > 0) {
                Double pace = (timeAtKm / 60.0) / km;

                Map<String, Object> kmData = new HashMap<>();
                kmData.put("km", km);
                kmData.put("pace", Math.round(pace * 100.0) / 100.0);
                kmData.put("time", timeAtKm);

                splitPace.add(kmData);
            }
        }

        return splitPace;
    }

    /**
     * 런닝 결과 조회
     * - running_result 테이블에서 조회
     *
     * @param sessionId 세션 ID
     * @param loginId   사용자 loginId
     * @return 런닝 결과 데이터
     */
    public Object getRunningResult(Long sessionId, String loginId) {
        // 사용자 조회
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + loginId));

        // running_result 조회 (최신 결과)
        RunningResult result = runningResultRepository
                .findTopByUserOrderByCreatedAtDesc(user)
                .orElseThrow(() -> new IllegalArgumentException("런닝 결과를 찾을 수 없습니다."));

        // split_pace는 이미 List<Map<String, Object>> 타입으로 파싱되어 있음 (Hibernate 자동 변환)
        List<Map<String, Object>> splitPaceList = result.getSplitPace() != null
                ? result.getSplitPace()
                : new ArrayList<>();

        // 응답 데이터 생성
        Map<String, Object> response = new HashMap<>();
        response.put("totalDistance", result.getTotalDistance());
        response.put("totalTime", result.getTotalTime());
        response.put("avgPace", result.getAvgPace().doubleValue());
        response.put("splitPace", splitPaceList);
        response.put("startedAt", result.getStartedAt());
        response.put("runStatus", result.getRunStatus());
        response.put("runningType", result.getRunningType());

        return response;
    }
}
