package com.multi.runrunbackend.domain.crew;

import com.multi.runrunbackend.domain.crew.entity.Crew;
import com.multi.runrunbackend.domain.crew.repository.CrewRepository;
import com.multi.runrunbackend.domain.crew.service.CrewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author : BoKyung
 * @description : 크루 스케줄러
 * @since : 2026. 1. 8.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CrewScheduler {

    private final CrewService crewService;
    private final CrewRepository crewRepository;

    /**
     * @description : 위임 기한 만료 크루 해체
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void processExpiredDelegations() {

        log.info("=== 크루장 위임 기한 만료 처리 스케줄러 시작 ===");

        LocalDateTime now = LocalDateTime.now();

        // 위임 필요 + 기한 지난 크루 조회
        List<Crew> expiredCrews = crewRepository
                .findAllByRequiresDelegationTrueAndDelegationDeadlineBefore(now);

        if (expiredCrews.isEmpty()) {
            log.info("위임 기한 만료 크루 없음");
            return;
        }

        int disbandedCount = 0;

        for (Crew crew : expiredCrews) {
            try {
                // 3일 지났는데 위임 안 함 → 크루 해체
                crewService.disbandCrew(crew.getId());
                disbandedCount++;

                log.info("위임 기한 만료로 크루 해체 - crewId: {}", crew.getId());

            } catch (Exception e) {
                log.error("위임 기한 만료 크루 해체 실패 - crewId: {}", crew.getId(), e);
            }
        }

        log.info("=== 총 {}건의 크루 해체 완료 (위임 기한 만료) ===", disbandedCount);
    }
}
