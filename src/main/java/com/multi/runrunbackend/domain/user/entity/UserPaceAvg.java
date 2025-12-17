package com.multi.runrunbackend.domain.user.entity;

import com.multi.runrunbackend.common.entitiy.BaseEntity;
import com.multi.runrunbackend.domain.match.entity.RunningResult;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * @description : km당 평균 페이스 정보
 * @filename : PaceAvg
 * @since : 2025-12-17 수요일
 */
@Entity
@Table(name = "pace_avg")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPaceAvg extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "running_result_id", nullable = false)
    private RunningResult runningResult;

    @Column(name = "pace_3km", nullable = false)
    private Long pace3km;

    @Column(name = "pace_5km", nullable = false)
    private Long pace5km;

    @Column(name = "pace_10km", nullable = false)
    private Long pace10km;


}