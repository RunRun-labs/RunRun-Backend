package com.multi.runrunbackend.domain.match.service;

import com.multi.runrunbackend.common.event.RunningResultCompletedEvent;
import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.match.constant.RunStatus;
import com.multi.runrunbackend.domain.match.constant.RunningResultFilterType;
import com.multi.runrunbackend.domain.match.dto.res.RunningRecordResDto;
import com.multi.runrunbackend.domain.match.entity.RunningResult;
import com.multi.runrunbackend.domain.match.repository.RunningResultRepository;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author : KIMGWANGHO
 * @description : Please explain the class!!!
 * @filename : RunningResultService
 * @since : 2026-01-04 일요일
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class RunningResultService {

  private final RunningResultRepository runningResultRepository;
  private final UserRepository userRepository;
  private final ApplicationEventPublisher eventPublisher;

  public Slice<RunningRecordResDto> getMyRunningResults(
      CustomUser principal,
      RunningResultFilterType filterType,
      LocalDate startDate,
      LocalDate endDate,
      Pageable pageable
  ) {
    User user = getUser(principal);

    List<RunStatus> targetStatuses = List.of(
        RunStatus.COMPLETED
    );

    LocalDateTime start = (startDate != null) ? startDate.atStartOfDay() : null;
    LocalDateTime end = (endDate != null) ? endDate.atTime(LocalTime.MAX) : null;

    BigDecimal min = calculateMinDistance(filterType);
    BigDecimal max = calculateMaxDistance(filterType);

    Slice<RunningResult> results = runningResultRepository.findMyRecordsByStatuses(
        user.getId(),
        targetStatuses,
        min,
        max,
        start,
        end,
        pageable
    );

    return results.map(RunningRecordResDto::from);
  }

  private BigDecimal calculateMinDistance(RunningResultFilterType type) {
    if (type == null) {
      return null;
    }
    return switch (type) {
      case UNDER_3 -> BigDecimal.ZERO;
      case BETWEEN_3_5 -> BigDecimal.valueOf(3.0);
      case BETWEEN_5_10 -> BigDecimal.valueOf(5.0);
      case OVER_10 -> BigDecimal.valueOf(10.0);
      case ALL -> null;
    };
  }

  private BigDecimal calculateMaxDistance(RunningResultFilterType type) {
    if (type == null) {
      return null;
    }
    return switch (type) {
      case UNDER_3 -> BigDecimal.valueOf(3.0);
      case BETWEEN_3_5 -> BigDecimal.valueOf(5.0);
      case BETWEEN_5_10 -> BigDecimal.valueOf(10.0);
      case OVER_10, ALL -> null;
    };
  }

  private User getUser(CustomUser principal) {
    return userRepository.findByLoginId(principal.getLoginId())
        .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
  }

  /**
   * RunningResult 저장 및 평균 페이스 업데이트 - 모든 런닝 모드에서 RunningResult 저장 시 이 메서드를 사용 - 저장 후 자동으로 사용자의 평균
   * 페이스를 업데이트
   *
   * @param runningResult 저장할 RunningResult
   * @return 저장된 RunningResult
   */
  @Transactional
  public RunningResult saveAndUpdateAverage(RunningResult runningResult) {
    // 1. RunningResult 저장 (먼저!)
    RunningResult saved = runningResultRepository.save(runningResult);

    log.info("✅ RunningResult 저장: userId={}, distance={}km, pace={}분/km, type={}",
        saved.getUser().getId(),
        saved.getTotalDistance(),
        saved.getAvgPace(),
        saved.getRunningType());

    // 2. 평균 페이스 업데이트 (저장 후!)
    updateUserAveragePace(saved.getUser().getId());

    if (saved.getRunStatus() == RunStatus.COMPLETED
        || saved.getRunStatus() == RunStatus.TIME_OUT) {
      eventPublisher.publishEvent(
          new RunningResultCompletedEvent(saved.getUser().getId(), saved.getTotalDistance())
      );
    }

    return saved;
  }

  /**
   * 사용자의 평균 페이스 업데이트 - 최근 5개 완주 기록의 avgPace 평균 계산 - 기록이 없으면 null로 설정
   *
   * @param userId 사용자 ID
   */
  @Transactional
  public void updateUserAveragePace(Long userId) {
    // 1. 최근 5개 완주 기록 조회
    List<RunningResult> recentResults = runningResultRepository
        .findTop5ByUserIdForAverage(userId, PageRequest.of(0, 5));

    // 2. User 엔티티 조회
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

    // 3. 기록이 없으면 평균 페이스를 null로 설정
    if (recentResults.isEmpty()) {
      user.updateAveragePace(null);
      log.info("ℹ️ 평균 페이스 초기화 (기록 없음): userId={}", userId);
      return;
    }

    // 4. 평균 페이스 계산 (avgPace의 평균)
    BigDecimal averagePace = recentResults.stream()
        .map(RunningResult::getAvgPace)
        .reduce(BigDecimal.ZERO, BigDecimal::add)
        .divide(BigDecimal.valueOf(recentResults.size()), 2, RoundingMode.HALF_UP);

    // 5. User 엔티티 업데이트
    user.updateAveragePace(averagePace);

    log.info("✅ 평균 페이스 업데이트: userId={}, averagePace={}분/km, 기록={}개",
        userId, averagePace, recentResults.size());
  }
}
