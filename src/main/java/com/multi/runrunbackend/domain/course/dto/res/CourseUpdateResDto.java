package com.multi.runrunbackend.domain.course.dto.res;

import com.multi.runrunbackend.domain.course.entity.Course;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : CourseUpdateResDto
 * @since : 2025. 12. 20. Saturday
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CourseUpdateResDto {

    private Long courseId;

    public static CourseUpdateResDto from(Course course) {
        return CourseUpdateResDto.builder()
            .courseId(course.getId())
            .build();
    }

}
