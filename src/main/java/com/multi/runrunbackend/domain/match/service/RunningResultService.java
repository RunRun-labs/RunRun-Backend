package com.multi.runrunbackend.domain.match.service;

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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

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

  public Slice<RunningRecordResDto> getMyRunningResults(
      CustomUser principal,
      RunningResultFilterType filterType,
      LocalDate startDate,
      LocalDate endDate,
      Pageable pageable
  ) {
    User user = getUser(principal);

    List<RunStatus> targetStatuses = List.of(
        RunStatus.COMPLETED,
        RunStatus.GIVE_UP
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
}
