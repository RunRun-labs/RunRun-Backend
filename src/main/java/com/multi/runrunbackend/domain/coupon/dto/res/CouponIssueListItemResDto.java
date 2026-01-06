package com.multi.runrunbackend.domain.coupon.dto.res;

import com.multi.runrunbackend.domain.coupon.constant.CouponBenefitType;
import com.multi.runrunbackend.domain.coupon.constant.CouponChannel;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : CouponIssueListItemResDto
 * @since : 2025. 12. 29. Monday
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponIssueListItemResDto {

    private Long id;
    private String name;
    private Integer benefitValue;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private CouponBenefitType benefitType;
    private CouponChannel couponChannel;
}
