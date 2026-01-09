package com.multi.runrunbackend.domain.rating.service;

import com.multi.runrunbackend.common.constant.DistanceType;
import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.match.constant.RunStatus;
import com.multi.runrunbackend.domain.match.entity.BattleResult;
import com.multi.runrunbackend.domain.match.entity.MatchSession;
import com.multi.runrunbackend.domain.match.entity.RunningResult;
import com.multi.runrunbackend.domain.match.repository.BattleResultRepository;
import com.multi.runrunbackend.domain.match.repository.MatchSessionRepository;
import com.multi.runrunbackend.domain.rating.dto.res.DistanceRatingResDto;
import com.multi.runrunbackend.domain.rating.entity.DistanceRating;
import com.multi.runrunbackend.domain.rating.repository.DistanceRatingRepository;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author : KIMGWANGHO
 * @description : Please explain the class!!!
 * @filename : DistanceRatingService
 * @since : 2026-01-02 금요일
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DistanceRatingService {

  private final DistanceRatingRepository distanceRatingRepository;
  private final BattleResultRepository battleResultRepository;
  private final MatchSessionRepository matchSessionRepository;
  private final UserRepository userRepository;

  /**
   * 대결 종료 후 점수 정산 (2~4인 가변 대응) - 기대승률(Elo 확장) 기반
   */
  @Transactional
  public void processBattleResults(Long sessionId, List<RunningResult> results,
      DistanceType distanceType) {

    MatchSession session = matchSessionRepository.findById(sessionId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.SESSION_NOT_FOUND));

    // ✅ GIVE_UP만 제외 (COMPLETED + TIME_OUT 포함)
    List<RunningResult> rankedResults = results.stream()
        .filter(r -> r.getRunStatus()
            != RunStatus.GIVE_UP)
        .collect(java.util.stream.Collectors.toList());

    int n = rankedResults.size();
    if (n == 0) {
      log.info("✅ 레이팅 계산 대상 없음: sessionId={}", sessionId);
      return;
    }

    if (n < 2) {
      if (n == 1) {
        RunningResult rr = rankedResults.get(0);
        User user = rr.getUser();
        DistanceRating rating = distanceRatingRepository
            .findByUserIdAndDistanceType(user.getId(), distanceType)
            .orElseGet(() -> createNewRating(user, distanceType));

        int previousRating = rating.getCurrentRating();

        //  완주 보너스 점수 (K-factor 기반, 일반 1등과 동일)
        int bonusPoints = calculateCompletionBonus(rating, distanceType);
        rating.updateRating(bonusPoints, 1);
        distanceRatingRepository.save(rating);

        BattleResult battleResult = BattleResult.builder()
            .session(session)
            .user(user)
            .runningResult(rr)
            .distanceType(distanceType)
            .ranking(1)  // 혼자만 있으므로 1등
            .previousRating(previousRating)
            .currentRating(rating.getCurrentRating())
            .build();

        battleResultRepository.save(battleResult);

        log.info(
            " 1명 완주 - BattleResult 저장 (완주 보너스 +{}점): sessionId={}, userId={}, rating: {} -> {}",
            bonusPoints, sessionId, user.getId(), previousRating, rating.getCurrentRating());
      }
      return;
    }

    // ✅ 순위는 이미 BattleService에서 부여되어 있으므로 재정렬 불필요
    List<RunningResult> resultsToProcess = rankedResults;

    List<DistanceRating> ratings = new ArrayList<>(n);

    for (RunningResult rr : resultsToProcess) {
      User user = rr.getUser();
      DistanceRating rating = distanceRatingRepository
          .findByUserIdAndDistanceType(user.getId(), distanceType)
          .orElseGet(() -> createNewRating(user, distanceType));

      ratings.add(rating);
    }

    List<Integer> preRatings = new ArrayList<>(n);
    for (DistanceRating r : ratings) {
      preRatings.add(r.getCurrentRating());
    }

    List<Integer> deltas = new ArrayList<>(Collections.nCopies(n, 0));
    for (int i = 0; i < n; i++) {
      int rank = i + 1;
      RunningResult rr = resultsToProcess.get(i);
      boolean isCompleted =
          rr.getRunStatus() == RunStatus.COMPLETED;

      int delta = calculateEloDelta(
          ratings.get(i),
          preRatings,
          i,
          rank,
          n,
          isCompleted
      );
      deltas.set(i, delta);
    }

    List<BattleResult> battleResultList = new ArrayList<>(n);

    for (int i = 0; i < n; i++) {
      RunningResult rr = resultsToProcess.get(i);
      DistanceRating myRating = ratings.get(i);
      int rank = i + 1;

      int previousRating = myRating.getCurrentRating();
      int delta = deltas.get(i);

      myRating.updateRating(delta, rank);
      distanceRatingRepository.save(myRating);

      BattleResult battleResult = BattleResult.builder()
          .session(session)
          .user(rr.getUser())
          .runningResult(rr)
          .distanceType(distanceType)
          .ranking(rank)
          .previousRating(previousRating)
          .currentRating(myRating.getCurrentRating())
          .build();

      battleResultList.add(battleResult);
    }

    battleResultRepository.saveAll(battleResultList);

    log.info("Elo 정산 완료 - SessionID: {}, 레이팅 계산 대상: {}명 (COMPLETED + TIME_OUT)", sessionId, n);
  }

  /**
   * 멀티플레이 Elo 확장: - actual: 등수 기반 0~1 점수 - expected: 상대들과의 pairwise 기대승률 평균 - delta = K * (actual -
   * expected)
   */
  private int calculateEloDelta(
      DistanceRating me,
      List<Integer> preRatings,
      int myIndex,
      int rank,
      int totalParticipants,
      boolean isCompleted
  ) {
    if (totalParticipants <= 1) {
      return 5;
    }

    int myRating = preRatings.get(myIndex);

    double actual = actualScore(rank, totalParticipants);

    double expected = 0.0;
    int oppCount = 0;

    for (int j = 0; j < totalParticipants; j++) {
      if (j == myIndex) {
        continue;
      }
      int oppRating = preRatings.get(j);
      expected += expectedWinProb(myRating, oppRating);
      oppCount++;
    }
    expected = expected / Math.max(1, oppCount);

    int k = kFactor(me.getUser(), me.getDistanceType());

    int delta = (int) Math.round(k * (actual - expected));

    delta = clamp(delta, -50, 50);

    //  완주자 최소 보너스: 완주자는 최소 +5점 보장
    if (isCompleted && delta < 5) {
      delta = 5;
    }

    //  완주자 보호: 완주자는 포기자 패널티(-10점)보다 항상 유리
    if (isCompleted) {
      int giveUpPenalty = 10;  // 포기자 최소 패널티
      if (delta < -giveUpPenalty + 1) {
        delta = -giveUpPenalty + 1;  // 포기자보다 1점이라도 유리 (최소 -9점)
      }
    }

    if (rank == 1 && delta <= 0) {
      delta = 1;
    }
    if (rank == totalParticipants && delta >= 0) {
      delta = -1;
    }

    return delta;
  }

  private double actualScore(int rank, int totalParticipants) {
    return 1.0 - (double) (rank - 1) / (double) (totalParticipants - 1);
  }

  /**
   * Elo 기대승률: 1 / (1 + 10^((opp-my)/400))
   */
  private double expectedWinProb(int myRating, int oppRating) {
    return 1.0 / (1.0 + Math.pow(10.0, (oppRating - myRating) / 400.0));
  }

  /**
   * 판수 기반 K-factor (원하면 거리/티어별로 다르게 가능)
   */
  private int kFactor(User user, DistanceType distanceType) {
    long games = battleResultRepository.countByUserAndDistanceType(user, distanceType);
    if (games < 10) {
      return 40;
    }
    if (games < 30) {
      return 32;
    }
    return 24;
  }

  private int clamp(int value, int min, int max) {
    return Math.max(min, Math.min(max, value));
  }

  /**
   * 완주 보너스 점수 계산 (1명만 완주한 경우)
   */
  private int calculateCompletionBonus(DistanceRating rating, DistanceType distanceType) {
    int k = kFactor(rating.getUser(), distanceType);

    return (int) Math.round(k * 0.5);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  protected DistanceRating createNewRating(User user, DistanceType type) {
    return distanceRatingRepository.save(
        DistanceRating.builder()
            .user(user)
            .distanceType(type)
            .build()
    );
  }


  @Transactional(readOnly = true)
  public DistanceRatingResDto getUserDistanceRating(CustomUser principal,
      DistanceType distanceType) {
    User user = userRepository.findByLoginId(principal.getLoginId())
        .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

    DistanceRating rating = distanceRatingRepository
        .findByUserIdAndDistanceType(user.getId(), distanceType)
        .orElseGet(() -> createNewRating(user, distanceType));

    return DistanceRatingResDto.builder()
        .currentRating(rating.getCurrentRating())
        .currentTier(rating.getCurrentTier())
        .build();
  }
}