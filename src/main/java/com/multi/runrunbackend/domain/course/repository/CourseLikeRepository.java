package com.multi.runrunbackend.domain.course.repository;

import com.multi.runrunbackend.domain.course.entity.CourseLike;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : CourseLikeRepository
 * @since : 2025. 12. 22. Monday
 */
public interface CourseLikeRepository extends JpaRepository<CourseLike, Long> {

    boolean existsByCourse_IdAndUser_Id(Long courseId, Long userId);

    int deleteByCourseIdAndUserId(Long courseId, Long userId);
}
