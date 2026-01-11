package com.multi.runrunbackend.domain.advertisement.entity;

import com.multi.runrunbackend.common.entitiy.BaseTimeEntity;
import com.multi.runrunbackend.domain.advertisement.dto.req.adplacement.AdPlacementAdminCreateReqDto;
import com.multi.runrunbackend.domain.advertisement.dto.req.adplacement.AdPlacementAdminUpdateReqDto;
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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "ad_placement",
    indexes = {
        @Index(name = "idx_ad_placement_slot_active", columnList = "slot_id,is_active"),
        @Index(name = "idx_ad_placement_slot_period", columnList = "slot_id,start_at,end_at")
    }
)
public class AdPlacement extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id", nullable = false)
    private AdSlot slot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ad_id", nullable = false)
    private Ad ad;

    @Column(nullable = false)
    private Integer weight;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Column(nullable = false)
    private Integer totalClicks;

    @Column(nullable = false)
    private Integer totalImpressions;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @PrePersist
    public void prePersist() {
        if (this.weight == null) {
            this.weight = 1;
        }
        if (this.isActive == null) {
            this.isActive = true;
        }
        if (this.totalClicks == null) {
            this.totalClicks = 0;
        }
        if (this.totalImpressions == null) {
            this.totalImpressions = 0;
        }
    }

    public static AdPlacement create(AdSlot slot, Ad ad, AdPlacementAdminCreateReqDto dto) {
        AdPlacement p = new AdPlacement();
        p.slot = slot;
        p.ad = ad;
        p.weight = dto.getWeight();
        p.startAt = dto.getStartAt();
        p.endAt = dto.getEndAt();
        return p;
    }

    public void update(AdPlacementAdminUpdateReqDto dto, Ad ad, AdSlot adSlot) {
        this.ad = ad;
        this.slot = adSlot;
        this.weight = dto.getWeight();
        this.startAt = dto.getStartAt();
        this.endAt = dto.getEndAt();
    }

    public void changeAd(Ad ad) {
        this.ad = ad;
    }

    public void changeSlot(AdSlot slot) {
        this.slot = slot;
    }

    public void disable() {
        this.isActive = false;
    }

    public void enable() {
        this.isActive = true;
    }


    public boolean isWithinPeriod(LocalDateTime now) {
        if (now == null) {
            return false;
        }
        if (startAt != null && now.isBefore(startAt)) {
            return false;
        }
        if (endAt != null && now.isAfter(endAt)) {
            return false;
        }
        return true;
    }
}