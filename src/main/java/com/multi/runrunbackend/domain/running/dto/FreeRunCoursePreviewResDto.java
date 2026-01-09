package com.multi.runrunbackend.domain.running.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 자유러닝(코스 없음) 종료 후: 방장 GPS 트랙 기반으로 코스 프리뷰를 생성해 반환한다.
 * - 프론트는 이 데이터를 "코스 저장 모달"에 주입하고, 사용자가 필수 입력을 채운 뒤 /api/courses 로 저장한다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FreeRunCoursePreviewResDto {

    /**
     * CourseCreateReqDto.path 로 그대로 저장 가능한 문자열
     * - GeometryParser가 파싱 가능한 GeoJSON(LineString) 문자열
     */
    private String path;

    private Integer distanceM;

    private Double startLat;
    private Double startLng;
}


