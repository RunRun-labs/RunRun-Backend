package com.multi.runrunbackend.domain.rating.entity;

import com.multi.runrunbackend.common.constant.DistanceType;
import com.multi.runrunbackend.common.entitiy.BaseEntity;
import com.multi.runrunbackend.domain.match.constant.Tier;
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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

/**
 * @author : KIMGWANGHO
 * @description : 거리별 유저 레이팅 관리 엔티티
 * @filename : DistanceRating
 * @since : 2025-12-17 수요일
 */
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "distance_rating", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "distance_type"})
})
@SQLRestriction("is_deleted = false")
public class DistanceRating extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(name = "distance_type", length = 10, nullable = false)
  private DistanceType distanceType;

  @Column(name = "current_rating", nullable = false)
  @Builder.Default
  private Integer currentRating = 1000;

  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(name = "current_tier", length = 20, nullable = false)
  private Tier currentTier = Tier.거북이;


  @Column(name = "win_count", nullable = false)
  @Builder.Default
  private Integer winCount = 0;

  public void updateRating(int changePoint, int rank) {
    this.currentRating += changePoint;
    if (this.currentRating < 0) {
      this.currentRating = 0;
    }

    if (rank == 1) {
      this.winCount++;
    }

    updateTier();

  }

  private void updateTier() {
    if (this.currentRating >= 2000) {
      this.currentTier = Tier.장산범;
    } else if (this.currentRating >= 1800) {
      this.currentTier = Tier.호랑이;
    } else if (this.currentRating >= 1600) {
      this.currentTier = Tier.표범;
    } else if (this.currentRating >= 1400) {
      this.currentTier = Tier.사슴;
    } else if (this.currentRating >= 1200) {
      this.currentTier = Tier.토끼;
    } else {
      this.currentTier = Tier.거북이;
    }
  }
}