package com.multi.runrunbackend.domain.advertisement.entity;

import com.multi.runrunbackend.common.entitiy.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author : kyungsoo
 * @description : 광고 캠페인의 실제 크리에이티브(이미지, 링크 등)를 관리하는 엔티티. 클릭 수와 노출 수를 추적한다.
 * @filename : AdCreative
 * @since : 2025. 12. 17. Wednesday
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "ad_creative")
public class AdCreative extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private AdCampaign campaign;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id", nullable = false)
    private AdSlot slot;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(nullable = false)
    private Long clickCount;

    @Column(nullable = false)
    private Long exposureCount;

    @Column(name = "redirect_url", length = 500)
    private String redirectUrl;

    @PrePersist
    public void prePersist() {
        if (this.clickCount == null) {
            this.clickCount = 0L;
        }
        if (this.exposureCount == null) {
            this.exposureCount = 0L;
        }
    }
}