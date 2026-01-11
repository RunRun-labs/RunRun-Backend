package com.multi.runrunbackend.domain.membership.repository;

import com.multi.runrunbackend.domain.membership.constant.MembershipStatus;
import com.multi.runrunbackend.domain.membership.entity.Membership;
import com.multi.runrunbackend.domain.user.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author : BoKyung
 * @description : 멤버십 DB 접근을 담당하는 Repository
 * @filename : MembershipRepository
 * @since : 25. 12. 30. 월요일
 */
@Repository
public interface MembershipRepository extends JpaRepository<Membership, Long> {

    // 멤버십 찾기
    Optional<Membership> findByUser(User user);

    // 멤버십 존재 여부 확인
    boolean existsByUser_Id(Long userId);

    // ACTIVE 또는 CANCELED 확인
    boolean existsByUser_IdAndMembershipStatusIn(Long userId, List<MembershipStatus> statuses);

    // 해지 신청 상태 + 만료일이 지난 멤버십 찾기 (자동 처리용)
    List<Membership> findByMembershipStatusAndEndDateBefore(
        MembershipStatus status,
        LocalDateTime dateTime
    );

    /**
     * @description : 자동결제 대상 조회 (스케줄러용)
     */
    List<Membership> findByMembershipStatusAndNextBillingDateBetween(
        MembershipStatus status,
        LocalDateTime start,
        LocalDateTime end
    );

    /**
     * @description : 해지 신청 상태 + 만료일이 하루 후인 멤버십 찾기 (만료 전 알림용)
     */
    List<Membership> findByMembershipStatusAndEndDateBetween(
        MembershipStatus status,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime
    );

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
