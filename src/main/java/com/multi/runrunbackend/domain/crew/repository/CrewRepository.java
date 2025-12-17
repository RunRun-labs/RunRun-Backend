package com.multi.runrunbackend.domain.crew.repository;

import com.multi.runrunbackend.domain.crew.entity.Crew;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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
     * @param id 크루 ID
     * @description : 크루 ID로 조회 (is_deleted = false만)
     */
    Optional<Crew> findByIdAndIsDeletedFalse(@Param("id") Long id);

    /**
     * @param cursor   마지막 조회한 크루 ID (null 가능)
     * @param pageable 페이징 정보
     * @description : 크루 목록 조회 (커서 기반 페이징, 필터링 없음)
     */
    @Query("SELECT c FROM Crew c " +
            "WHERE (:cursor IS NULL OR c.id < :cursor) " +
            "AND c.crewStatus = 'ACTIVE' " +
            "ORDER BY c.id DESC")
    List<Crew> findAllByIdLessThanOrderByIdDesc(
            @Param("cursor") Long cursor,
            Pageable pageable
    );

    /**
     * @param cursor   마지막 조회한 크루 ID
     * @param region   지역
     * @param pageable 페이징 정보
     * @description : 크루 목록 조회 (지역 필터링)
     */
    @Query("SELECT c FROM Crew c " +
            "WHERE (:cursor IS NULL OR c.id < :cursor) " +
            "AND c.region = :region " +
            "AND c.crewStatus = 'ACTIVE' " +
            "ORDER BY c.id DESC")
    List<Crew> findAllByRegionAndIdLessThanOrderByIdDesc(
            @Param("cursor") Long cursor,
            @Param("region") String region,
            Pageable pageable
    );

    /**
     * @param cursor   마지막 조회한 크루 ID
     * @param distance 러닝거리
     * @param pageable 페이징 정보
     * @description : 크루 목록 조회 (러닝거리 필터링)
     */
    @Query("SELECT c FROM Crew c " +
            "WHERE (:cursor IS NULL OR c.id < :cursor) " +
            "AND c.distance = :distance " +
            "AND c.crewStatus = 'ACTIVE' " +
            "ORDER BY c.id DESC")
    List<Crew> findAllByDistanceAndIdLessThanOrderByIdDesc(
            @Param("cursor") Long cursor,
            @Param("distance") String distance,
            Pageable pageable
    );

    /**
     * @param cursor   마지막 조회한 크루 ID
     * @param region   지역
     * @param distance 러닝거리
     * @param pageable 페이징 정보
     * @description : 크루 목록 조회 (지역 + 러닝거리 필터링)
     */
    @Query("SELECT c FROM Crew c " +
            "WHERE (:cursor IS NULL OR c.id < :cursor) " +
            "AND c.region = :region " +
            "AND c.distance = :distance " +
            "AND c.crewStatus = 'ACTIVE' " +
            "ORDER BY c.id DESC")
    List<Crew> findAllByRegionAndDistanceAndIdLessThanOrderByIdDesc(
            @Param("cursor") Long cursor,
            @Param("region") String region,
            @Param("distance") String distance,
            Pageable pageable
    );

    /**
     * @param cursor   마지막 조회한 크루 ID
     * @param pageable 페이징 정보
     * @description : 크루 목록 조회 (모집중 우선 정렬 - Recruiting)
     */
    @Query("SELECT c FROM Crew c " +
            "WHERE (:cursor IS NULL OR c.id < :cursor) " +
            "AND c.crewStatus = 'ACTIVE' " +
            "ORDER BY CASE WHEN c.crewRecruitStatus = 'RECRUITING' THEN 0 ELSE 1 END, c.id DESC")
    List<Crew> findAllOrderByRecruitStatusDescIdDesc(
            @Param("cursor") Long cursor,
            Pageable pageable
    );

    /**
     * @param keyword  검색 키워드
     * @param cursor   마지막 조회한 크루 ID
     * @param pageable 페이징 정보
     * @description : 크루 목록 조회 (크루명 검색)
     */
    @Query("SELECT c FROM Crew c " +
            "WHERE (:cursor IS NULL OR c.id < :cursor) " +
            "AND c.crewName LIKE %:keyword% " +
            "AND c.crewStatus = 'ACTIVE' " +
            "ORDER BY c.id DESC")
    List<Crew> findAllByCrewNameContainingAndIdLessThanOrderByIdDesc(
            @Param("keyword") String keyword,
            @Param("cursor") Long cursor,
            Pageable pageable
    );
}
