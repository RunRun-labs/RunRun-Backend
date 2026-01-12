package com.multi.runrunbackend.domain.advertisement.service;

import com.multi.runrunbackend.common.exception.custom.ForbiddenException;
import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.advertisement.constant.AdSlotType;
import com.multi.runrunbackend.domain.advertisement.dto.res.adserve.AdServeResDto;
import com.multi.runrunbackend.domain.advertisement.entity.AdPlacement;
import com.multi.runrunbackend.domain.advertisement.repository.AdDailyStatsRepository;
import com.multi.runrunbackend.domain.advertisement.repository.AdPlacementRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : AdTrackingService
 * @since : 2026. 1. 11. Sunday
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class AdTrackingService {

    private final AdPlacementRepository placementRepository;
    private final AdDailyStatsRepository dailyStatsRepository;

    /**
     * serve = 노출로 간주 - 후보들 중 weight 기반으로 1개 선택 - 같은 트랜잭션에서 placement.totalImpressions +1,
     * daily.impressions +1
     */
    @Transactional
    public AdServeResDto serveOne(AdSlotType slotType) {
        LocalDateTime now = LocalDateTime.now();
        List<AdPlacement> candidates = placementRepository.findServeCandidates(slotType, now);

        if (candidates.isEmpty()) {
            throw new NotFoundException(ErrorCode.AD_SERVE_NOT_FOUND);
        }

        AdPlacement picked = pickByWeight(candidates);

        int updated = placementRepository.increaseTotalImpression(picked.getId());
        if (updated != 1) {
            throw new ForbiddenException(ErrorCode.AD_PLACEMENT_NOT_FOUND);
        }
        dailyStatsRepository.upsertDaily(picked.getId(), LocalDate.now(), 1, 0);

        return AdServeResDto.of(
            picked.getId(),
            picked.getAd().getId(),
            picked.getAd().getName(),
            picked.getAd().getImageUrl(),
            picked.getAd().getRedirectUrl()
        );
    }


    @Transactional
    public void click(Long placementId) {
        int updated = placementRepository.increaseTotalClick(placementId);
        if (updated != 1) {
            throw new NotFoundException(ErrorCode.AD_PLACEMENT_NOT_FOUND);
        }
        int row = dailyStatsRepository.upsertDaily(placementId, LocalDate.now(), 0, 1);
        log.info("row : ->>>>>>>>>>>>>>>>>>>>>>>>>>" + row);
    }

    private AdPlacement pickByWeight(List<AdPlacement> list) {
        long total = 0;
        for (AdPlacement p : list) {
            int w = (p.getWeight() == null || p.getWeight() < 1) ? 1 : p.getWeight();
            total += w;
        }

        long r = ThreadLocalRandom.current().nextLong(total);
        long acc = 0;
        for (AdPlacement p : list) {
            int w = (p.getWeight() == null || p.getWeight() < 1) ? 1 : p.getWeight();
            acc += w;
            if (r < acc) {
                return p;
            }
        }
        return list.get(0);
    }
}
