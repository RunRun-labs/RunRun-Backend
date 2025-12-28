package com.multi.runrunbackend.domain.match.entity;

import com.multi.runrunbackend.common.entitiy.BaseEntity;
import com.multi.runrunbackend.domain.course.entity.Course;
import com.multi.runrunbackend.domain.match.constant.SessionStatus;
import com.multi.runrunbackend.domain.match.constant.SessionType;
import com.multi.runrunbackend.domain.recruit.entity.Recruit;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

/**
 * @author : KIMGWANGHO
 * @description : 매치 세션(온라인, 오프라인, 솔로런, 고스트런) 관리 엔티티
 * @filename : MatchSession
 * @since : 2025-12-17 수요일
 */
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "match_session")
@SQLRestriction("is_deleted = false")
public class MatchSession extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruit_id")
    private Recruit recruit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "running_result_id")
    private RunningResult runningResult;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private SessionType type;

    @Column(name = "target_distance")
    private Double targetDistance;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private SessionStatus status;

    @Column(nullable = false)
    private Integer duration;

    /**
     * 세션 상태 업데이트
     * @param status 새로운 상태
     */
    public void updateStatus(SessionStatus status) {
        this.status = status;
    }

}