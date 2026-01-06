package com.multi.runrunbackend.domain.challenge.entity;

import com.multi.runrunbackend.common.entitiy.BaseEntity;
import com.multi.runrunbackend.domain.challenge.constant.ChallengeType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;

/**
 * @author : kimyongwon
 * @description :  러닝 챌린지 정보를 관리하는 엔티티 - 기간 기반 챌린지 - 목표값(거리, 횟수 등) 관리 - 논리 삭제를 통해 이력 보존
 * @filename : Challenge
 * @since : 25. 12. 17. 오전 10:16 수요일
 */
@Entity
@Table(name = "challenge")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("is_deleted = false")
public class Challenge extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChallengeType challengeType;

    @Column(nullable = false)
    private Double targetValue;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(length = 500)
    private String imageUrl;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Builder
    public Challenge(String title, ChallengeType challengeType, Double targetValue,
                     String description, String imageUrl, LocalDate startDate, LocalDate endDate) {
        this.title = title;
        this.challengeType = challengeType;
        this.targetValue = targetValue;
        this.description = description;
        this.imageUrl = imageUrl;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void update(String title, ChallengeType challengeType, Double targetValue,
                       String description, LocalDate startDate, LocalDate endDate) {
        this.title = title;
        this.challengeType = challengeType;
        this.targetValue = targetValue;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void deleteChallenge() {
        this.isDeleted = true;
    }

    public void updateImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @PrePersist
    public void prePersist() {
        if (this.startDate != null && this.endDate != null
                && this.startDate.isAfter(this.endDate)) {
            throw new IllegalStateException("시작일은 종료일보다 이전이어야 합니다.");
        }
    }

}