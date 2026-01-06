package com.multi.runrunbackend.domain.coupon.respository;

import com.multi.runrunbackend.domain.coupon.dto.res.CouponIssueListItemResDto;
import com.multi.runrunbackend.domain.coupon.dto.res.CouponIssueListReqDto;
import com.multi.runrunbackend.domain.course.dto.req.CursorPage;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : CouponIssueRepositoryCustom
 * @since : 2025. 12. 29. Monday
 */
public interface CouponIssueRepositoryCustom {

    CursorPage<CouponIssueListItemResDto> searchIssuedCoupons(Long userId,
        CouponIssueListReqDto req);
}