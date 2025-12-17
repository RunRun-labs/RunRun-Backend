package com.multi.runrunbackend.domain.course.entity;

import com.multi.runrunbackend.common.entitiy.BaseSoftDeleteEntity;
import com.multi.runrunbackend.domain.user.entity.User;
import jakarta.persistence.Column;
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
 * @description : 코스 신고 엔티티. 사용자가 부적절한 코스를 신고할 때 사용되며, 신고자와 신고 사유를 기록합니다.
 * @filename : CourseSiren
 * @since : 2025. 12. 17. Wednesday
 */
@Entity
@Table(
    name = "course_siren",
    uniqueConstraints = @UniqueConstraint(columnNames = {"course_id", "reporter_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseSiren extends BaseSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(columnDefinition = "TEXT")
    private String description;
}
