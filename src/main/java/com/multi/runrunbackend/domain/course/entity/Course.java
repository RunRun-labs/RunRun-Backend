package com.multi.runrunbackend.domain.course.entity;

import com.multi.runrunbackend.common.entitiy.BaseEntity;
import com.multi.runrunbackend.domain.course.constant.CourseStatus;
import com.multi.runrunbackend.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author : kyungsoo
 * @description : 사용자가 생성한 러닝 코스 엔티티. 코스의 기본 정보(제목, 설명, 거리, 시작 좌표)와 GPS 경로(LineString)를 저장하며,
 * 상태값(CourseStatus)을 통해 노출/차단/삭제를 관리한다.
 * @filename : Course
 * @since : 2025. 12. 17. Wednesday
 */
@Table(name = "course")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class Course extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;


    @Column(nullable = false, length = 200)
    private String title;


    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;


    @Column(columnDefinition = "geometry(LineString,4326)", nullable = false)
    private Object path;


    @Column(name = "distance_m", nullable = false)
    private Integer distanceM;


    @Column(name = "start_lat", nullable = false)
    private Double startLat;

    @Column(name = "start_lng", nullable = false)
    private Double startLng;

    @Column(name = "thumbnail_url", length = 500, nullable = false)
    private String thumbnailUrl;


    @Column(name = "image_url", length = 500, nullable = false)
    private String imageUrl;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CourseStatus status;


    @PrePersist
    public void prePersist() {
        if (this.status == null) {
            this.status = CourseStatus.ACTIVE;
        }
    }


    public void block() {
        this.status = CourseStatus.BLOCKED;
    }

    public void delete() {
        this.status = CourseStatus.DELETED;
    }
}