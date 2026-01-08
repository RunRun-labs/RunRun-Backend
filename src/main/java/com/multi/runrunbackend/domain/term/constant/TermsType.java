package com.multi.runrunbackend.domain.term.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author : kimyongwon
 * @description : 약관 타입 관리 Enum (서비스 이용약관, 개인정보처리방침, 유료 서비스 이용약관)
 * @filename : TermsType
 * @since : 25. 12. 17. 오후 1:24 수요일
 */
@Getter
@RequiredArgsConstructor
public enum TermsType {
    SERVICE("서비스 이용약관"),
    PRIVACY("개인정보 처리방침"),
    MARKETING("유료 서비스 이용약관");

    private final String description;
}
