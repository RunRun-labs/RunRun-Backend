package com.multi.runrunbackend.common.event;

import java.math.BigDecimal;

/**
 * @author : kyungsoo
 * @description : 러닝 결과 완료 이벤트 (첫 러닝, 거리 달성 쿠폰 발급용)
 * @filename : RunningResultCompletedEvent
 * @since : 2026. 1. 13. Monday
 */
public record RunningResultCompletedEvent(
    Long userId,
    BigDecimal totalDistance // km 단위
) {
}

