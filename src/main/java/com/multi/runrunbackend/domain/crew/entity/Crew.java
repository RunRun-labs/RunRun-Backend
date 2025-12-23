package com.multi.runrunbackend.domain.crew.entity;

import com.multi.runrunbackend.common.entitiy.BaseEntity;
import com.multi.runrunbackend.common.exception.custom.BusinessException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.crew.constant.CrewRecruitStatus;
import com.multi.runrunbackend.domain.crew.constant.CrewStatus;
import com.multi.runrunbackend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
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

    @ManyToOne(fetch = FetchType.LAZY)
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

    @Column(name = "pace", length = 50)
    private String averagePace;

    @Column(name = "activity_time", nullable = false, length = 100)
    private String activityTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "crew_status", length = 20)
    private CrewStatus crewStatus;  // ACTIVE, DISBANDED

    @Enumerated(EnumType.STRING)
    @Column(name = "crew_recruit_status", length = 20)
    private CrewRecruitStatus crewRecruitStatus;  // RECRUITING, CLOSED

    /**
     * @description : toEntity - 엔티티 생성 정적 팩토리 메서드
     * @filename : Crew
     * @author : BoKyung
     * @since : 25. 12. 17. 수요일
     */
    public static Crew create(String crewName, String crewDescription, String crewImageUrl,
                              String region, String distance, String averagePace, String activityTime, User user) {
        return Crew.builder()
                .crewName(crewName)
                .crewDescription(crewDescription)
                .crewImageUrl(crewImageUrl)
                .region(region)
                .distance(distance)
                .averagePace(averagePace)
                .activityTime(activityTime)
                .user(user)
                .crewStatus(CrewStatus.ACTIVE)
                .crewRecruitStatus(CrewRecruitStatus.RECRUITING)
                .build();
    }

    /**
     * @description : updateCrew - 크루 정보 수정
     * @filename : Crew
     * @author : BoKyung
     * @since : 25. 12. 17. 수요일
     */
    public void updateCrew(String crewDescription, String crewImageUrl,
                           String region, String distance, String averagePace, String activityTime) {
        this.crewDescription = crewDescription;
        if (crewImageUrl != null) {
            this.crewImageUrl = crewImageUrl;
        }
        this.region = region;
        this.distance = distance;
        this.averagePace = averagePace;
        this.activityTime = activityTime;
    }

    /**
     * @description : updateStatus - 크루 상태 변경
     * @filename : Crew
     * @author : BoKyung
     * @since : 25. 12. 17. 수요일
     */
    public void updateStatus(CrewStatus crewStatus) {
        this.crewStatus = crewStatus;
    }

    /**
     * @description : updateRecruitStatus - 모집 상태 변경
     * @filename : Crew
     * @author : BoKyung
     * @since : 25. 12. 17. 수요일
     */
    public void updateRecruitStatus(CrewRecruitStatus crewRecruitStatus) {
        if (this.crewStatus == CrewStatus.DISBANDED) {
            throw new BusinessException(ErrorCode.CREW_ALREADY_DISBANDED);
        }
        this.crewRecruitStatus = crewRecruitStatus;
    }

    /**
     * @description : softDelete - 크루 해체 (Soft Delete)
     * @filename : Crew
     * @author : BoKyung
     * @since : 25. 12. 17. 수요일
     */
    public void softDelete() {
        if (this.crewStatus == CrewStatus.DISBANDED) {
            throw new BusinessException(ErrorCode.CREW_ALREADY_DISBANDED);
        }
        this.crewStatus = CrewStatus.DISBANDED;
        this.delete();
    }

    /**
     * @throws BusinessException 이미 해체된 크루인 경우
     * @description : validateNotDisbanded - 해체되지 않은 크루인지 검증
     * @filename : Crew
     * @author : BoKyung
     * @since : 25. 12. 17. 수요일
     */
    public void validateNotDisbanded() {
        if (this.crewStatus == CrewStatus.DISBANDED) {
            throw new BusinessException(ErrorCode.CREW_ALREADY_DISBANDED);
        }
    }

    /**
     * @description : isRecruiting - 모집중인지 확인
     * @filename : Crew
     * @author : BoKyung
     * @since : 25. 12. 17. 수요일
     */
    public boolean isRecruiting() {
        return this.crewRecruitStatus == CrewRecruitStatus.RECRUITING
                && this.crewStatus == CrewStatus.ACTIVE;
    }

}
