package com.multi.runrunbackend.domain.match.dto.req;

import com.multi.runrunbackend.common.constant.DistanceType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author : KIMGWANGHO
 * @description : Please explain the class!!!
 * @filename : SoloRunStartReqDto
 * @since : 2026-01-01 목요일
 */
@Getter
@NoArgsConstructor
public class SoloRunStartReqDto {

    private DistanceType distance;

    @DecimalMin(value = "0.1", message = "목표 거리는 최소 0.1km(100m) 이상이어야 합니다")
    private Double manualDistance;

    private Long courseId;

    /**
     * 코스 선택, 거리 선택, 직접 입력 중 하나는 필수입니다.
     */
    @AssertTrue(message = "코스 선택, 거리 선택, 직접 입력 중 하나는 필수입니다")
    private boolean isValidDistanceInput() {
        return courseId != null || distance != null || manualDistance != null;
    }
}
