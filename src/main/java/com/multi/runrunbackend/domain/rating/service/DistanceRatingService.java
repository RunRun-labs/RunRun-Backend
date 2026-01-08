package com.multi.runrunbackend.domain.rating.service;

import com.multi.runrunbackend.common.constant.DistanceType;
import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.match.entity.BattleResult;
import com.multi.runrunbackend.domain.match.entity.MatchSession;
import com.multi.runrunbackend.domain.match.entity.RunningResult;
import com.multi.runrunbackend.domain.match.repository.BattleResultRepository;
import com.multi.runrunbackend.domain.match.repository.MatchSessionRepository;
import com.multi.runrunbackend.domain.rating.entity.DistanceRating;
import com.multi.runrunbackend.domain.rating.repository.DistanceRatingRepository;
import com.multi.runrunbackend.domain.user.entity.User;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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
        .filter(r -> r.getRunStatus() != com.multi.runrunbackend.domain.match.constant.RunStatus.GIVE_UP)
        .collect(java.util.stream.Collectors.toList());

    int n = rankedResults.size();
    if (n == 0) {
      log.info("✅ 레이팅 계산 대상 없음: sessionId={}", sessionId);
      return;
    }

    if (n < 2) {
      log.info("✅ 레이팅 계산 2명 미만: sessionId={}, 대상={}명", sessionId, n);
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
      int delta = calculateEloDelta(
          ratings.get(i),
          preRatings,
          i,
          rank,
          n
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
      int totalParticipants
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

  private DistanceRating createNewRating(User user, DistanceType type) {
    return distanceRatingRepository.save(
        DistanceRating.builder()
            .user(user)
            .distanceType(type)
            .build()
    );
  }
}