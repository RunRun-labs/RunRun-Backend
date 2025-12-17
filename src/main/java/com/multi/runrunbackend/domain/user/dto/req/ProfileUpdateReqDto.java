package com.multi.runrunbackend.domain.user.dto.req;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 *
 * @author : kimyongwon
 * @description : 사용자 프로필 수정 요청 DTO
 * - 키/몸무게 수정
 * - 닉네임 수정 추후 확장
 * - 상한선은 비즈니스 확정 후 추가
 * @filename : ProfileUpdateReqDto
 * @since : 25. 12. 17. 오후 5:48 수요일
 */
@Getter
@NoArgsConstructor
public class ProfileUpdateReqDto {

    @Min(value = 50, message = "키는 50cm 이상이어야 합니다.")
    private Integer heightCm;

    @Max(value = 500, message = "몸무게는 500kg 이하이어야 합니다.")
    private Integer weightKg;
}