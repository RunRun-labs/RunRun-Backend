package com.multi.runrunbackend.domain.match.repository;

import com.multi.runrunbackend.domain.match.entity.BattleResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author : chang
 * @description : 배틀 결과 Repository
 * @filename : BattleResultRepository
 * @since : 2025-12-30
 */
@Repository
public interface BattleResultRepository extends JpaRepository<BattleResult, Long> {
}
