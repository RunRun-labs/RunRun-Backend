package com.multi.runrunbackend.domain.crew.entity;

import com.multi.runrunbackend.common.entitiy.BaseEntity;
import com.multi.runrunbackend.common.exception.custom.BusinessException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.crew.constant.CrewRole;
import com.multi.runrunbackend.domain.user.entity.User;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;


/**
 * @author : BoKyung
 * @description : 크루원 정보 엔티티
 * @filename : CrewUser
 * @since : 25. 12. 17. 수요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CrewUser extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crew_id", nullable = false)
    private Crew crew;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private CrewRole role;  // LEADER, SUB_LEADER, STAFF, MEMBER

    @Column(name = "participation_count", nullable = false)
    private Integer participationCount;


    /**
     * @description : toEntity - 엔티티 생성 정적 팩토리 메서드
     * @filename : CrewMember
     * @author : BoKyung
     * @since : 25. 12. 17. 수요일
     */
    public static CrewUser toEntity(Crew crew, User user, CrewRole role) {
        return CrewUser.builder()
                .crew(crew)
                .user(user)
                .role(role)
                .participationCount(0)
                .build();
    }

    /**
     * @description : updateRole - 크루원 권한 변경
     * @filename : CrewMember
     * @author : BoKyung
     * @since : 25. 12. 17. 수요일
     */
    public void updateRole(CrewRole role) {

        this.role = role;
    }

    /**
     * @description : incrementParticipationCount - 참여 횟수 증가
     * @filename : CrewMember
     * @author : BoKyung
     * @since : 25. 12. 17. 수요일
     */
    public void incrementParticipationCount() {
        this.participationCount++;
    }

    /**
     * @throws BusinessException 크루장이 아닌 경우
     * @description : validateLeader - 크루장 권한 검증
     * @filename : CrewMember
     * @author : BoKyung
     * @since : 25. 12. 17. 수요일
     */
    public void validateLeader() {
        if (this.role != CrewRole.LEADER) {
            throw new BusinessException(ErrorCode.NOT_CREW_LEADER);
        }
    }

}
