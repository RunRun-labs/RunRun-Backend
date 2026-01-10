package com.multi.runrunbackend.domain.user.repository;

import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.entity.UserBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 *
 * @author : kimyongwon
 * @description : 차단 여부 확인 / 차단 해제 / 차단 목록 조회를 위한 리포지토리
 * @filename : UserBlockRepository
 * @since : 25. 12. 29. 오전 12:09 월요일
 */
public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {

    // 차단 여부 확인
    boolean existsByBlockerAndBlockedUser(User blocker, User blockedUser);

    // 차단 관계 조회 (해제 시 사용)
    Optional<UserBlock> findByBlockerAndBlockedUser(User blocker, User blockedUser);

    // 내가 차단한 목록 조회
    @Query("SELECT ub FROM UserBlock ub JOIN FETCH ub.blockedUser WHERE ub.blocker.id = :blockerId")
    List<UserBlock> findAllByBlockerId(@Param("blockerId") Long blockerId);

    // 나를 차단한 유저 목록 조회
    @Query("SELECT ub FROM UserBlock ub JOIN FETCH ub.blocker WHERE ub.blockedUser.id = :userId")
    List<UserBlock> findAllByBlockedUserId(@Param("userId") Long userId);
}