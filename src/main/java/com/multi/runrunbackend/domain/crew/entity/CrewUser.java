package com.multi.runrunbackend.domain.crew.entity;

import com.multi.runrunbackend.common.entitiy.BaseEntity;
import com.multi.runrunbackend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

/**
 * @author : BoKyung
 * @description : 크루원 정보 엔티티
 * @filename : CrewMember
 * @since : 25. 12. 17. 수요일
 */
@Entity
@Table(name = "crew_user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CrewUser extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "crew_id", nullable = false)
    private Crew crew;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "role", nullable = false, length = 20)
    private String role;  // LEADER, SUB_LEADER, MANAGER, MEMBER

    @Column(name = "participation_count", nullable = false)
    private Integer participationCount;


    /**
     * @description : toEntity - 엔티티 생성 정적 팩토리 메서드
     * @filename : CrewMember
     * @author : BoKyung
     * @since : 25. 12. 17. 수요일
     */
    public static CrewUser toEntity(Crew crew, User user, String role) {
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
    public void updateRole(String role) {
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
}
