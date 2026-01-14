package com.multi.runrunbackend.domain.coupon.dto.res;

import com.multi.runrunbackend.domain.coupon.entity.Coupon;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : CouponPageResDto
 * @since : 2025. 12. 29. Monday
 */

@Getter
@AllArgsConstructor
public class CouponPageResDto {

    private List<CouponListItemResDto> items;

    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;


    public static CouponPageResDto of(Page<Coupon> page) {
        List<CouponListItemResDto> items = page.getContent().stream()
            .map(CouponListItemResDto::from)
            .toList();

        return new CouponPageResDto(
            items,
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.hasNext(),
            page.hasPrevious()
        );
    }
}
