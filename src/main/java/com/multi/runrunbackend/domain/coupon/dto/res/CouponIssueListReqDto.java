package com.multi.runrunbackend.domain.coupon.dto.res;

import com.multi.runrunbackend.domain.coupon.constant.CouponBenefitType;
import com.multi.runrunbackend.domain.coupon.constant.CouponChannel;
import com.multi.runrunbackend.domain.coupon.constant.CouponIssueSortType;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : CouponIssueListReqDto
 * @since : 2025. 12. 29. Monday
 */

@Data
public class CouponIssueListReqDto {

    private Integer size;
    private String cursor;
    private CouponIssueSortType sortType;
    
    private List<CouponBenefitType> benefitTypes;
    private List<CouponChannel> couponChannels;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startTo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endTo;
}