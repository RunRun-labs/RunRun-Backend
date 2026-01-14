package com.multi.runrunbackend.domain.advertisement.service;

import com.multi.runrunbackend.common.exception.custom.ForbiddenException;
import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.common.file.storage.FileStorage;
import com.multi.runrunbackend.domain.advertisement.constant.AdSlotType;
import com.multi.runrunbackend.domain.advertisement.dto.res.adserve.AdServeResDto;
import com.multi.runrunbackend.domain.advertisement.entity.AdPlacement;
import com.multi.runrunbackend.domain.advertisement.entity.AdSlot;
import com.multi.runrunbackend.domain.advertisement.repository.AdDailyStatsRepository;
import com.multi.runrunbackend.domain.advertisement.repository.AdPlacementRepository;
import com.multi.runrunbackend.domain.advertisement.repository.AdSlotRepository;
import com.multi.runrunbackend.domain.membership.constant.MembershipStatus;
import com.multi.runrunbackend.domain.membership.repository.MembershipRepository;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
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
    private final FileStorage fileStorage;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final AdSlotRepository slotRepository;

    /**
     * serve = 노출로 간주 - 후보들 중 weight 기반으로 1개 선택 - 같은 트랜잭션에서 placement.totalImpressions +1,
     * daily.impressions +1
     */
    @Transactional
    public AdServeResDto serveOne(AdSlotType slotType, Long userId) {
        LocalDateTime now = LocalDateTime.now();

        checkDailyLimit(slotType, LocalDate.now());

        List<AdPlacement> candidates = placementRepository.findServeCandidates(slotType, now);

        List<AdPlacement> filteredCandidates = filterCandidatesByPremium(candidates, userId);

        if (filteredCandidates.isEmpty()) {
            throw new NotFoundException(ErrorCode.AD_SERVE_NOT_FOUND);
        }

        AdPlacement picked = pickByWeight(filteredCandidates);

        int updated = placementRepository.increaseTotalImpression(picked.getId());
        if (updated != 1) {
            throw new ForbiddenException(ErrorCode.AD_PLACEMENT_NOT_FOUND);
        }
        dailyStatsRepository.upsertDaily(picked.getId(), LocalDate.now(), 1, 0);
        String imageUrl = fileStorage.toHttpsUrl(picked.getAd().getImageUrl());
        return AdServeResDto.of(
            picked.getId(),
            picked.getAd().getId(),
            picked.getAd().getName(),
            imageUrl,
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

    /**
     * dailyLimit 체크 (0이면 무제한)
     */
    private void checkDailyLimit(AdSlotType slotType, LocalDate today) {
        AdSlot slot = slotRepository.findBySlotType(slotType)
            .orElse(null);

        if (slot == null) {
            return;  // 슬롯이 없으면 체크하지 않음
        }

        Integer dailyLimit = slot.getDailyLimit();
        // ✅ dailyLimit이 0이거나 null이면 무제한 (체크 안 함)
        if (dailyLimit == null || dailyLimit == 0) {
            return;
        }

        // ✅ 오늘의 노출 수 확인
        Long todayImpressions = dailyStatsRepository.sumTodayImpressionsBySlotType(slotType, today);
        if (todayImpressions == null) {
            todayImpressions = 0L;
        }

        // ✅ dailyLimit 초과 시 예외 발생
        if (todayImpressions >= dailyLimit) {
            throw new NotFoundException(ErrorCode.AD_SERVE_NOT_FOUND);
        }
    }

    /**
     * 프리미엄 사용자인 경우 allowPremium=false인 후보 필터링
     */
    private List<AdPlacement> filterCandidatesByPremium(
        List<AdPlacement> candidates,
        Long userId
    ) {
        // ✅ userId가 없으면 모든 후보 반환
        if (userId == null) {
            log.debug("[AdTracking] userId is null, returning all candidates");
            return candidates;
        }

        // ✅ 프리미엄 사용자 여부 확인
        boolean isPremium = checkPremiumMembership(userId);
        log.info("[AdTracking] userId={}, isPremium={}, candidatesCount={}",
            userId, isPremium, candidates.size());

        if (!isPremium) {
            log.debug("[AdTracking] user is not premium, returning all candidates");
            return candidates;
        }

        // ✅ 프리미엄 사용자인 경우 allowPremium=false인 후보 제외
        List<AdPlacement> filtered = candidates.stream()
            .filter(placement -> {
                Boolean allowPremium = placement.getSlot().getAllowPremium();
                boolean allowed = Boolean.TRUE.equals(allowPremium);
                log.debug("[AdTracking] placementId={}, slotType={}, allowPremium={}, allowed={}",
                    placement.getId(), placement.getSlot().getSlotType(), allowPremium, allowed);
                return allowed;  // allowPremium=true만 통과
            })
            .toList();

        log.info("[AdTracking] premium user filtered: {} -> {} candidates",
            candidates.size(), filtered.size());
        return filtered;
    }

    /**
     * 프리미엄 멤버십 확인
     */
    private boolean checkPremiumMembership(Long userId) {
        boolean result = userRepository.findById(userId)
            .flatMap(user -> {
                log.debug("[AdTracking] checking membership for userId={}", userId);
                return membershipRepository.findByUser(user);
            })
            .map(membership -> {
                MembershipStatus status = membership.getMembershipStatus();
                LocalDateTime endDate = membership.getEndDate();
                LocalDateTime now = LocalDateTime.now();

                log.info("[AdTracking] membership found: userId={}, status={}, endDate={}, now={}",
                    userId, status, endDate, now);

                if (status == MembershipStatus.ACTIVE) {
                    log.debug("[AdTracking] membership is ACTIVE, user is premium");
                    return true;
                }
                if (status == MembershipStatus.CANCELED
                    && endDate != null
                    && endDate.isAfter(now)) {
                    log.debug(
                        "[AdTracking] membership is CANCELED but endDate is in future, user is premium");
                    return true;
                }
                log.debug("[AdTracking] membership is not active or expired, user is not premium");
                return false;
            })
            .orElse(false);

        log.info("[AdTracking] checkPremiumMembership result: userId={}, isPremium={}", userId,
            result);
        return result;
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
