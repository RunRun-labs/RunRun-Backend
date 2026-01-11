package com.multi.runrunbackend.domain.advertisement.service;

import com.multi.runrunbackend.common.exception.custom.FileUploadException;
import com.multi.runrunbackend.common.exception.custom.ForbiddenException;
import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.common.file.FileDomainType;
import com.multi.runrunbackend.common.file.storage.FileStorage;
import com.multi.runrunbackend.domain.advertisement.dto.req.ad.AdAdminCreateReqDto;
import com.multi.runrunbackend.domain.advertisement.dto.req.ad.AdAdminUpdateReqDto;
import com.multi.runrunbackend.domain.advertisement.dto.res.ad.AdAdminCreateResDto;
import com.multi.runrunbackend.domain.advertisement.dto.res.ad.AdAdminDetailResDto;
import com.multi.runrunbackend.domain.advertisement.dto.res.ad.AdAdminListItemResDto;
import com.multi.runrunbackend.domain.advertisement.dto.res.ad.AdAdminUpdateResDto;
import com.multi.runrunbackend.domain.advertisement.entity.Ad;
import com.multi.runrunbackend.domain.advertisement.repository.AdPlacementRepository;
import com.multi.runrunbackend.domain.advertisement.repository.AdRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : AdAdminService
 * @since : 2026. 1. 11. Sunday
 */
@Service
@RequiredArgsConstructor
public class AdAdminService {

    private final AdRepository adRepository;
    private final AdPlacementRepository placementRepository;
    private final FileStorage fileStorage;


    @Transactional
    public AdAdminCreateResDto create(AdAdminCreateReqDto dto, MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            throw new FileUploadException(ErrorCode.FILE_REQUIRED);
        }

        String uploadedKey = null;
        try {
            uploadedKey = fileStorage.upload(imageFile, FileDomainType.AD_IMAGE, null);

            Ad ad = Ad.create(dto, uploadedKey);
            Ad saved = adRepository.save(ad);
            return AdAdminCreateResDto.of(saved.getId());

        } catch (DataIntegrityViolationException | JpaSystemException e) {
            if (uploadedKey != null) {
                try {
                    fileStorage.delete(uploadedKey);
                } catch (Exception ignore) {
                }
            }
            throw new com.multi.runrunbackend.common.exception.custom.BusinessException(
                ErrorCode.AD_SAVE_FAILED);
        }
    }

    @Transactional(readOnly = true)
    public Page<AdAdminListItemResDto> list(String keyword, Pageable pageable) {
        Page<Ad> page = (keyword == null || keyword.isBlank())
            ? adRepository.findAll(pageable)
            : adRepository.findByNameContainingIgnoreCase(keyword.trim(), pageable);

        return page.map(ad -> AdAdminListItemResDto.of(
            ad.getId(),
            ad.getName(),
            ad.getRedirectUrl()
        ));
    }

    @Transactional(readOnly = true)
    public AdAdminDetailResDto detail(Long adId) {
        Ad ad = adRepository.findById(adId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.AD_NOT_FOUND));

        Object[] placementTotalsRaw = placementRepository.sumPlacementTotalsByAdId(adId);

        Object[] placementTotals;
        if (placementTotalsRaw != null && placementTotalsRaw.length > 0
            && placementTotalsRaw[0] != null && placementTotalsRaw[0].getClass().isArray()) {
            placementTotals = (Object[]) placementTotalsRaw[0];
        } else {
            placementTotals = placementTotalsRaw;
        }

        long placementCount =
            placementTotals != null && placementTotals.length > 0 ? toLong(placementTotals[0]) : 0L;
        long placementTotalImp =
            placementTotals != null && placementTotals.length > 1 ? toLong(placementTotals[1]) : 0L;
        long placementTotalClk =
            placementTotals != null && placementTotals.length > 2 ? toLong(placementTotals[2]) : 0L;
        // imageUrl은 key로 저장되어 있으므로 toHttpsUrl로 변환
        String imageUrl = fileStorage.toHttpsUrl(ad.getImageUrl());
        return AdAdminDetailResDto.of(
            ad.getId(),
            ad.getName(),
            imageUrl,
            ad.getRedirectUrl(),
            placementCount,
            placementTotalImp,
            placementTotalClk
        );
    }

    @Transactional
    public AdAdminUpdateResDto update(Long adId, AdAdminUpdateReqDto dto, MultipartFile imageFile) {
        Ad ad = adRepository.findById(adId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.AD_NOT_FOUND));

        if (placementRepository.existsByAdId(adId)) {
            throw new ForbiddenException(ErrorCode.AD_LOCKED_BY_PLACEMENT);
        }

        String finalKey = ad.getImageUrl();
        if (imageFile != null && !imageFile.isEmpty()) {
            finalKey = fileStorage.uploadIfChanged(imageFile, FileDomainType.AD_IMAGE,
                adId, ad.getImageUrl());
        }

        ad.update(dto, finalKey);
        return AdAdminUpdateResDto.of(ad.getId());
    }

    @Transactional
    public void delete(Long adId) {
        Ad ad = adRepository.findById(adId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.AD_NOT_FOUND));

        if (placementRepository.existsByAdId(adId)) {
            throw new ForbiddenException(ErrorCode.AD_LOCKED_BY_PLACEMENT);
        }

        ad.delete();
    }

    private long toLong(Object obj) {
        if (obj == null) {
            return 0L;
        }
        if (obj instanceof Number) {
            return ((Number) obj).longValue();
        }
        try {
            return Long.parseLong(obj.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
