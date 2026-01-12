package com.multi.runrunbackend.domain.course.dto.req;

import com.multi.runrunbackend.domain.course.constant.CourseRegisterType;
import com.multi.runrunbackend.domain.course.constant.CourseSortType;
import java.util.List;
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


    private List<CourseRegisterType> registerTypes;
    private Boolean nearby;
    private Double lat;
    private Double lng;
    private Integer radiusM;

    private String distanceBucket;


    private CourseSortType sortType;


    private String cursor;
    private Integer size;

    private Boolean myCourses;
    private Boolean myLikedCourses;
    private Boolean myFavoritedCourses;


    public static CourseListReqDto fromParams(
        String keyword,
        List<CourseRegisterType> registerTypes,
        Boolean nearby,
        Double lat,
        Double lng,
        Integer radiusM,
        String distanceBucket,
        CourseSortType sortType,
        String cursor,
        Integer size,
        Boolean myCourses,
        Boolean myLikedCourses,
        Boolean myFavoritedCourses
    ) {
        return CourseListReqDto.builder()
            .keyword(normalizeBlank(keyword))
            .registerTypes(registerTypes)
            .nearby(nearby)
            .lat(lat)
            .lng(lng)
            .radiusM(radiusM)
            .distanceBucket(normalizeBlank(distanceBucket))
            .sortType(sortType)
            .cursor(normalizeBlank(cursor))
            .size(normalizeSize(size))
            .myCourses(myCourses)
            .myLikedCourses(myLikedCourses)
            .myFavoritedCourses(myFavoritedCourses)
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


