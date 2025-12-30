package com.multi.runrunbackend.domain.membership.repository;

import com.multi.runrunbackend.domain.membership.entity.Membership;
import com.multi.runrunbackend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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

}
