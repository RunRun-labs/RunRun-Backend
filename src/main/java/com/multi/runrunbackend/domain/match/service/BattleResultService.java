package com.multi.runrunbackend.domain.match.service;

import com.multi.runrunbackend.common.constant.DistanceType;
import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.match.constant.SessionStatus;
import com.multi.runrunbackend.domain.match.dto.res.BattleResultDetailResDto;
import com.multi.runrunbackend.domain.match.dto.res.BattleResultResDto;
import com.multi.runrunbackend.domain.match.dto.res.OnlineBattleRankingResDto;
import com.multi.runrunbackend.domain.match.entity.BattleResult;
import com.multi.runrunbackend.domain.match.entity.MatchSession;
import com.multi.runrunbackend.domain.match.repository.BattleResultRepository;
import com.multi.runrunbackend.domain.match.repository.MatchSessionRepository;
import com.multi.runrunbackend.domain.match.repository.SessionUserRepository;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author : KIMGWANGHO
 * @description : Please explain the class!!!
 * @filename : BattleResultService
 * @since : 2026-01-03 토요일
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BattleResultService {

  private final BattleResultRepository battleResultRepository;
  private final MatchSessionRepository matchSessionRepository;
  private final SessionUserRepository sessionUserRepository;
  private final UserRepository userRepository;

  public Slice<BattleResultResDto> getMyBattleResults(CustomUser principal,
      DistanceType distanceType, LocalDateTime from, LocalDateTime to, Pageable pageable) {

    User user = getUser(principal);
    return battleResultRepository.findMyBattleResults(user.getId(), distanceType, from, to,
        pageable);

  }

  @Transactional(readOnly = true)
  public BattleResultDetailResDto getSessionBattleResults(Long sessionId, CustomUser principal) {

    User user = getUser(principal);

    MatchSession session = matchSessionRepository.findById(sessionId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.SESSION_NOT_FOUND));

    boolean participated = sessionUserRepository
        .findBySessionIdAndUserId(sessionId, user.getId())
        .isPresent();

    if (!participated) {
      throw new NotFoundException(ErrorCode.SESSION_NOT_FOUND);
    }

    List<BattleResult> battleResults = battleResultRepository.findSessionBattleResultsEntity(
        sessionId);

    if (battleResults.isEmpty()) {
      if (session.getStatus() == SessionStatus.COMPLETED) {
        log.warn(" BattleResult가 아직 저장되지 않음: sessionId={}, userId={}, status={}",
            sessionId, user.getId(), session.getStatus());
        throw new NotFoundException(ErrorCode.BATTLE_RESULT_NOT_FOUND);
      } else {
        throw new NotFoundException(ErrorCode.BATTLE_RESULT_NOT_FOUND);
      }
    }

    return BattleResultDetailResDto.from(session, battleResults);
  }

  @Transactional(readOnly = true)
  public OnlineBattleRankingResDto getOnlineBattleRankingByRunningResultId(
      CustomUser principal,
      Long runningResultId
  ) {

    getUser(principal);

    if (runningResultId == null) {
      throw new NotFoundException(ErrorCode.RUNNING_RESULT_NOT_FOUND);
    }

    Map<Long, Integer> rankingById = battleResultRepository
        .findRankingByRunningResultIds(Set.of(runningResultId))
        .stream()
        .collect(java.util.stream.Collectors.toMap(
            row -> (Long) row[0],
            row -> (Integer) row[1],
            (a, b) -> a
        ));

    Integer ranking = rankingById.get(runningResultId);
    if (ranking == null) {
      throw new NotFoundException(ErrorCode.BATTLE_RESULT_NOT_FOUND);
    }

    return new OnlineBattleRankingResDto(runningResultId, ranking);
  }

  private User getUser(CustomUser principal) {
    return userRepository.findByLoginId(principal.getLoginId())
        .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
  }


}
