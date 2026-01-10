package com.multi.runrunbackend.domain.crew.repository;

import com.multi.runrunbackend.domain.crew.constant.CrewRecruitStatus;
import com.multi.runrunbackend.domain.crew.constant.CrewStatus;
import com.multi.runrunbackend.domain.crew.entity.Crew;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author : BoKyung
 * @description : 크루 Repository
 * @filename : CrewRepository
 * @since : 25. 12. 17. 수요일
 */
@Repository
public interface CrewRepository extends JpaRepository<Crew, Long> {

    /**
     * @param crewName 크루명
     * @description : 크루명 중복 확인
     */
    boolean existsByCrewName(String crewName);

    /**
     * @param cursor   마지막 조회한 크루 ID (null 가능)
     * @param pageable 페이징 정보
     * @description : 크루 목록 조회 (커서 기반 페이징, 필터링 없음)
     */
    @Query("SELECT c FROM Crew c " +
            "WHERE (:cursor IS NULL OR c.id < :cursor) " +
            "AND c.crewStatus = :status " +
            "ORDER BY c.id DESC")
    List<Crew> findAllByIdLessThanOrderByIdDesc(
            @Param("cursor") Long cursor,
            @Param("status") CrewStatus status,
            Pageable pageable
    );

    /**
     * @param cursor      마지막 조회한 크루 ID
     * @param keyword     검색 키워드
     * @param distance    거리 필터
     * @param averagePace 평균 페이스 필터
     * @param recruiting  모집 상태 필터
     * @param pageable    페이징 정보
     * @description : 크루 목록 조회 (동적 필터 조합, 모집 상태 필터 포함)
     */
    @Query("SELECT c FROM Crew c " +
            "WHERE (:cursor IS NULL OR c.id < :cursor) " +
            "AND c.crewStatus = :status " +
            "AND (:keyword IS NULL OR :keyword = '' OR c.crewName LIKE CONCAT('%', :keyword, '%')) " +
            "AND (:distance IS NULL OR :distance = '' OR c.distance = :distance) " +
            "AND (:averagePace IS NULL OR :averagePace = '' OR c.averagePace = :averagePace) " +
            "AND (:recruiting IS NULL OR " +
            "     (:recruiting = true AND c.crewRecruitStatus = :recruitingStatus) OR " +
            "     (:recruiting = false AND c.crewRecruitStatus = :closedStatus)) " +
            "ORDER BY c.id DESC")
    List<Crew> findAllWithFilters(
            @Param("cursor") Long cursor,
            @Param("keyword") String keyword,
            @Param("distance") String distance,
            @Param("averagePace") String averagePace,
            @Param("recruiting") Boolean recruiting,
            @Param("status") CrewStatus status,
            @Param("recruitingStatus") CrewRecruitStatus recruitingStatus,
            @Param("closedStatus") CrewRecruitStatus closedStatus,
            Pageable pageable
    );

    /**
     * 위임 기한이 지난 크루 조회
     */
    List<Crew> findAllByRequiresDelegationTrueAndDelegationDeadlineBefore(LocalDateTime deadline);

}
