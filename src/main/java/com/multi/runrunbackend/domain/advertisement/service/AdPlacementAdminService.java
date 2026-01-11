package com.multi.runrunbackend.domain.advertisement.service;

import static java.time.LocalDateTime.now;

import com.multi.runrunbackend.common.exception.custom.ForbiddenException;
import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.advertisement.constant.AdSlotType;
import com.multi.runrunbackend.domain.advertisement.dto.req.adplacement.AdPlacementAdminCreateReqDto;
import com.multi.runrunbackend.domain.advertisement.dto.req.adplacement.AdPlacementAdminListItemProjection;
import com.multi.runrunbackend.domain.advertisement.dto.req.adplacement.AdPlacementAdminUpdateReqDto;
import com.multi.runrunbackend.domain.advertisement.dto.res.adplacement.AdPlacementAdminListItemResDto;
import com.multi.runrunbackend.domain.advertisement.dto.res.common.PageResDto;
import com.multi.runrunbackend.domain.advertisement.entity.Ad;
import com.multi.runrunbackend.domain.advertisement.entity.AdPlacement;
import com.multi.runrunbackend.domain.advertisement.entity.AdSlot;
import com.multi.runrunbackend.domain.advertisement.repository.AdPlacementRepository;
import com.multi.runrunbackend.domain.advertisement.repository.AdRepository;
import com.multi.runrunbackend.domain.advertisement.repository.AdSlotRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : AdPlacementAdminService
 * @since : 2026. 1. 11. Sunday
 */

@Service
@RequiredArgsConstructor
public class AdPlacementAdminService {

    private final AdPlacementRepository adPlacementAdminRepository;
    private final AdSlotRepository adSlotAdminRepository;
    private final AdRepository adAdminRepository;

    @Transactional
    public Long create(AdPlacementAdminCreateReqDto dto) {
        AdSlot slot = adSlotAdminRepository.findById(dto.getSlotId())
            .orElseThrow(() -> new NotFoundException(ErrorCode.AD_SLOT_NOT_FOUND));

        Ad ad = adAdminRepository.findById(dto.getAdId())
            .orElseThrow(() -> new NotFoundException(ErrorCode.AD_NOT_FOUND));

        AdPlacement placement = AdPlacement.create(slot, ad, dto);
        return adPlacementAdminRepository.save(placement).getId();
    }

    @Transactional(readOnly = true)
    public PageResDto<AdPlacementAdminListItemResDto> listPlacements(
        int page,
        int size,
        Boolean isActive,
        AdSlotType slotType,
        String keyword
    ) {
        Pageable pageable = PageRequest.of(
            page,
            size,
            Sort.by(Sort.Direction.DESC, "id")
        );

        String kw = normalize(keyword);

        Page<AdPlacementAdminListItemProjection> p =
            adPlacementAdminRepository.searchAdminList(isActive, slotType, kw, pageable);

        Page<AdPlacementAdminListItemResDto> mapped =
            p.map(AdPlacementAdminListItemResDto::from);

        return PageResDto.of(mapped);
    }

    private String normalize(String keyword) {
        if (keyword == null) {
            return "";
        }
        String t = keyword.trim();
        return t.isBlank() ? "" : t;
    }

    @Transactional
    public void update(Long placementId, AdPlacementAdminUpdateReqDto dto) {

        AdPlacement p = adPlacementAdminRepository.findDetailById(placementId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.AD_PLACEMENT_NOT_FOUND));
        if (Boolean.TRUE.equals(p.getIsActive())) {
            throw new ForbiddenException(ErrorCode.AD_PLACEMENT_UPDATE_ONLY_DISABLED);
        }
        LocalDateTime now = LocalDateTime.now();
        boolean hasStarted = now.isAfter(p.getStartAt()) || now.isEqual(p.getStartAt());

        if (!hasStarted) {

            Ad ad = adAdminRepository.findById(dto.getAdId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.AD_NOT_FOUND));
            AdSlot adSlot = adSlotAdminRepository.findById(dto.getSlotId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.AD_SLOT_NOT_FOUND));

            p.update(dto, ad, adSlot);
        } else {
            throw new ForbiddenException(ErrorCode.AD_PLACEMENT_UPDATE_NOT_ALLOWED_DURING_PERIOD);
        }

    }

    @Transactional
    public void disable(Long placementId) {
        AdPlacement p = adPlacementAdminRepository.findById(placementId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.AD_PLACEMENT_NOT_FOUND));
        p.disable();
    }

    @Transactional
    public void enable(Long placementId) {
        AdPlacement p = adPlacementAdminRepository.findById(placementId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.AD_PLACEMENT_NOT_FOUND));

        // 운영 기간 밖이면 활성화 금지
        if (!p.isWithinPeriod(now())) {
            throw new ForbiddenException(ErrorCode.AD_PLACEMENT_OUT_OF_PERIOD);
        }

        p.enable();
    }
}

