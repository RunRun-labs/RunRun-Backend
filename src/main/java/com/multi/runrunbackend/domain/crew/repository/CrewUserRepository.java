package com.multi.runrunbackend.domain.crew.repository;

import com.multi.runrunbackend.domain.crew.constant.CrewRole;
import com.multi.runrunbackend.domain.crew.entity.Crew;
import com.multi.runrunbackend.domain.crew.entity.CrewUser;
import com.multi.runrunbackend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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
    boolean existsByCrewIdAndUserIdAndRoleAndIsDeletedFalse(Long crewId, Long userId, CrewRole role);

    /**
     * @param crewId 크루 ID
     * @param userId 사용자 ID
     * @description : 특정 크루에 사용자가 크루원으로 존재하는지 확인 (중복 가입 방지)
     */
    boolean existsByCrewIdAndUserIdAndIsDeletedFalse(Long crewId, Long userId);

    /**
     * @param crew 크루 엔티티
     * @param user 사용자 엔티티
     * @description : 특정 크루의 특정 사용자 크루원 정보를 조회 (권한 확인용, 역할 변경에 사용)
     */
    Optional<CrewUser> findByCrewAndUserAndIsDeletedFalse(Crew crew, User user);

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

    /**
     * @param userId 사용자 ID
     * @param role   LEADER
     * @description : 사용자가 LEADER 역할로 있는지 확인 (1인 1크루 검증용)
     */
    boolean existsByUserIdAndRoleAndIsDeletedFalse(Long userId, CrewRole role);


    /**
     * 사용자가 다른 크루에 가입했는지 확인 (1인 1크루 검증용)
     *
     * @param userId 사용자 ID
     */
    boolean existsByUserIdAndIsDeletedFalse(Long userId);

    /**
     * @param crewId 크루 ID
     * @description : 크루원 목록 + 각 크루원의 활동 참여 횟수 한 번에 조회
     */
    @Query("SELECT cu, COUNT(cau.id), MAX(ca.createdAt) " +
            "FROM CrewUser cu " +
            "LEFT JOIN CrewActivityUser cau ON cau.user.id = cu.user.id " +
            "LEFT JOIN cau.crewActivity ca ON ca.id = cau.crewActivity.id " +
            "WHERE cu.crew.id = :crewId " +
            "AND cu.isDeleted = false " +
            "AND (ca.id IS NULL OR (ca.crew.id = :crewId AND ca.isDeleted = false)) " +
            "GROUP BY cu.id, cu.user.id, cu.user.name, cu.user.profileImageUrl, cu.role, cu.createdAt " +
            "ORDER BY " +
            "  CASE cu.role " +
            "    WHEN com.multi.runrunbackend.domain.crew.constant.CrewRole.LEADER THEN 0 " +
            "    WHEN com.multi.runrunbackend.domain.crew.constant.CrewRole.SUB_LEADER THEN 1 " +
            "    WHEN com.multi.runrunbackend.domain.crew.constant.CrewRole.STAFF THEN 2 " +
            "    ELSE 3 " +
            "  END, cu.createdAt ASC")
    List<Object[]> findAllWithParticipationCountAndLastActivity(@Param("crewId") Long crewId);

    /**
     * @param userId 사용자 ID
     * @description : 사용자가 가입한 크루 정보 조회
     */
    Optional<CrewUser> findByUserIdAndIsDeletedFalse(Long userId);

    /**
     * @param crewId 크루 ID
     * @param userId 사용자 ID
     * @description : 크루 ID와 사용자 ID로 크루원 조회 (채팅방 역할 표시용)
     */
    Optional<CrewUser> findByCrewIdAndUserIdAndIsDeletedFalse(Long crewId, Long userId);


    /**
     * @description : 특정 사용자가 특정 역할로 있는 크루원 정보 조회
     */
    Optional<CrewUser> findByUserAndRoleAndIsDeletedFalse(User user, CrewRole role);

    /**
     * @description : 특정 크루의 특정 역할 크루원 목록 조회
     */
    List<CrewUser> findByCrewAndRoleAndIsDeletedFalse(Crew crew, CrewRole role);

    /**
     * @description : 특정 크루의 모든 크루원 조회
     */
    List<CrewUser> findByCrewAndIsDeletedFalse(Crew crew);

    /**
     * 사용자 ID와 역할로 크루원 조회 (삭제되지 않은 것만)
     */
    Optional<CrewUser> findByUserIdAndRoleAndIsDeletedFalse(Long userId, CrewRole role);

}
