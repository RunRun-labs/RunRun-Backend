package com.multi.runrunbackend.domain.running.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.multi.runrunbackend.common.exception.custom.BadRequestException;
import com.multi.runrunbackend.common.exception.custom.ForbiddenException;
import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.challenge.service.ChallengeProgressService;
import com.multi.runrunbackend.domain.chat.dto.ChatMessageDto;
import com.multi.runrunbackend.domain.chat.service.ChatService;
import com.multi.runrunbackend.domain.course.entity.Course;
import com.multi.runrunbackend.domain.course.repository.CourseRepository;
import com.multi.runrunbackend.domain.course.util.GeoJsonConverter;
import com.multi.runrunbackend.domain.course.util.route.CoursePathProcessor;
import com.multi.runrunbackend.domain.match.constant.RunStatus;
import com.multi.runrunbackend.domain.match.constant.RunningType;
import com.multi.runrunbackend.domain.match.constant.SessionStatus;
import com.multi.runrunbackend.domain.match.constant.SessionType;
import com.multi.runrunbackend.domain.match.entity.MatchSession;
import com.multi.runrunbackend.domain.match.entity.RunningResult;
import com.multi.runrunbackend.domain.match.entity.SessionUser;
import com.multi.runrunbackend.domain.match.repository.MatchSessionRepository;
import com.multi.runrunbackend.domain.match.repository.RunningResultRepository;
import com.multi.runrunbackend.domain.match.repository.SessionUserRepository;
import com.multi.runrunbackend.domain.match.service.RunningResultService;
import com.multi.runrunbackend.domain.recruit.constant.RecruitStatus;
import com.multi.runrunbackend.domain.recruit.repository.RecruitRepository;
import com.multi.runrunbackend.domain.running.dto.FreeRunCoursePreviewResDto;
import com.multi.runrunbackend.domain.running.dto.GPSDataDTO;
import com.multi.runrunbackend.domain.running.dto.RunningCoursePathResDto;
import com.multi.runrunbackend.domain.running.dto.RunningStatsDTO;
import com.multi.runrunbackend.domain.running.dto.req.FinishRunningReqDto;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
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
  private final RunningResultService runningResultService;
  private final UserRepository userRepository;
  private final ChallengeProgressService challengeProgressService;
  private final CourseRepository courseRepository;
  private final CoursePathProcessor coursePathProcessor;
  private final ChatService chatService;
  private final RecruitRepository recruitRepository;

    private static final Duration LATEST_STATS_TTL = Duration.ofHours(2);

    /**
     * GPS 데이터 처리 및 통계 계산 - 1초마다 호출됨 - Redis List에 GPS 데이터 추가 - 실시간 통계 계산 후 반환
     *
     * @param gpsData GPS 데이터 (방장)
     * @return 런닝 통계 (모든 참여자에게 브로드캐스트)
     */
    @Transactional(readOnly = true)
    public RunningStatsDTO processGPSData(GPSDataDTO gpsData) {

        Long sessionId = gpsData.getSessionId();
        Long userId = gpsData.getUserId();

        log.debug("📡 GPS 처리: sessionId={}, userId={}, distance={}km, time={}초",
            "matchedDistanceM={}",
            sessionId, userId, gpsData.getTotalDistance(), gpsData.getRunningTime(),
            gpsData.getMatchedDistanceM());
        log.info("getMatchedDistanceM : " + gpsData.getMatchedDistanceM());
        // 1. Redis List에 GPS 데이터 추가 (계속 누적)
        saveUserGPSData(gpsData);

        // 2. km 도달 시간 기록 (1km, 2km, 3km...)
        recordKilometerMarks(gpsData);

        // 3. 세션 정보 조회 (목표 거리)
        MatchSession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.SESSION_NOT_FOUND));

        Double targetDistance = session.getTargetDistance();

        // 3.5 hostMatchedDistM 계산/보정 (프론트에서 못 보내는 경우: 서버에서 코스 라인으로 계산)
        Double resolvedMatchedM = resolveHostMatchedDistM(session, gpsData);
        if (resolvedMatchedM != null) {
            gpsData.setMatchedDistanceM(resolvedMatchedM);
        }
        log.info("!!!!!!!resolvedMatchedM : " + resolvedMatchedM);
        // 4. 통계 계산
        RunningStatsDTO stats = calculateStats(gpsData, targetDistance);

        // ✅ 완주 판정 강화:
        // - 거리(targetDistance) 충족 + (코스가 있으면) 코스 진행도(hostMatchedDistM)도 끝까지 도달해야 완주
        boolean distanceDone = false;
        try {
            double td = gpsData.getTotalDistance() != null ? gpsData.getTotalDistance() : 0.0;
            double target = targetDistance != null ? targetDistance : 0.0;
            distanceDone = td + 1e-9 >= target;
        } catch (Exception e) {
            e.printStackTrace();
        }

        boolean courseDone = true;
        try {
            if (session.getCourse() != null && session.getCourse().getPath() != null) {
                double totalM = computeLineStringMeters(session.getCourse().getPath());
                double matchedM = gpsData.getMatchedDistanceM() != null && Double.isFinite(
                    gpsData.getMatchedDistanceM()) ? gpsData.getMatchedDistanceM() : 0.0;
                // 5m 여유 (좌표/근사 오차)
                courseDone = matchedM >= Math.max(0.0, totalM - 5.0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (distanceDone && courseDone) {
            stats.setCompleted(true);
        } else {
            stats.setCompleted(false);
        }

        saveLatestRunningStats(sessionId, stats);
        return stats;
    }

    /**
     * 세션 기준 코스 경로 조회 (재진입 복원용) - fullPath + remainingPath(방장 기준 진행도 반영) 반환
     */
    @Transactional(readOnly = true)
    public RunningCoursePathResDto getSessionCoursePath(CustomUser principal, Long sessionId) {
        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

        // 세션 참여자 검증
        sessionUserRepository.findBySessionIdAndUserId(sessionId, user.getId())
            .orElseThrow(() -> new NotFoundException(ErrorCode.SESSION_NOT_FOUND));

        MatchSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.SESSION_NOT_FOUND));

        Course course = session.getCourse();
        if (course == null || course.getPath() == null) {
            return RunningCoursePathResDto.builder()
                .courseId(null)
                .fullPath(null)
                .remainingPath(null)
                .startLat(null)
                .startLng(null)
                .distanceM(null)
                .hostMatchedDistM(null)
                .build();
        }

        RunningStatsDTO latest = getLatestRunningStats(sessionId, principal);
        double matchedM = 0.0;
        // ✅ STANDBY 상태일 때는 진행도를 0으로 고정 (이전 러닝 데이터 무시)
        if (session.getStatus() == SessionStatus.IN_PROGRESS &&
            latest != null && latest.getHostMatchedDistM() != null &&
            Double.isFinite(latest.getHostMatchedDistM())) {
            matchedM = Math.max(0.0, latest.getHostMatchedDistM());
        }

        Map<String, Object> full = GeoJsonConverter.toGeoJson(course.getPath());
        Map<String, Object> remaining = sliceLineStringGeoJson(course.getPath(), matchedM);

        return RunningCoursePathResDto.builder()
                .courseId(course.getId())
                .fullPath(full)
                .remainingPath(remaining)
                .startLat(course.getStartLat())
                .startLng(course.getStartLng())
                .distanceM(course.getDistanceM())
                .hostMatchedDistM(matchedM)
                .build();
    }

    private Double resolveHostMatchedDistM(MatchSession session, GPSDataDTO gpsData) {
        // 이전 값(단조 증가 보장용)

        Double prev = null;
        try {
            RunningStatsDTO prevStats = getRunningStat(gpsData.getSessionId());
            if (prevStats != null) {
                prev = prevStats.getHostMatchedDistM();
            }
        } catch (Exception e) {

        }

        // 프론트에서 매칭 진행도를 보내준 경우 우선 사용 (단조 증가)
        Double provided = gpsData.getMatchedDistanceM();
        if (provided != null && Double.isFinite(provided)) {
            if (prev != null && Double.isFinite(prev)) {
                return Math.max(prev, Math.max(0, provided));
            }
            return Math.max(0, provided);
        }

        // 코스 없으면 이전값 유지
        if (session == null || session.getCourse() == null) {
            return prev;
        }
        LineString path = session.getCourse().getPath();
        if (path == null || path.isEmpty() || path.getNumPoints() < 2) {
            return prev;
        }
        if (gpsData.getLatitude() == null || gpsData.getLongitude() == null) {
            return prev;
        }

        MatchResult r = matchAlongMeters(path, gpsData.getLatitude(), gpsData.getLongitude(), prev);
        if (r == null) {
            return prev;
        }

        // 너무 멀면 진행도 갱신하지 않음 (근처만 가도 지워지는 문제 방지)
        // ✅ 단, GPS 오차가 큰 기기에서는 너무 빡세면 진행도가 아예 안 올라가서 선이 안 지워짐
        // -> accuracy 기반으로 허용거리 조절 (코스 근처 지나가면 사라지게: 50m~150m로 확대)
        double accM = gpsData.getAccuracy() != null && Double.isFinite(gpsData.getAccuracy())
                ? gpsData.getAccuracy()
                : 30.0;
        double thresholdM = clamp(accM * 2.0, 50.0, 150.0); // 1.8 → 2.0, 40~120m → 50~150m로 증가
        if (r.minDistM > thresholdM) {
            return prev;
        }

        double along = Math.max(0, r.alongM);
        if (prev != null && Double.isFinite(prev)) {
            // ✅ 1초 tick에서 비정상적으로 크게 점프하는 경우 제한 (루프/교차 점프 방지)
            double maxForwardJumpM = 300.0; // 250m → 300m로 증가
            along = Math.min(along, prev + maxForwardJumpM);
            return Math.max(prev, along);
        }
        return along;
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private static final class MatchResult {

        final double alongM;
        final double minDistM;

        MatchResult(double alongM, double minDistM) {
            this.alongM = alongM;
            this.minDistM = minDistM;
        }
    }

    private MatchResult matchAlongMeters(LineString path, double lat, double lng,
                                         Double prevAlongM) {
        Coordinate[] coords = path.getCoordinates();
        if (coords == null || coords.length < 2) {
            return null;
        }

        // 누적거리(미터)
        double[] cum = new double[coords.length];
        double acc = 0;
        cum[0] = 0;
        for (int i = 1; i < coords.length; i++) {
            double aLng = coords[i - 1].x;
            double aLat = coords[i - 1].y;
            double bLng = coords[i].x;
            double bLat = coords[i].y;
            acc += haversineMeters(aLat, aLng, bLat, bLng);
            cum[i] = acc;
        }

        double bestDist = Double.POSITIVE_INFINITY;
        double bestAlong = 0;

        // ✅ prev가 있으면 루프/교차에서 뒤쪽으로 점프하지 않도록, prev 근처만 탐색
        // - prev가 없으면 전체 탐색
        double windowBackM = 30.0;
        double windowForwardM = 400.0;
        double windowStart = 0;
        double windowEnd = Double.POSITIVE_INFINITY;
        if (prevAlongM != null && Double.isFinite(prevAlongM)) {
            windowStart = Math.max(0, prevAlongM - windowBackM);
            windowEnd = prevAlongM + windowForwardM;
        }

        // 탐색 구간 인덱스 계산 (cum 기반)
        int startIdx = 0;
        int endIdx = coords.length - 2;
        if (prevAlongM != null && Double.isFinite(prevAlongM)) {
            // start: cum[i+1] >= windowStart 인 첫 i
            int lo = 0, hi = coords.length - 1;
            while (lo < hi) {
                int mid = (lo + hi) >>> 1;
                if (cum[mid] < windowStart) {
                    lo = mid + 1;
                } else {
                    hi = mid;
                }
            }
            startIdx = Math.max(0, lo - 1);

            // end: cum[i] <= windowEnd 인 마지막 i
            lo = 0;
            hi = coords.length - 1;
            while (lo < hi) {
                int mid = (lo + hi + 1) >>> 1;
                if (cum[mid] <= windowEnd) {
                    lo = mid;
                } else {
                    hi = mid - 1;
                }
            }
            endIdx = Math.min(coords.length - 2, lo);
        }

        // P를 원점으로 하는 평면 근사 좌표 (미터)
        double cosLat = Math.cos(Math.toRadians(lat));
        for (int i = startIdx; i <= endIdx; i++) {
            double aLng = coords[i].x;
            double aLat = coords[i].y;
            double bLng = coords[i + 1].x;
            double bLat = coords[i + 1].y;

            // meters (근사)
            double ax = (aLng - lng) * 111320.0 * cosLat;
            double ay = (aLat - lat) * 110540.0;
            double bx = (bLng - lng) * 111320.0 * cosLat;
            double by = (bLat - lat) * 110540.0;

            double abx = bx - ax;
            double aby = by - ay;
            double denom = abx * abx + aby * aby;
            if (denom <= 1e-9) {
                continue;
            }

            // AP = -A (P=0,0)
            double t = ((-ax) * abx + (-ay) * aby) / denom;
            if (t < 0) {
                t = 0;
            } else if (t > 1) {
                t = 1;
            }

            double qx = ax + t * abx;
            double qy = ay + t * aby;
            double dist = Math.sqrt(qx * qx + qy * qy);

            // 후보 along (세그먼트 길이는 cum 기반)
            double segLen = Math.max(1.0, cum[i + 1] - cum[i]);
            double along = cum[i] + t * segLen;

            // window 밖이면 제외
            if (along < windowStart - 1e-6 || along > windowEnd + 1e-6) {
                continue;
            }

            // best 갱신: dist 우선, dist 동률이면 prev가 있으면 prev에 더 가까운 along,
            // prev 없으면 더 작은 along(시작점 쪽) 선택
            double eps = 0.5; // meters
            boolean better = false;
            if (dist + eps < bestDist) {
                better = true;
            } else if (Math.abs(dist - bestDist) <= eps) {
                if (prevAlongM != null && Double.isFinite(prevAlongM)) {
                    better = Math.abs(along - prevAlongM) < Math.abs(bestAlong - prevAlongM);
                } else {
                    better = along < bestAlong;
                }
            }

            if (better) {
                bestDist = dist;
                bestAlong = along;
            }
        }

        if (!Double.isFinite(bestDist)) {
            return null;
        }
        return new MatchResult(bestAlong, bestDist);
    }

    /**
     * hostMatchedDistM 만큼 진행된 구간을 제거한 remainingPath(GeoJSON LineString)를 만든다. - coordinates:
     * [[lng,lat], ...]
     */
    private Map<String, Object> sliceLineStringGeoJson(LineString lineString, double traveledM) {
        if (lineString == null || lineString.isEmpty() || lineString.getNumPoints() < 2) {
            return null;
        }

        Coordinate[] coords = lineString.getCoordinates();
        if (coords == null || coords.length < 2) {
            return null;
        }

        // 누적거리(미터)
        double[] cum = new double[coords.length];
        double acc = 0;
        cum[0] = 0;
        for (int i = 1; i < coords.length; i++) {
            acc += haversineMeters(coords[i - 1].y, coords[i - 1].x, coords[i].y, coords[i].x);
            cum[i] = acc;
        }

        double total = acc;
        double tM = Math.max(0, Math.min(total, traveledM));

        Map<String, Object> geoJson = new HashMap<>();
        geoJson.put("type", "LineString");

        List<List<Double>> out = new ArrayList<>();

        // 시작점(그대로)
        if (tM <= 0) {
            for (Coordinate c : coords) {
                out.add(List.of(c.x, c.y));
            }
            geoJson.put("coordinates", out);
            return geoJson;
        }

        // 완주(빈 라인)
        if (tM >= total) {
            geoJson.put("coordinates", out);
            return geoJson;
        }

        // 이진탐색: cum[i] <= tM < cum[i+1]
        int lo = 0;
        int hi = cum.length - 1;
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (cum[mid] <= tM) {
                lo = mid;
            } else {
                hi = mid - 1;
            }
        }
        int i = Math.min(lo, coords.length - 2);
        Coordinate a = coords[i];
        Coordinate b = coords[i + 1];
        double segLen = Math.max(1.0, cum[i + 1] - cum[i]);
        double t = (tM - cum[i]) / segLen;

        // 선형 보간(근사)
        double lng = a.x + (b.x - a.x) * t;
        double lat = a.y + (b.y - a.y) * t;

        out.add(List.of(lng, lat));
        for (int k = i + 1; k < coords.length; k++) {
            out.add(List.of(coords[k].x, coords[k].y));
        }

        geoJson.put("coordinates", out);
        return geoJson;
    }

    private double haversineMeters(double lat1, double lng1, double lat2, double lng2) {
        double R = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private double computeLineStringMeters(LineString lineString) {
        if (lineString == null || lineString.isEmpty() || lineString.getNumPoints() < 2) {
            return 0.0;
        }
        double acc = 0.0;
        for (int i = 1; i < lineString.getNumPoints(); i++) {
            double aLng = lineString.getCoordinateN(i - 1).x;
            double aLat = lineString.getCoordinateN(i - 1).y;
            double bLng = lineString.getCoordinateN(i).x;
            double bLat = lineString.getCoordinateN(i).y;
            acc += haversineMeters(aLat, aLng, bLat, bLng);
        }
        return acc;
    }

    /**
     * 최신 러닝 통계 조회 (재진입 복원용)
     */
    public RunningStatsDTO getLatestRunningStats(Long sessionId, CustomUser principal) {
        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
        return getRunningStat(sessionId);
    }

    private RunningStatsDTO getRunningStat(Long sessionId) {
        if (sessionId == null) {
            return null;
        }

        String key = latestStatsKey(sessionId);
        String json = gpsRedisTemplate.opsForValue().get(key);

        if (json == null || json.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readValue(json, RunningStatsDTO.class);
        } catch (Exception e) {
            log.warn("latest running stats parse failed: sessionId={}, err={}", sessionId,
                    e.getMessage());
            return null;
        }
    }

    /**
     * 오프라인 런닝 종료 - Redis 데이터 → PostgreSQL 저장 - 모든 참여자에게 동일한 기록 저장 - 세션 상태를 COMPLETED로 변경
     *
     * @param sessionId 세션 ID
     * @param loginId   방장 loginId
     */
    @Transactional
    public void finishOfflineRunning(Long sessionId, String loginId,
                                     FinishRunningReqDto req) {

        log.info("🏁 오프라인 런닝 종료: sessionId={}, loginId={}", sessionId, loginId);

    Long courseId = (req != null) ? req.getCourseId() : null;
    // 0. loginId로 User 조회
    User hostUser = userRepository.findByLoginId(loginId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

    Long hostUserId = hostUser.getId();

        // 1. 세션 조회 (필요시 코스 연결) 및 상태 업데이트
        MatchSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.SESSION_NOT_FOUND));

        // ✅ 자유러닝(코스 없음)인데 courseId도 없으면 에러
        // OFFLINE과 SOLO 모두 코스 없으면 코스 저장 강제
        if (session.getCourse() == null && courseId == null) {
            throw new BadRequestException(ErrorCode.FREE_RUN_COURSE_REQUIRED);
        }

        if (courseId != null) {
            // 자유러닝(코스 없음) 종료: 코스 저장 후 finish에 courseId를 전달하면 세션/결과에 연결한다.
            if (session.getCourse() == null) {
                Course course = courseRepository.findById(courseId)
                        .orElseThrow(() -> new NotFoundException(ErrorCode.COURSE_NOT_FOUND));

                // ✅ 코스 소유자가 방장인지 검증
                if (!course.getUser().getId().equals(hostUserId)) {
                    throw new ForbiddenException(ErrorCode.UNAUTHORIZED_COURSE);
                }

                session.updateCourse(course);
            }
        }

        // 세션 상태를 COMPLETED로 변경
        session.updateStatus(SessionStatus.COMPLETED);

        log.info("✅ 세션 상태 업데이트: sessionId={}, status=COMPLETED", sessionId);

    if (session.getType() == SessionType.OFFLINE && session.getRecruit() != null) {
      session.getRecruit().updateStatus(RecruitStatus.COMPLETED);
      recruitRepository.save(session.getRecruit());
      log.info("모집글 상태 업데이트: recruitId={}, status=COMPLETED", session.getRecruit().getId());
    }

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
        List<SessionUser> participants = null;
        // 5. split_pace JSON 생성
        List<Map<String, Object>> splitPace = createSplitPace(sessionId, hostUserId);
        if (session.getType() == SessionType.OFFLINE) {
            // 6. 모든 참여자 조회
            participants = sessionUserRepository.findActiveUsersBySessionId(
                    sessionId);

            log.info("👥 참여자 수: {}", participants.size());
        }

        // 7. 시작 시간 계산
        LocalDateTime startedAt = LocalDateTime.now().minusSeconds(finalGPS.getRunningTime());

        RunningResult hostResult = null;
        if (session.getType() == SessionType.OFFLINE) {
            // 8. 모든 참여자에게 동일한 RunningResult 저장
            if (participants != null) {
                for (SessionUser participant : participants) {
                    RunningResult result = RunningResult.builder()
                            .user(participant.getUser())
                            .course(session.getCourse())
                            .totalDistance(BigDecimal.valueOf(finalGPS.getTotalDistance())
                                    .setScale(2, java.math.RoundingMode.HALF_UP))
                            .totalTime(finalGPS.getRunningTime())
                            .avgPace(avgPace)
                            .splitPace(splitPace)
                            .startedAt(startedAt)
                            .runStatus(RunStatus.COMPLETED)
                            .runningType(RunningType.OFFLINE)
                            .build();

                    RunningResult saved = runningResultService.saveAndUpdateAverage(result);
                    // 추가 : 챌린지 진행도 반영
                    challengeProgressService.applyRunningResult(saved);

                    if (participant.getUser().getId().equals(hostUserId)) {
                        hostResult = result;
                    }

                    log.info("✅ 기록 저장: userId={}, distance={}km, time={}초, pace={}분/km",
                            participant.getUser().getId(),
                            finalGPS.getTotalDistance(),
                            finalGPS.getRunningTime(),
                            avgPace);
                }
            }
        } else if (session.getType() == SessionType.SOLO) {
            // ✅ 솔로런: 방장(본인)에게만 RunningResult 저장
            RunningResult result = RunningResult.builder()
                    .user(hostUser)
                    .course(session.getCourse())
                    .totalDistance(BigDecimal.valueOf(finalGPS.getTotalDistance())
                            .setScale(2, java.math.RoundingMode.HALF_UP))
                    .totalTime(finalGPS.getRunningTime())
                    .avgPace(avgPace)
                    .splitPace(splitPace)
                    .startedAt(startedAt)
                    .runStatus(RunStatus.COMPLETED)
                    .runningType(RunningType.SOLO)
                    .build();

            hostResult = runningResultService.saveAndUpdateAverage(result);


            challengeProgressService.applyRunningResult(hostResult);

            log.info("✅ 솔로런 기록 저장: userId={}, distance={}km, time={}초, pace={}분/km",
                    hostUserId,
                    finalGPS.getTotalDistance(),
                    finalGPS.getRunningTime(),
                    avgPace);
        }
        if (hostResult != null) {
            session.updateRunningResult(hostResult);
        }

        // 9. Redis 데이터 삭제
        cleanupRedisData(sessionId, hostUserId);

        if (session.getType() == SessionType.OFFLINE) {
            // 10. 러닝 결과 저장 완료 후 시스템 메시지 전송
            ChatMessageDto systemMessage = ChatMessageDto.builder()
                    .sessionId(sessionId)
                    .senderId(null)
                    .senderName("SYSTEM")
                    .content("🏁 런닝이 종료되었습니다! 수고하셨습니다!")
                    .messageType("SYSTEM")
                    .build();
            chatService.sendMessage(systemMessage);
        }
        log.info("🏁 오프라인 런닝 종료 완료: sessionId={}", sessionId);
    }

    /**
     * 자유러닝(코스 없음) 코스 프리뷰 생성 - 방장 GPS 트랙을 LineString으로 구성하고, 저장용/프리뷰용 path(GeoJSON 문자열)로 반환한다.
     */
    @Transactional(readOnly = true)
    public FreeRunCoursePreviewResDto previewFreeRunCourse(CustomUser principal, Long sessionId) {

        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

        // 세션 참여자 검증
        sessionUserRepository.findBySessionIdAndUserId(sessionId, user.getId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.SESSION_NOT_FOUND));

        MatchSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.SESSION_NOT_FOUND));

        // ✅ 방장인지 검증
        // SOLO는 recruit가 없으니, 요청자를 host로 취급(솔로는 본인만 있음)
        Long hostUserId = null;
        if (session.getType() == SessionType.SOLO) {
            hostUserId = user.getId();
        } else {
            hostUserId = session.getRecruit() != null
                    ? session.getRecruit().getUser().getId()
                    : null;
        }

    if (hostUserId == null || !hostUserId.equals(user.getId())) {
      throw new ForbiddenException(ErrorCode.NOT_SESSION_HOST);
    }

    // 코스가 이미 있는 세션이면 프리뷰 생성 불가
    if (session.getCourse() != null) {
      throw new BadRequestException(ErrorCode.INVALID_REQUEST);
    }

    // 방장 GPS 트랙 조회 (프리뷰 생성은 방장만 수행)
    String trackKey = String.format("running:%d:user:%d:track", sessionId, hostUserId);
    List<String> rawTrack = gpsRedisTemplate.opsForList().range(trackKey, 0, -1);
    if (rawTrack == null || rawTrack.isEmpty()) {
      throw new NotFoundException(ErrorCode.SESSION_NOT_FOUND);
    }

    // ✅ 마지막 GPS 데이터에서 실제 뛴 거리 가져오기
    GPSDataDTO finalGPS = null;
    try {
      String lastJson = rawTrack.get(rawTrack.size() - 1);
      finalGPS = objectMapper.readValue(lastJson, GPSDataDTO.class);
    } catch (Exception e) {
      log.warn("마지막 GPS 파싱 실패: {}", e.getMessage());
    }

    List<Coordinate> coords = new ArrayList<>();
    long startTime = -1;
    double startLat = 0;
    double startLng = 0;
    int skipCount = 0;

    for (String json : rawTrack) {
      try {
        GPSDataDTO gps = objectMapper.readValue(json, GPSDataDTO.class);
        if (gps.getLatitude() == null || gps.getLongitude() == null) {
        }

                // ✅ 시작 시간 기록 (첫 GPS)
                if (startTime < 0) {
                    startTime = gps.getTimestamp() != null ? gps.getTimestamp()
                            : System.currentTimeMillis();
                    startLat = gps.getLatitude();
                    startLng = gps.getLongitude();
                }

                // ✅ 초반 튀는 GPS 필터링: 시작 후 10초 이내이거나 50m 이내는 제외
                long currentTime =
                        gps.getTimestamp() != null ? gps.getTimestamp() : System.currentTimeMillis();
                double timeSinceStart = (currentTime - startTime) / 1000.0; // 초

                if (timeSinceStart < 10) {
                    // 시작 후 10초 이내: 거리 체크
                    double distFromStart = haversineMeters(
                            startLat, startLng,
                            gps.getLatitude(), gps.getLongitude()
                    );
                    if (distFromStart > 50) {
                        // 50m 이상 튀면 제외
                        skipCount++;
                        continue;
                    }
                }

                // ✅ 추가 필터: 정확도가 나쁜 GPS도 제외 (30m 이상)
                if (gps.getAccuracy() != null && gps.getAccuracy() > 30) {
                    skipCount++;
                    continue;
                }

                // ✅ 추가 필터: 이전 GPS와의 거리가 너무 멀면 제외 (100m 이상 점프)
                if (!coords.isEmpty()) {
                    Coordinate lastCoord = coords.get(coords.size() - 1);
                    double distFromLast = haversineMeters(
                            lastCoord.y, lastCoord.x, // (lat, lng)
                            gps.getLatitude(), gps.getLongitude()
                    );
                    if (distFromLast > 100) {
                        // 100m 이상 점프는 제외
                        skipCount++;
                        continue;
                    }
                }

                // JTS는 (x=lng, y=lat)
                coords.add(new Coordinate(gps.getLongitude(), gps.getLatitude()));
            } catch (Exception e) {
                // skip
            }
        }

        log.info("📊 초반 튀는 GPS 제외: {}개", skipCount);

        if (coords.size() < 2) {
            throw new BadRequestException(ErrorCode.INVALID_REQUEST);
        }

        // LineString 생성 + 저장용 샘플링/정리(포인트 과다/노이즈 완화)
        LineString raw = new org.locationtech.jts.geom.GeometryFactory(
                new org.locationtech.jts.geom.PrecisionModel(), 4326
        ).createLineString(coords.toArray(new Coordinate[0]));
        LineString cleaned = coursePathProcessor.simplifyForStore(raw);

        // GeoJSON 문자열로 반환 (CourseCreateReqDto.path에 그대로 사용 가능)
        String pathJson;
        try {
            Map<String, Object> geo = GeoJsonConverter.toGeoJson(cleaned);
            pathJson = objectMapper.writeValueAsString(geo);
        } catch (Exception e) {
            throw new BadRequestException(ErrorCode.INVALID_REQUEST);
        }

        Coordinate start = cleaned.getCoordinateN(0);
        double distM = computeLineStringMeters(cleaned); // 코스 경로 거리 (참고용)

        // ✅ 실제 뛴 거리 사용 (마지막 GPS의 totalDistance)
        // - 코스 없이 뛸 때: 목표 거리만큼 뛰면 종료 → 실제 거리 = 목표 거리
        // - 코스 있이 뛸 때: 코스 이탈 시 더 뛸 수 있음 → 실제 거리 >= 코스 거리
        int finalDistanceM;
        if (finalGPS != null && finalGPS.getTotalDistance() != null) {
            // km를 m로 변환 (실제 뛴 거리)
            finalDistanceM = (int) Math.max(0, Math.round(finalGPS.getTotalDistance() * 1000));
            log.info("✅ 실제 뛴 거리 사용: {}m (GPS 트랙 계산 거리: {}m)",
                    finalDistanceM, (int) Math.round(distM));
        } else {
            // fallback: 계산된 거리 사용 (드물게 발생)
            finalDistanceM = (int) Math.max(0, Math.round(distM));
            log.warn("⚠️ 마지막 GPS totalDistance 없음, 계산 거리 사용: {}m", finalDistanceM);
        }

        return FreeRunCoursePreviewResDto.builder()
                .path(pathJson)
                .distanceM(finalDistanceM)  // 실제 뛴 거리 사용
                .startLat(start != null ? start.getY() : null)
                .startLng(start != null ? start.getX() : null)
                .build();
    }

    // ===== Redis 헬퍼 메서드 =====

    /**
     * /** Redis List에 GPS 데이터 추가 (계속 누적)
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
                gpsRedisTemplate.opsForValue()
                        .set(kmKey, String.valueOf(time), Duration.ofHours(2));

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

        // ✅ latestStats 키도 삭제
        String latestStatsKey = latestStatsKey(sessionId);
        gpsRedisTemplate.delete(latestStatsKey);
        log.info("🗑️ Redis latestStats 키 삭제: sessionId={}", sessionId);
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

        // 남은 거리 (targetDistance가 null일 수 있음)
        Double target = Optional.ofNullable(targetDistance).orElse(0.0);
        Double remainingDistance = Math.max(0, target - totalDistance);

        // 구간별 페이스
        Map<Integer, Double> segmentPaces = calculateSegmentPaces(sessionId, userId);

        // 새로 도달한 km 감지 (1km 도달 시 알림용)
        Integer kmReached = detectKmReached(sessionId, userId, totalDistance);

        // 목표 거리 완주 여부 (자동 종료용)
        boolean isCompleted = targetDistance != null && totalDistance >= targetDistance;

        return RunningStatsDTO.builder()
                .sessionId(sessionId)
                .teamAveragePace(avgPace)
                .totalDistance(totalDistance)
                .remainingDistance(remainingDistance)
                .totalRunningTime(runningTime)
                .segmentPaces(segmentPaces)
                .kmReached(kmReached)
                // ✅ 참가자 화면에서도 방장 GPS를 표시할 수 있도록 포함
                .hostLatitude(gpsData.getLatitude())
                .hostLongitude(gpsData.getLongitude())
                .hostHeading(gpsData.getHeading())
                // ✅ 참여자도 방장과 동일한 기준(코스 위 진행도)으로 선을 지울 수 있도록 포함
                .hostMatchedDistM(gpsData.getMatchedDistanceM())
                .isCompleted(isCompleted)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    private String latestStatsKey(Long sessionId) {
        return "runningOfflineStatus:latestStats:" + sessionId;
    }

    private void saveLatestRunningStats(Long sessionId, RunningStatsDTO stats) {
        try {
            String key = latestStatsKey(sessionId);
            String json = objectMapper.writeValueAsString(stats);
            gpsRedisTemplate.opsForValue().set(key, json, LATEST_STATS_TTL);
        } catch (Exception e) {
            log.warn("latest running stats save failed: sessionId={}, err={}", sessionId,
                    e.getMessage());
        }
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
                    String notifiedKey = String.format("running:%d:user:%d:km:%d:notified",
                            sessionId, userId,
                            km);
                    if (Boolean.FALSE.equals(gpsRedisTemplate.hasKey(notifiedKey))) {
                        // 알림 보냄 표시 (TTL 2시간)
                        gpsRedisTemplate.opsForValue()
                                .set(notifiedKey, "true", Duration.ofHours(2));
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
     * 런닝 결과 조회 - running_result 테이블에서 조회
     *
     * @param sessionId 세션 ID
     * @param loginId   사용자 loginId
     * @return 런닝 결과 데이터
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getRunningResult(Long sessionId, String loginId) {
        // ✅ 세션에 연결된 러닝 결과가 "정본"
        // - 참여자도 동일 세션 결과를 보게 해야, 저장 딜레이로 이전 기록이 뜨는 문제를 방지할 수 있다.
        MatchSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.SESSION_NOT_FOUND));

        RunningResult result = session.getRunningResult();

        // ✅ 세션 결과가 아직 연결되기 전(저장 딜레이)에는 "처리중"으로 응답하고, 프론트에서 retry 하게 한다.
        // - 유저 최신 기록으로 fallback 하면, 참여자가 이전 기록을 보게 되는 버그가 생긴다.
        if (result == null) {
            throw new BadRequestException(ErrorCode.RUNNING_RESULT_PROCESSING);
        }

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
