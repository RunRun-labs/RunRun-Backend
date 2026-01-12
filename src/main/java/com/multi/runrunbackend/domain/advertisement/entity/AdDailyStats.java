package com.multi.runrunbackend.domain.advertisement.entity;

import com.multi.runrunbackend.common.entitiy.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "ad_daily_stats",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_ad_daily_stats_date_placement",
            columnNames = {"stat_date", "placement_id"}
        )
    },
    indexes = {
        @Index(name = "idx_ad_daily_stats_date", columnList = "stat_date"),
        @Index(name = "idx_ad_daily_stats_placement", columnList = "placement_id")
    }
)

public class AdDailyStats extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 통계 날짜(일 단위)
    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "placement_id", nullable = false)
    private AdPlacement placement;

    // 일 노출 수
    @Column(nullable = false)
    private Integer impressions;

    // 일 클릭 수
    @Column(nullable = false)
    private Integer clicks;

    @PrePersist
    public void prePersist() {
        if (this.impressions == null) {
            this.impressions = 0;
        }
        if (this.clicks == null) {
            this.clicks = 0;
        }
    }
}