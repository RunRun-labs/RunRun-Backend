package com.multi.runrunbackend.domain.match.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author : chang
 * @description : 런닝 상태관리 ENUM
 * @filename : RunStatus
 * @since : 2025-12-17 수요일
 */
@Getter
@RequiredArgsConstructor
public enum RunStatus {
    IN_PROGRESS("진행중"),
    COMPLETED("완료"),
    GIVE_UP("포기"),
    CANCELLED("취소");

    private final String description;
}