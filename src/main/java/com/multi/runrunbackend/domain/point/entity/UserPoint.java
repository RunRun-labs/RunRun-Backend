package com.multi.runrunbackend.domain.point.entity;

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

/**
 * @author : BoKyung
 * @description : 사용자 총 포인트 엔티티
 * @filename : UserPoint
 * @since : 25. 12. 17. 수요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "total_point", nullable = false)
    private Integer totalPoint;

    /**
     * @description : toEntity - 엔티티 생성 정적 팩토리 메서드
     * @filename : MemberPoint
     * @author : BoKyung
     * @since : 25. 12. 17. 수요일
     */
    public static UserPoint toEntity(User user) {
        return UserPoint.builder()
            .user(user)
            .totalPoint(0)
            .build();
    }

    /**
     * @description : addPoint - 포인트 추가
     * @filename : MemberPoint
     * @author : BoKyung
     * @since : 25. 12. 17. 수요일
     */
    public void addPoint(Integer amount) {
        this.totalPoint += amount;
    }

    /**
     * @description : subtractPoint - 포인트 차감
     * @filename : MemberPoint
     * @author : BoKyung
     * @since : 25. 12. 17. 수요일
     */
    public void subtractPoint(Integer amount) {
        this.totalPoint -= amount;
    }
}
