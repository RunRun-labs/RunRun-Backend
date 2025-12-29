package com.multi.runrunbackend.domain.crew.repository;

import com.multi.runrunbackend.domain.crew.entity.CrewRole;
import com.multi.runrunbackend.domain.crew.entity.CrewUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author : BoKyung
 * @description : 크루원 Repository
 * @filename : CrewUserRepository
 * @since : 25. 12. 18. 목요일
 */
@Repository
public interface CrewUserRepository extends JpaRepository<CrewUser, Long> {

    /**
     * @param crewId 크루 ID
     * @param userId 사용자 ID
     * @param role   역할 (LEADER)
     * @description : 크루장 권한 확인: 특정 크루(crewId)에 특정 사용자(userId)가 특정 역할(role)로 있는지 확인
     */
    boolean existsByCrewIdAndUserIdAndRole(Long crewId, Long userId, CrewRole role);

    /**
     * @param userId 사용자 ID
     * @param role   LEADER
     * @description : 사용자가 LEADER 역할로 있는지 확인 (1인 1크루 검증용)
     */
    boolean existsByUserIdAndRoleAndIsDeletedFalse(Long userId, CrewRole role);

    /**
     * @param crewId 크루 ID
     * @param userId 사용자 ID
     * @description : 특정 크루에 사용자가 크루원으로 존재하는지 확인 (중복 가입 방지)
     */
    boolean existsByCrewIdAndUserIdAndIsDeletedFalse(Long crewId, Long userId);

//    /**
//     * @param crewId 크루 ID
//     * @description : 특정 크루의 모든 크루원을 역할로 조회 (is_deleted = false만)
//     */
//    @Query("SELECT cu FROM CrewUser cu " +
//            "JOIN FETCH cu.user " +
//            "WHERE cu.crew.id = :crewId " +
//            "AND cu.isDeleted = false " +
//            "ORDER BY " +
//            "CASE cu.role " +
//            "  WHEN com.multi.runrunbackend.domain.crew.entity.CrewRole.LEADER THEN 0 " +
//            "  WHEN com.multi.runrunbackend.domain.crew.entity.CrewRole.SUB_LEADER THEN 1 " +
//            "  WHEN com.multi.runrunbackend.domain.crew.entity.CrewRole.STAFF THEN 2 " +
//            "  ELSE 3 " +
//            "END, cu.createdAt ASC")
//    List<CrewUser> findAllByCrewIdAndIsDeletedFalseOrderByRole(@Param("crewId") Long crewId);

    /**
     * @param crewId 크루 ID
     * @description : 크루원 수 조회
     */
    Long countByCrewIdAndIsDeletedFalse(Long crewId);

    /**
     * @param crewId 크루 ID
     * @description : 특정 크루의 모든 크루원을 soft delete 처리할 대상 조회 (크루 해체용)
     */
    List<CrewUser> findAllByCrewIdAndIsDeletedFalse(@Param("crewId") Long crewId);
}
