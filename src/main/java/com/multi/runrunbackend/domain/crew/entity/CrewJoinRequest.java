package com.multi.runrunbackend.domain.crew.entity;

import com.multi.runrunbackend.common.entitiy.BaseEntity;
import com.multi.runrunbackend.common.exception.custom.BusinessException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.crew.constant.CrewDistanceType;
import com.multi.runrunbackend.domain.crew.constant.CrewPaceType;
import com.multi.runrunbackend.domain.crew.constant.JoinStatus;
import com.multi.runrunbackend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

/**
 * @author : BoKyung
 * @description : 크루 가입 신청 엔티티
 * @filename : CrewJoinRequest
 * @since : 25. 12. 17. 수요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@SQLRestriction("is_deleted = false")
public class CrewJoinRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crew_id", nullable = false)
    private Crew crew;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "introduction", columnDefinition = "TEXT")
    private String introduction;

    @Enumerated(EnumType.STRING)
    @Column(name = "distance", nullable = false)
    private CrewDistanceType distance;

    @Enumerated(EnumType.STRING)
    @Column(name = "pace", nullable = false)
    private CrewPaceType pace;

    @Column(name = "region", nullable = false, length = 100)
    private String region;

    @Enumerated(EnumType.STRING)
    @Column(name = "join_status", nullable = false, length = 20)
    private JoinStatus joinStatus;  // PENDING, APPROVED, REJECTED, CANCELED

    /**
     * @description : toEntity - 엔티티 생성 정적 팩토리 메서드
     * @filename : CrewJoinRequest
     * @author : BoKyung
     * @since : 25. 12. 17. 수요일
     */
    public static CrewJoinRequest toEntity(Crew crew, User user, String introduction,
                                           CrewDistanceType distance, CrewPaceType pace, String region) {
        return CrewJoinRequest.builder()
                .crew(crew)
                .user(user)
                .introduction(introduction)
                .distance(distance)
                .pace(pace)
                .region(region)
                .joinStatus(JoinStatus.PENDING)
                .build();
    }

    /**
     * @description : approve - 가입 신청 승인
     * @filename : CrewJoinRequest
     * @author : BoKyung
     * @since : 25. 12. 17. 수요일
     */
    public void approve() {
        validatePending();
        this.joinStatus = JoinStatus.APPROVED;
    }

    /**
     * @description : reject - 가입 신청 거절 (거절 후 재신청 가능하도록 soft delete 처리)
     * @filename : CrewJoinRequest
     * @author : BoKyung
     * @since : 25. 12. 17. 수요일
     */
    public void reject() {
        validatePending();
        this.joinStatus = JoinStatus.REJECTED;
        this.delete(); // 거절 후 재신청 가능하도록 soft delete 처리
    }

    /**
     * @description : cancel - 가입 신청 취소
     * @filename : CrewJoinRequest
     * @author : BoKyung
     * @since : 25. 12. 17. 수요일
     */
    public void cancel() {
        validatePending();
        this.joinStatus = JoinStatus.CANCELED;
        this.delete();
    }

    /**
     * @throws BusinessException PENDING 상태가 아닌 경우
     * @description : validatePending - PENDING 상태인지 검증
     * @filename : CrewJoinRequest
     * @author : BoKyung
     * @since : 25. 12. 17. 수요일
     */
    private void validatePending() {
        if (this.joinStatus != JoinStatus.PENDING) {
            throw new BusinessException(ErrorCode.JOIN_REQUEST_NOT_PENDING);
        }
    }
}
