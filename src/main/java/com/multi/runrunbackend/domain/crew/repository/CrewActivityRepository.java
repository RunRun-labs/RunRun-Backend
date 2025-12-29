package com.multi.runrunbackend.domain.crew.repository;

import com.multi.runrunbackend.domain.crew.entity.CrewActivity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author : BoKyung
 * @description : 크루 활동 Repository
 * @filename : CrewActivityRepository
 * @since : 25. 12. 18. 목요일
 */
@Repository
public interface CrewActivityRepository extends CrudRepository<CrewActivity, Long> {

    /**
     * @param crewId   크루 ID
     * @param pageable 페이징 정보
     * @description : 특정 크루의 최근 활동 내역 조회 (최대 5개)
     */
    @Query("SELECT ca FROM CrewActivity ca " +
            "WHERE ca.crew.id = :crewId " +
            "AND ca.isDeleted = false " +
            "ORDER BY ca.createdAt DESC")
    List<CrewActivity> findTop5ByCrewIdOrderByCreatedAtDesc(
            @Param("crewId") Long crewId,
            Pageable pageable
    );

    /**
     * @param crewId 크루 ID
     * @return 참여 횟수 합계
     * @description :  특정 크루의 전체 참여 횟수 합계를 계산
     */
    @Query("SELECT COALESCE(SUM(ca.participationCnt), 0) " +
            "FROM CrewActivity ca " +
            "WHERE ca.crew.id = :crewId " +
            "AND ca.isDeleted = false")
    Long calculateTotalParticipation(
            @Param("crewId") Long crewId
    );

    /**
     * @param crewId 크루 ID
     * @return 활동 수
     * @description : 특정 크루의 총 활동 수 조회
     */
    @Query("SELECT COUNT(ca) " +
            "FROM CrewActivity ca " +
            "WHERE ca.crew.id = :crewId " +
            "AND ca.isDeleted = false")
    Long countByCrewId(@Param("crewId") Long crewId);
}
