package com.multi.runrunbackend.common.event;

/**
 * @author : kyungsoo
 * @description : 사용자 로그인 이벤트 (생일 쿠폰 발급용)
 * @filename : UserLoginEvent
 * @since : 2026. 1. 13. Monday
 */
public record UserLoginEvent(Long userId) {
}
