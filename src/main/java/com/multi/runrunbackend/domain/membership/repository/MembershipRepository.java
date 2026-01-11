package com.multi.runrunbackend.domain.membership.repository;

import com.multi.runrunbackend.domain.membership.constant.MembershipStatus;
import com.multi.runrunbackend.domain.membership.entity.Membership;
import com.multi.runrunbackend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
    
    // 오늘 생성된 멤버십 카운트
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

}
