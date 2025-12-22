package com.multi.runrunbackend.domain.course.dto.req;

import com.multi.runrunbackend.domain.course.constant.CourseRegisterType;
import com.multi.runrunbackend.domain.course.constant.CourseSortType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : CourseListReqDto
 * @since : 2025. 12. 19. Friday
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseListReqDto {


    private String keyword;


    private CourseRegisterType registerType;
    private Boolean nearby;
    private Double lat;
    private Double lng;
    private Integer radiusM;

    private String distanceBucket;


    private CourseSortType sortType;


    private String cursor;
    private Integer size;


    public static CourseListReqDto fromParams(
        String keyword,
        CourseRegisterType registerType,
        Boolean nearby,
        Double lat,
        Double lng,
        Integer radiusM,
        String distanceBucket,
        CourseSortType sortType,
        String cursor,
        Integer size
    ) {
        return CourseListReqDto.builder()
            .keyword(normalizeBlank(keyword))
            .registerType(registerType)
            .nearby(nearby)
            .lat(lat)
            .lng(lng)
            .radiusM(radiusM)
            .distanceBucket(normalizeBlank(distanceBucket))
            .sortType(sortType)
            .cursor(normalizeBlank(cursor))
            .size(normalizeSize(size))
            .build();
    }


    private static String normalizeBlank(String v) {
        if (v == null) {
            return null;
        }
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    private static Integer normalizeSize(Integer size) {
        if (size == null || size <= 0) {
            return 10;
        }
        return Math.min(size, 50);
    }

}


