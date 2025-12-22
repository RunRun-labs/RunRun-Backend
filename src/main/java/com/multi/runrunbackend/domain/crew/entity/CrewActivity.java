package com.multi.runrunbackend.domain.crew.entity;

import com.multi.runrunbackend.common.entitiy.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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

/**
 * @author : BoKyung
 * @description : 크루 활동 이력 엔티티
 * @filename : CrewActivity
 * @since : 25. 12. 17. 수요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CrewActivity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crew_id", nullable = false)
    private Crew crew;

    @Column(name = "region", nullable = false, length = 100)
    private String region;

    @Column(name = "distance", nullable = false)
    private Integer distance;

    @Column(name = "participation_cnt", nullable = false)
    private Integer participationCnt;


    /**
     * @description : toEntity - 엔티티 생성 정적 팩토리 메서드
     * @filename : CrewActivity
     * @author : BoKyung
     * @since : 25. 12. 17. 수요일
     */
    public static CrewActivity toEntity(Crew crew, String region, Integer distance,
                                        Integer participationCnt) {
        return CrewActivity.builder()
                .crew(crew)
                .region(region)
                .distance(distance)
                .participationCnt(participationCnt)
                .build();
    }

    /**
     * @description : updateParticipationCnt - 참여 인원 업데이트
     * @filename : CrewActivity
     * @author : BoKyung
     * @since : 25. 12. 17. 수요일
     */
    public void updateParticipationCnt(Integer participationCnt) {
        this.participationCnt = participationCnt;

    }
}
