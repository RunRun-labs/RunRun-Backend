package com.multi.runrunbackend.domain.course.repository;

import com.multi.runrunbackend.domain.course.dto.req.CourseListReqDto;
import com.multi.runrunbackend.domain.course.dto.req.CursorPage;
import com.multi.runrunbackend.domain.course.dto.res.CourseListResDto;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : CourseRepositoryCustom
 * @since : 2025. 12. 19. Friday
 */
public interface CourseRepositoryCustom {

    CursorPage<CourseListResDto> searchCourses(CourseListReqDto req);
}
