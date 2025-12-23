package com.multi.runrunbackend.domain.match.scheduler;

import com.multi.runrunbackend.domain.match.service.MatchSessionService;
import com.multi.runrunbackend.domain.recruit.entity.Recruit;
import com.multi.runrunbackend.domain.recruit.repository.RecruitRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author : KIMGWANGHO
 * @description : Please explain the class!!!
 * @filename : MatchSessionScheduler
 * @since : 2025-12-22 월요일
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchSessionScheduler {

  private final RecruitRepository recruitRepository;
  private final MatchSessionService matchSessionService;

  @Scheduled(cron = "0 * * * * *")
  public void autoCreateMatchSession() {

    LocalDateTime targetTime = LocalDateTime.now().plusHours(1);
    List<Recruit> pendingRecruits = recruitRepository.findAllByIsDeletedFalseAndMeetingAtBefore(
        targetTime);

    if (pendingRecruits.isEmpty()) {
      return;
    }

    for (Recruit recruit : pendingRecruits) {
      try {
        matchSessionService.createOfflineSession(recruit.getId(), recruit.getUser().getId());
      } catch (Exception e) {
        log.error("자동 생성 실패 id={}", recruit.getId(), e);
      }
    }
  }

}
