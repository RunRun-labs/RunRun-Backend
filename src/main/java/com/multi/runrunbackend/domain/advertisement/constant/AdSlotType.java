package com.multi.runrunbackend.domain.advertisement.constant;

/**
 * @author : kyungsoo
 * @description : 광고 슬롯이 들어갈 위치 BANNER_TOP, BANNER_MID, BANNER_BOTTOM, BANNER_CAROUSEL, LIST
 * @filename : AdSlotType
 * @since : 2025. 12. 17. Wednesday
 */
public enum AdSlotType {
    FEED_LIST_ITEM,     // 피드 리스트 중간 삽입 광고 (n번째마다)

    RUN_END_BANNER,     // 러닝 종료 후 결과 화면 배너

    HOME_TOP_BANNER,    // 홈 화면 상단 배너

    COUPON_LIST_BANNER  // 쿠폰 리스트 상단/중단 배너
}
