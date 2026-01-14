package com.multi.runrunbackend.domain.crew.entity;

import com.multi.runrunbackend.common.entitiy.BaseCreatedEntity;
import com.multi.runrunbackend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

/**
 * @author : BoKyung
 * @description : 크루 활동 참여자 엔티티
 * @filename : CrewActivityMember
 * @since : 25. 12. 17. 수요일
 */
@Entity
@Table(name = "crew_activity_user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CrewActivityUser extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crew_activity_id", nullable = false)
    private CrewActivity crewActivity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * @description : create - 엔티티 생성 정적 팩토리 메서드
     * @filename : CrewActivityMember
     * @author : BoKyung
     * @since : 25. 12. 17. 수요일
     */
    public static CrewActivityUser create(CrewActivity crewActivity, User user) {
        CrewActivityUser activityUser = new CrewActivityUser();
        activityUser.crewActivity = crewActivity;
        activityUser.user = user;
        return activityUser;
    }

}






















