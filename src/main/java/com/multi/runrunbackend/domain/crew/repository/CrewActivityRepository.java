package com.multi.runrunbackend.domain.crew.repository;

import com.multi.runrunbackend.domain.crew.entity.CrewActivity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
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
public interface CrewActivityRepository {

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
}
