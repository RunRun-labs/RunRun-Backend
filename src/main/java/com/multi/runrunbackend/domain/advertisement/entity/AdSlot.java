package com.multi.runrunbackend.domain.advertisement.entity;

import com.multi.runrunbackend.common.entitiy.BaseTimeEntity;
import com.multi.runrunbackend.domain.advertisement.constant.AdSlotType;
import com.multi.runrunbackend.domain.advertisement.constant.AdStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : CourseSiren
 * @since : 2025. 12. 17. Wednesday
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "ad_slot")
public class AdSlot extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "slot_type", nullable = false, length = 50)
    private AdSlotType slotType;

    // null = 무제한
    private Integer dailyLimit;

    @Column(nullable = false)
    private Boolean isPremium;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AdStatus status;

    @PrePersist
    public void prePersist() {
        if (this.status == null) {
            this.status = AdStatus.ACTIVE;
        }

        if (this.isPremium == null) {
            this.isPremium = false;
        }
    }
}