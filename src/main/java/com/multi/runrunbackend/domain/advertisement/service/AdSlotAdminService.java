package com.multi.runrunbackend.domain.advertisement.service;

import com.multi.runrunbackend.common.exception.custom.ForbiddenException;
import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.advertisement.constant.AdSlotStatus;
import com.multi.runrunbackend.domain.advertisement.constant.AdSlotType;
import com.multi.runrunbackend.domain.advertisement.dto.req.adslot.AdSlotAdminCreateReqDto;
import com.multi.runrunbackend.domain.advertisement.dto.req.adslot.AdSlotAdminUpdateReqDto;
import com.multi.runrunbackend.domain.advertisement.dto.res.adslot.AdSlotAdminListItemResDto;
import com.multi.runrunbackend.domain.advertisement.dto.res.common.PageResDto;
import com.multi.runrunbackend.domain.advertisement.entity.AdSlot;
import com.multi.runrunbackend.domain.advertisement.repository.AdPlacementRepository;
import com.multi.runrunbackend.domain.advertisement.repository.AdSlotRepository;
import com.multi.runrunbackend.domain.advertisement.spec.AdSlotSpecs;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : AdSlotAdminService
 * @since : 2026. 1. 11. Sunday
 */
@Service
@RequiredArgsConstructor
public class AdSlotAdminService {

    private final AdSlotRepository adSlotAdminRepository;
    private final AdPlacementRepository adPlacementAdminRepository;

    @Transactional
    public Long create(AdSlotAdminCreateReqDto dto) {
        // 같은 타입의 슬롯이 이미 존재하는지 확인
        if (adSlotAdminRepository.findBySlotType(dto.getSlotType()).isPresent()) {
            throw new ForbiddenException(ErrorCode.AD_SLOT_TYPE_ALREADY_EXISTS);
        }
        
        AdSlot slot = AdSlot.create(dto);
        return adSlotAdminRepository.save(slot).getId();
    }

    @Transactional(readOnly = true)
    public PageResDto<AdSlotAdminListItemResDto> list(
        int page,
        int size,
        String keyword,
        AdSlotType slotType,
        AdSlotStatus status
    ) {
        Specification<AdSlot> spec = Specification
            .where(AdSlotSpecs.keyword(keyword))
            .and(AdSlotSpecs.slotType(slotType))
            .and(AdSlotSpecs.status(status));

        Page<AdSlotAdminListItemResDto> result = adSlotAdminRepository.findAll(
                spec,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
            )
            .map(AdSlotAdminListItemResDto::from);

        return PageResDto.of(result);
    }

    @Transactional
    public void update(Long slotId, AdSlotAdminUpdateReqDto dto) {
        AdSlot slot = adSlotAdminRepository.findById(slotId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.AD_SLOT_NOT_FOUND));

        // 운영중(ENABLED)인 슬롯은 먼저 비활성화하고 수정하라는 정책이면 여기서 막기
        if (slot.isEnabled()) {
            throw new ForbiddenException(ErrorCode.AD_SLOT_UPDATE_ONLY_DISABLED);
        }

        // 이미 placement가 걸려있는 슬롯이면 수정 금지(너가 말한 정책)
        if (adPlacementAdminRepository.existsBySlotId(slotId)) {
            throw new ForbiddenException(ErrorCode.AD_SLOT_IN_USE);
        }

        slot.update(dto);
    }

    @Transactional
    public void disable(Long slotId) {
        AdSlot slot = adSlotAdminRepository.findById(slotId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.AD_SLOT_NOT_FOUND));
        if (slot.isEnabled()) {
            throw new ForbiddenException(ErrorCode.AD_SLOT_UPDATE_ONLY_DISABLED);
        }
        slot.disable();
    }

    @Transactional
    public void enable(Long slotId) {
        AdSlot slot = adSlotAdminRepository.findById(slotId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.AD_SLOT_NOT_FOUND));
        slot.enable();
    }
}
