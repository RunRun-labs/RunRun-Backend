package com.multi.runrunbackend.domain.advertisement.dto.res.common;

import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : PageResDto
 * @since : 2026. 1. 11. Sunday
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PageResDto<T> {

    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;
    private List<T> content;

    public static <T> PageResDto<T> of(Page<T> pageData) {
        return new PageResDto<>(
            pageData.getNumber(),
            pageData.getSize(),
            pageData.getTotalElements(),
            pageData.getTotalPages(),
            pageData.hasNext(),
            pageData.hasPrevious(),
            pageData.getContent()
        );
    }

    public static <T> PageResDto<T> of(
        List<T> content,
        int page,
        int size,
        long totalElements
    ) {
        int totalPages = (size <= 0) ? 0 : (int) Math.ceil((double) totalElements / size);
        boolean hasPrevious = page > 0;
        boolean hasNext = (page + 1) < totalPages;

        return new PageResDto<>(
            page,
            size,
            totalElements,
            totalPages,
            hasNext,
            hasPrevious,
            content
        );
    }
}
