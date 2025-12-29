package com.multi.runrunbackend.domain.course.entity;

import com.multi.runrunbackend.common.entitiy.BaseTimeEntity;
import com.multi.runrunbackend.domain.course.constant.CourseRegisterType;
import com.multi.runrunbackend.domain.course.constant.CourseStatus;
import com.multi.runrunbackend.domain.course.dto.req.CourseCreateReqDto;
import com.multi.runrunbackend.domain.course.dto.req.CourseUpdateReqDto;
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
import org.hibernate.annotations.SQLRestriction;
import org.locationtech.jts.geom.LineString;

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
@SQLRestriction("status = 'ACTIVE'")
public class Course extends BaseTimeEntity {

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
    private LineString path;


    @Column(name = "distance_m", nullable = false)
    private Integer distanceM;


    @Column(name = "start_lat", nullable = false)
    private Double startLat;

    @Column(name = "start_lng", nullable = false)
    private Double startLng;

    @Column(name = "thumbnail_url", columnDefinition = "TEXT", nullable = false)
    private String thumbnailUrl;


    @Column(name = "image_url", columnDefinition = "TEXT", nullable = false)
    private String imageUrl;


    @Enumerated(EnumType.STRING)
    @Column(name = "register_type", nullable = false)
    private CourseRegisterType registerType;

    @Column(nullable = false)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CourseStatus status;

    @Column(name = "like_count", nullable = false)
    private long likeCount;

    @Column(name = "favorite_count", nullable = false)
    private long favoriteCount;

    @PrePersist
    public void prePersist() {
        if (this.status == null) {
            this.status = CourseStatus.ACTIVE;
        }
    }

    public static Course create(
        User user,
        CourseCreateReqDto req,
        LineString path,
        String imageUrl,
        String thumbnailUrl,
        CourseRegisterType type
    ) {
        Course c = new Course();
        c.user = user;
        c.title = req.getTitle();
        c.description = req.getDescription();
        c.path = path;
        c.distanceM = req.getDistanceM();
        c.startLat = req.getStartLat();
        c.startLng = req.getStartLng();
        c.thumbnailUrl = thumbnailUrl;
        c.imageUrl = imageUrl;
        c.registerType = type;
        c.status = CourseStatus.ACTIVE;
        c.address = req.getAddress();
        return c;
    }

    public void update(
        User user,
        CourseUpdateReqDto req,
        LineString path,
        String imageUrl,
        String thumbnailUrl,
        CourseRegisterType type
    ) {
        this.user = user;
        this.title = req.getTitle();
        this.description = req.getDescription();
        this.path = path;
        this.distanceM = req.getDistanceM();
        this.startLat = req.getStartLat();
        this.startLng = req.getStartLng();
        this.thumbnailUrl = thumbnailUrl;
        this.imageUrl = imageUrl;
        this.registerType = type;
        this.address = req.getAddress();
    }

    public void resolveUrl(String imageUrl, String thumbnailUrl) {
        this.imageUrl = imageUrl;
        this.thumbnailUrl = thumbnailUrl;
    }

    public void block() {
        this.status = CourseStatus.BLOCKED;
    }

    public void delete() {
        this.status = CourseStatus.DELETED;
    }
}