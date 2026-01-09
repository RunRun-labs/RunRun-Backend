package com.multi.runrunbackend.domain.running.ghost.service;

import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author : chang
 * @description : 고스트런 페이스 비교 계산 서비스
 * @filename : GhostCompareService
 * @since : 2026-01-01
 */
@Service
@Slf4j
public class GhostCompareService {
  // 나 vs고스트 실시간 비교하는 계산 엔진

  /**
   * 고스트와 실시간 비교
   *
   * @param myDistance    내가 뛴 거리 (km)
   * @param myElapsedTime 내가 달린 시간 (초)
   * @param ghostSplits   고스트의 km별 페이스 데이터 (null 가능)
   * @param totalDistance 고스트의 총 거리 (km)
   * @param totalTime     고스트의 총 시간 (초)
   * @return 비교 결과 Map
   */
  public Map<String, Object> compare(
      double myDistance,
      long myElapsedTime,
      List<Map<String, Object>> ghostSplits,
      double totalDistance,
      int totalTime
  ) {
    long ghostTime;
    String compareMethod;

    // splitPace 데이터가 있으면 정밀 비교, 없으면 평균 페이스로 비교
    //1단계: 고스트가 내 거리를 뛰는데 걸린 시간 계산
    if (ghostSplits != null && !ghostSplits.isEmpty()) {
      ghostTime = interpolateTime(myDistance, ghostSplits);
      compareMethod = "KM_BASED";
    } else {
      ghostTime = calculateByAveragePace(myDistance, totalDistance, totalTime);
      compareMethod = "AVG_PACE";
    }

    // 시간 차이
    //2단계: 시간 차이 계산
    long timeDiff = myElapsedTime - ghostTime;

    // 현재 페이스로 거리 환산
    //3단계: 시간 차이를 거리로 환산
    double pace = ghostSplits != null && !ghostSplits.isEmpty()
        ? getCurrentSegmentPace(myDistance, ghostSplits)
        : totalDistance / totalTime;

    double distanceDiff = Math.abs(timeDiff * pace);

    // 결과 구성
    //4단계: 승패 판정
    String status = timeDiff > 0 ? "BEHIND" : (timeDiff < 0 ? "AHEAD" : "EVEN");
    int distanceDiffMeters = (int) (distanceDiff * 1000);

    // 5단계: 결과 반환
    return Map.of(
        "status", status,
        "distanceDiffMeters", distanceDiffMeters,
        "timeDiffSeconds", Math.abs(timeDiff),
        "myDistance", myDistance,
        "myTime", myElapsedTime,
        "ghostTime", ghostTime,
        "compareMethod", compareMethod
    );
  }

  /**
   * 평균 페이스로 고스트 시간 계산 (splitPace 없을 때)
   *
   * @param myDistance    내 현재 거리 (km)
   * @param totalDistance 고스트의 총 거리 (km)
   * @param totalTime     고스트의 총 시간 (초)
   * @return 고스트가 내 거리를 도달한 예상 시간 (초)
   */
  private long calculateByAveragePace(double myDistance, double totalDistance, int totalTime) {
    double avgPacePerKm = (double) totalTime / totalDistance;
    return (long) (myDistance * avgPacePerKm);
  }

  /**
   * 특정 거리에서 고스트가 도달한 시간 계산 (선형 보간)
   *
   * @param distance 현재 거리 (km)
   * @param splits   km별 페이스 [{km: 1, pace: 0.17, time: 10}, ...]
   * @return 고스트가 해당 거리를 도달한 시간 (초)
   */
  private long interpolateTime(double distance, List<Map<String, Object>> splits) {
    if (distance <= 0) {
      return 0;
    }

    int kmIndex = (int) distance;
    double fraction = distance - kmIndex;

    // 1km 미만
    if (kmIndex == 0) {
      Map<String, Object> firstSplit = splits.get(0);
      Number time = (Number) firstSplit.get("time");
      return (long) (time.doubleValue() * distance);
    }

    // 마지막 구간 넘어감
    if (kmIndex >= splits.size()) {
      Map<String, Object> lastSplit = splits.get(splits.size() - 1);
      Number lastTime = (Number) lastSplit.get("time");
      return lastTime.longValue();
    }

    // 보간 계산
    Map<String, Object> current = splits.get(kmIndex - 1);
    Map<String, Object> next = splits.get(kmIndex);

    Number currentTime = (Number) current.get("time");
    Number nextTime = (Number) next.get("time");

    long segmentTime = nextTime.longValue() - currentTime.longValue();
    return currentTime.longValue() + (long) (segmentTime * fraction);
  }

  /**
   * 현재 구간의 페이스 계산 (km/s)
   *
   * @param distance 현재 거리
   * @param splits   km별 페이스
   * @return 현재 구간 페이스 (km/s)
   */
  // 시간차이를 거리로 환산하기 위한 현재 구간 페이스 계산
  private double getCurrentSegmentPace(double distance, List<Map<String, Object>> splits) {
    int kmIndex = (int) distance;

    if (kmIndex >= splits.size()) {
      kmIndex = splits.size() - 1;
    }

    if (kmIndex < 0) {
      kmIndex = 0;
    }

    Map<String, Object> split = splits.get(kmIndex);
    Number pace = (Number) split.get("pace");

    return pace.doubleValue();  // km/s
  }
}
