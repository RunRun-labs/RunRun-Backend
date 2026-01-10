package com.multi.runrunbackend.domain.crew.entity;

import com.multi.runrunbackend.common.entitiy.BaseEntity;
import com.multi.runrunbackend.common.exception.custom.BusinessException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.crew.constant.CrewRole;
import com.multi.runrunbackend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;


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
    @Builder.Default
    private Integer participationCount = 0;  // 참여 횟수 (기본값 0)

    /**
     * @description : create - 크루원 엔티티 생성 정적 팩토리 메서드
     * @filename : CrewUser
     * @author : BoKyung
     * @since : 25. 12. 17. 수요일
     */
    public static CrewUser create(Crew crew, User user, CrewRole role) {
        CrewUser crewUser = new CrewUser();
        crewUser.crew = crew;
        crewUser.user = user;
        crewUser.role = role;
        crewUser.participationCount = 0;  // 기본값 0
        return crewUser;
    }

    /**
     * @throws BusinessException 크루장이 아닌 경우
     * @description : validateLeader - 크루장 권한 검증
     * @filename : CrewUser
     * @author : BoKyung
     * @since : 25. 12. 17. 수요일
     */
    public void validateLeader() {
        if (this.role != CrewRole.LEADER) {
            throw new BusinessException(ErrorCode.NOT_CREW_LEADER);
        }
    }

    /**
     * 권한 변경
     */
    public void changeRole(CrewRole newRole) {
        this.role = newRole;
    }

}
