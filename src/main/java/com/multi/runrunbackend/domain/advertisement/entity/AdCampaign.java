package com.multi.runrunbackend.domain.advertisement.entity;

import com.multi.runrunbackend.common.entitiy.BaseTimeEntity;
import com.multi.runrunbackend.domain.advertisement.constant.AdStatus;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author : kyungsoo
 * @description :  광고 캠페인을 나타내는 엔티티 <p> 광고주는 캠페인을 생성하여 특정 기간(startDate ~ endDate) 동안 예산(budget)을
 * 기반으로 광고를 운영한다. 캠페인의 현재 상태는 {@link AdStatus}로 관리된다. </p>  <p> 이 엔티티는 광고 캠페인의 기본 정보와 생명주기를 관리하며, 광고
 * 슬롯 및 크리에이티브와의 연관 관계의 기준이 된다. </p>
 * @filename : AdCampaign
 * @since : 2025. 12. 17. Wednesday
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "ad_campaign")
public class AdCampaign extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private Integer budget;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AdStatus status;

    @PrePersist
    public void prePersist() {
        if (this.status == null) {
            this.status = AdStatus.PENDING;
        }
    }
}
