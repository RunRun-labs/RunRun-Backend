package com.multi.runrunbackend.domain.course.entity;

import com.multi.runrunbackend.common.entitiy.BaseSoftDeleteEntity;
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
 * @description : 코스 즐겨찾기 엔티티
 * @filename : CourseFavorite
 * @since : 2025. 12. 17. Wednesday
 */
@Entity
@Table(
    name = "course_favorite",
    uniqueConstraints = @UniqueConstraint(columnNames = {"course_id", "user_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseFavorite extends BaseSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public static CourseFavorite create(User user, Course course) {
        CourseFavorite courseFavorite = new CourseFavorite();
        courseFavorite.course = course;
        courseFavorite.user = user;

        return courseFavorite;

    }
}
