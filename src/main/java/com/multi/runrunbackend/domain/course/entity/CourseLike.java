package com.multi.runrunbackend.domain.course.entity;

import com.multi.runrunbackend.common.entitiy.BaseTimeEntity;
import com.multi.runrunbackend.domain.user.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author : kyungsoo
 * @description : 코스 좋아요 엔티티
 * @filename : CourseSiren
 * @since : 2025. 12. 17. Wednesday
 */
@Entity
@Table(
    name = "course_like",
    uniqueConstraints = @UniqueConstraint(columnNames = {"course_id", "user_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseLike extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public static CourseLike create(User user, Course course) {
        CourseLike courseLike = new CourseLike();
        courseLike.course = course;
        courseLike.user = user;

        return courseLike;

    }

}
