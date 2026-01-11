package com.multi.runrunbackend.domain.advertisement.service;

import com.multi.runrunbackend.domain.advertisement.constant.AdDailySort;
import com.multi.runrunbackend.domain.advertisement.constant.SortDir;
import com.multi.runrunbackend.domain.advertisement.dto.res.adstats.AdDailyStatsListItemResDto;
import com.multi.runrunbackend.domain.advertisement.dto.res.common.PageResDto;
import com.multi.runrunbackend.domain.advertisement.repository.AdDailyStatsRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : AdDailyStatsService
 * @since : 2026. 1. 11. Sunday
 */
@Service
@RequiredArgsConstructor
public class AdDailyStatsService {

    private final AdDailyStatsRepository adDailyStatsRepository;

    @Transactional(readOnly = true)
    public PageResDto<AdDailyStatsListItemResDto> listByPlacement(
        Long placementId,
        LocalDate from,
        LocalDate to,
        AdDailySort sort,
        SortDir dir,
        int page,
        int size
    ) {
        // 기본값 설정
        LocalDate f = (from == null) ? LocalDate.now().minusDays(30) : from;
        LocalDate t = (to == null) ? LocalDate.now() : to;
        AdDailySort s = (sort == null) ? AdDailySort.DATE : sort;
        SortDir d = (dir == null) ? SortDir.DESC : dir;

        Sort.Direction direction = (d == SortDir.ASC) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sortObj = switch (s) {
            case IMPRESSIONS -> Sort.by(direction, "impressions");
            case CLICKS -> Sort.by(direction, "clicks");
            case DATE -> Sort.by(direction, "statDate");
        };

        Pageable pageable = PageRequest.of(page, size, sortObj);
        var result = adDailyStatsRepository.findByPlacementIdAndStatDateBetween(placementId, f, t, pageable)
            .map(AdDailyStatsListItemResDto::from);

        return PageResDto.of(result);
    }
}
