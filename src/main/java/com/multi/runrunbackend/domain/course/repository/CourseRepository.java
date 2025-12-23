package com.multi.runrunbackend.domain.course.repository;

import com.multi.runrunbackend.domain.course.entity.Course;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : CourseRepository
 * @since : 2025. 12. 18. Thursday
 */
public interface CourseRepository extends JpaRepository<Course, Long> {

    @Modifying
    @Query("""
            update Course c
            set c.likeCount = c.likeCount + 1
            where c.id = :courseId
        """)
    int increaseLikeCount(@Param("courseId") Long courseId);

    @Modifying
    @Query("""
            update Course c
            set c.likeCount = 
                case when c.likeCount > 0 then c.likeCount - 1 else 0 end
            where c.id = :courseId
        """)
    int decreaseLikeCount(@Param("courseId") Long courseId);

    @Modifying
    @Query("""
            update Course c
            set c.favoriteCount = c.favoriteCount + 1
            where c.id = :courseId
        """)
    int increaseFavoriteCount(@Param("courseId") Long courseId);

    @Modifying
    @Query("""
            update Course c
            set c.favoriteCount = 
                case when c.favoriteCount > 0 then c.favoriteCount - 1 else 0 end
            where c.id = :courseId
        """)
    int decreaseFavoriteCount(@Param("courseId") Long courseId);


}
