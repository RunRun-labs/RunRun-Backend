package com.multi.runrunbackend.domain.advertisement.scheduler;

import com.multi.runrunbackend.domain.advertisement.repository.AdPlacementRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author : kyungsoo
 * @description : 광고 배치 마감일 지난 것 자동 비활성화 스케줄러
 * @filename : AdPlacementScheduler
 * @since : 2026. 1. 12. Sunday
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdPlacementScheduler {

    private final AdPlacementRepository adPlacementRepository;

    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Seoul")
    @Transactional
    public void disableExpiredPlacements() {
        LocalDateTime now = LocalDateTime.now();
        int count = adPlacementRepository.disableExpired(now);

        if (count > 0) {
            log.info("[AdPlacementScheduler] disabled {} expired placements", count);
        }
    }
}