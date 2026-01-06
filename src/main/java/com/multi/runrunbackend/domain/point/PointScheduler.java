package com.multi.runrunbackend.domain.point;

import com.multi.runrunbackend.domain.point.service.PointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author : BoKyung
 * @description : 포인트 스케줄러
 * @since : 2026. 01. 06. 화요일
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PointScheduler {

    private final PointService pointService;

    /**
     * @description : 포인트 만료 처리
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void processExpiredPoints() {

        log.info("=== 포인트 만료 처리 스케줄러 시작 ===");

        pointService.expirePoints();

        log.info("=== 포인트 만료 처리 스케줄러 종료 ===");
    }
}
