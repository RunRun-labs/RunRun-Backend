package com.multi.runrunbackend.domain.advertisement.entity;

import com.multi.runrunbackend.common.entitiy.BaseTimeEntity;
import com.multi.runrunbackend.domain.advertisement.constant.AdSlotStatus;
import com.multi.runrunbackend.domain.advertisement.constant.AdSlotType;
import com.multi.runrunbackend.domain.advertisement.dto.req.adslot.AdSlotAdminCreateReqDto;
import com.multi.runrunbackend.domain.advertisement.dto.req.adslot.AdSlotAdminUpdateReqDto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "ad_slot",
    indexes = {
        @Index(name = "idx_ad_slot_type", columnList = "slot_type"),
        @Index(name = "idx_ad_slot_status", columnList = "status")
    }
)
public class AdSlot extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "slot_type", nullable = false, length = 50)
    private AdSlotType slotType;

    //0이면 무제한
    private Integer dailyLimit;

    @Column(nullable = false)
    private Boolean allowPremium;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdSlotStatus status;

    @PrePersist
    public void prePersist() {
        if (this.status == null) {
            this.status = AdSlotStatus.ENABLED;
        }
        if (this.allowPremium == null) {
            this.allowPremium = false;
        }
    }

    public static AdSlot create(AdSlotAdminCreateReqDto dto) {
        AdSlot s = new AdSlot();
        s.name = dto.getName();
        s.slotType = dto.getSlotType();
        s.dailyLimit = dto.getDailyLimit();
        s.allowPremium = dto.getAllowPremium();
        return s;
    }

    public void update(AdSlotAdminUpdateReqDto dto) {
        this.name = dto.getName();
        this.slotType = dto.getSlotType();
        this.dailyLimit = dto.getDailyLimit();
        this.allowPremium = dto.getAllowPremium();
    }

    public boolean isEnabled() {
        return this.status == AdSlotStatus.ENABLED;
    }

    public void disable() {
        this.status = AdSlotStatus.DISABLED;
    }

    public void enable() {
        this.status = AdSlotStatus.ENABLED;
    }

}