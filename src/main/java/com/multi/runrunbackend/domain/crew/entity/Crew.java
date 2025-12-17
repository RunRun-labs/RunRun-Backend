package com.multi.runrunbackend.domain.crew.entity;

import com.multi.runrunbackend.common.entitiy.BaseEntity;
import com.multi.runrunbackend.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

/**
 * @author : BoKyung
 * @description : 크루 기본 정보 엔티티
 * @filename : Crew
 * @since : 25. 12. 17. 수요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@SQLRestriction("is_deleted = false")
public class Crew extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "crew_name", nullable = false, length = 100)
    private String crewName;

    @Column(name = "crew_description", nullable = false, columnDefinition = "TEXT")
    private String crewDescription;

    @Column(name = "crew_image_url", nullable = false, length = 500)
    private String crewImageUrl;

    @Column(name = "region", nullable = false, length = 100)
    private String region;

    @Column(name = "distance", nullable = false, length = 50)
    private String distance;

    @Column(name = "activity_time", nullable = false, length = 100)
    private String activityTime;

    @Column(name = "crew_status", length = 20)
    private String crewStatus;  // ACTIVE, DISBANDED

    @Column(name = "recruit_status", length = 20)
    private String recruitStatus;  // RECRUITING, CLOSED

    /**
     * @description : toEntity - 엔티티 생성 정적 팩토리 메서드
     * @filename : Crew
     * @author : BoKyung
     * @since : 25. 12. 17. 수요일
     */
    public static Crew toEntity(String crewName, String crewDescription, String crewImageUrl,
        String region, String distance, String activityTime, User user) {
        return Crew.builder()
            .crewName(crewName)
            .crewDescription(crewDescription)
            .crewImageUrl(crewImageUrl)
            .region(region)
            .distance(distance)
            .activityTime(activityTime)
            .user(user)
            .crewStatus("ACTIVE")
            .recruitStatus("RECRUITING")
            .build();
    }

    /**
     * @description : updateCrew - 크루 정보 수정
     * @filename : Crew
     * @author : BoKyung
     * @since : 25. 12. 17. 수요일
     */
    public void updateCrew(String crewName, String crewDescription, String crewImageUrl,
        String region, String distance, String activityTime) {
        this.crewName = crewName;
        this.crewDescription = crewDescription;
        this.crewImageUrl = crewImageUrl;
        this.region = region;
        this.distance = distance;
        this.activityTime = activityTime;
    }

    /**
     * @description : updateStatus - 크루 상태 변경
     * @filename : Crew
     * @author : BoKyung
     * @since : 25. 12. 17. 수요일
     */
    public void updateStatus(String crewStatus) {
        this.crewStatus = crewStatus;
    }

    /**
     * @description : updateRecruitStatus - 모집 상태 변경
     * @filename : Crew
     * @author : BoKyung
     * @since : 25. 12. 17. 수요일
     */
    public void updateRecruitStatus(String recruitStatus) {
        this.recruitStatus = recruitStatus;
    }

    /**
     * @description : softDelete - 크루 해체 (Soft Delete)
     * @filename : Crew
     * @author : BoKyung
     * @since : 25. 12. 17. 수요일
     */
    public void softDelete() {
        this.crewStatus = "DISBANDED";
    }
}
