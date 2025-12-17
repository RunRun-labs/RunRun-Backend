package com.multi.runrunbackend.domain.challenge.entity;

import com.multi.runrunbackend.common.entitiy.BaseEntity;
import com.multi.runrunbackend.domain.challenge.constant.ChallengeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author : kimyongwon
 * @description :  러닝 챌린지 정보를 관리하는 엔티티 - 기간 기반 챌린지 - 목표값(거리, 횟수 등) 관리 - 논리 삭제를 통해 이력 보존
 * @filename : UserSetting
 * @since : 25. 12. 17. 오전 10:16 수요일
 */
@Entity
@Table(name = "challenge")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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
    @Column(nullable = false)
    private String description;

    @Column(length = 500)
    private String imageUrl;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;


}