package com.multi.runrunbackend.domain.match.entity;

import com.multi.runrunbackend.common.constant.DistanceType;
import com.multi.runrunbackend.common.entitiy.BaseEntity;
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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author : chang
 * @description : 온라인 배틀 결과 엔티티
 * @filename : BattleResult
 * @since : 2025-12-17 수요일
 */
@Entity
@Table(name = "battle_result")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BattleResult extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private MatchSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "ranking", nullable = false)
    private Integer ranking;

    @Enumerated(EnumType.STRING)
    @Column(name = "distance_type", nullable = false, length = 10)
    private DistanceType distanceType;

    @Column(name = "previous_rating", nullable = false)
    private Integer previousRating;

    @Column(name = "current_rating", nullable = false)
    private Integer currentRating;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "running_result_id", nullable = false)
    private RunningResult runningResult;


}
