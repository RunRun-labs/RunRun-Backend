package com.multi.runrunbackend.domain.crew.repository;

import com.multi.runrunbackend.domain.crew.constant.JoinStatus;
import com.multi.runrunbackend.domain.crew.entity.Crew;
import com.multi.runrunbackend.domain.crew.entity.CrewJoinRequest;
import com.multi.runrunbackend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author : BoKyung
 * @description : 크루 가입 신청 Repository
 * @filename : CrewJoinRequestRepository
 * @since : 25. 12. 22. 월요일
 */
@Repository
public interface CrewJoinRequestRepository extends JpaRepository<CrewJoinRequest, Long> {

    /**
     * @param crew       크루 엔티티
     * @param joinStatus 가입 신청 상태 (주로 PENDING)
     * @description : 특정 크루의 특정 상태인 모든 가입 신청 목록을 조회 (JOIN FETCH로 가입신청 + 신청한 사용자 같이 가져오기)
     */
    @Query("SELECT cjr FROM CrewJoinRequest cjr " +
            "JOIN FETCH cjr.user " +
            "WHERE cjr.crew = :crew " +
            "AND cjr.joinStatus = :joinStatus " +
            "AND cjr.isDeleted = false " +
            "ORDER BY cjr.createdAt DESC")
    List<CrewJoinRequest> findAllByCrewAndJoinStatusAndIsDeletedFalse(
            @Param("crew") Crew crew,
            @Param("joinStatus") JoinStatus joinStatus
    );

    /**
     * @param id 가입 신청 ID
     * @description : ID로 삭제되지 않은 가입 신청을 조회
     */
    Optional<CrewJoinRequest> findByIdAndIsDeletedFalse(Long id);

    /**
     * @param crew       크루 엔티티
     * @param user       사용자 엔티티
     * @param joinStatus 가입 신청 상태 (PENDING, APPROVED 등)
     * @description : 특정 크루에 특정 사용자가 특정 상태로 신청한 내역을 조회
     */
    Optional<CrewJoinRequest> findByCrewAndUserAndJoinStatus(Crew crew, User user, JoinStatus joinStatus);

    /**
     * @param crew       크루 엔티티
     * @param user       사용자 엔티티
     * @param joinStatus 가입 신청 상태
     * @description : 특정 크루에 특정 사용자가 특정 상태로 신청한 내역이 있는지 확인
     */
    boolean existsByCrewAndUserAndJoinStatus(Crew crew, User user, JoinStatus joinStatus);

    /**
     * @param crewId 크루 ID
     * @param userId 사용자 ID
     * @description : 크루 ID와 사용자 ID로 가장 최근 가입 신청 기록 조회 (삭제되지 않은 것만) - 가장 마지막 상태로 적용하기 위해
     */
    @Query("SELECT cjr FROM CrewJoinRequest cjr " +
            "WHERE cjr.crew.id = :crewId " +
            "AND cjr.user.id = :userId " +
            "AND cjr.isDeleted = false " +
            "ORDER BY cjr.createdAt DESC")
    Optional<CrewJoinRequest> findByCrewIdAndUserId(
            @Param("crewId") Long crewId,
            @Param("userId") Long userId
    );

    /**
     * @param userId     사용자 ID
     * @param joinStatus 가입 신청 상태
     * @description : 특정 사용자의 특정 상태인 모든 가입 신청 목록 조회 (다른 크루 신청 취소용)
     */
    @Query("SELECT cjr FROM CrewJoinRequest cjr " +
            "WHERE cjr.user.id = :userId " +
            "AND cjr.joinStatus = :joinStatus " +
            "AND cjr.isDeleted = false")
    List<CrewJoinRequest> findAllByUserIdAndJoinStatusAndIsDeletedFalse(
            @Param("userId") Long userId,
            @Param("joinStatus") JoinStatus joinStatus
    );
}

