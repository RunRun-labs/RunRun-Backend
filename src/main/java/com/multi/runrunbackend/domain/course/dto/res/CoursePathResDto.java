package com.multi.runrunbackend.domain.course.dto.res;

import com.multi.runrunbackend.domain.course.entity.Course;
import com.multi.runrunbackend.domain.course.util.GeoJsonConverter;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author : KIMGWANGHO
 * @description : 코스 경로 정보 응답 DTO
 * @filename : CoursePathResDto
 * @since : 2025-12-29
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoursePathResDto {

    private Long courseId;
    private Map<String, Object> path; // GeoJSON 형식
    private Double startLat;
    private Double startLng;
    private Integer distanceM;

    public static CoursePathResDto from(Course course) {
        return CoursePathResDto.builder()
            .courseId(course.getId())
            .path(GeoJsonConverter.toGeoJson(course.getPath()))
            .startLat(course.getStartLat())
            .startLng(course.getStartLng())
            .distanceM(course.getDistanceM())
            .build();

    }
}

