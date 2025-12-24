package com.multi.runrunbackend.domain.course.repository;

import com.multi.runrunbackend.domain.course.entity.CourseSiren;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : CourseSirenRepository
 * @since : 2025. 12. 23. Tuesday
 */
public interface CourseSirenRepository extends JpaRepository<CourseSiren, Long> {


    boolean existsByCourse_IdAndUser_Id(Long courseId, Long userId);
}
