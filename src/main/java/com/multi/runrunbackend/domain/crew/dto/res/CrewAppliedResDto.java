package com.multi.runrunbackend.domain.crew.dto.res;

import com.multi.runrunbackend.domain.crew.constant.CrewJoinState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author : BoKyung
 * @description : 크루 가입 신청 상태 응답 DTO
 * @filename : CrewAppliedResDto
 * @since : 25. 12. 23. 화요일
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrewAppliedResDto {

    /**
     * 크루 가입 상태
     */
    private CrewJoinState crewJoinState;
}
