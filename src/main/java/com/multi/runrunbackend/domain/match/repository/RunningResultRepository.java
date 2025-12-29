package com.multi.runrunbackend.domain.match.repository;

import com.multi.runrunbackend.domain.match.entity.RunningResult;
import com.multi.runrunbackend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 런닝 결과 Repository
 * 
 * @author : chang
 * @since : 2024-12-23
 */
public interface RunningResultRepository extends JpaRepository<RunningResult, Long> {
    
    /**
     * 사용자의 최신 런닝 결과 조회
     * - 런닝 종료 후 결과 모달 표시용
     * 
     * @param user 사용자 엔티티
     * @return 최신 런닝 결과
     */
    Optional<RunningResult> findTopByUserOrderByCreatedAtDesc(User user);
}
