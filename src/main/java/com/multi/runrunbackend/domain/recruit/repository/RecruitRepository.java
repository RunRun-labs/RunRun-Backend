package com.multi.runrunbackend.domain.recruit.repository;

import com.multi.runrunbackend.domain.recruit.entity.Recruit;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @author : KIMGWANGHO
 * @description : 러닝 모집글(Recruit) 엔티티의 데이터베이스 CRUD(생성, 조회, 수정, 삭제) 작업을 담당하는 인터페이스
 * @filename : RecruitRepository
 * @since : 2025-12-17 수요일
 */

@Repository
public interface RecruitRepository extends JpaRepository<Recruit, Long> {

  @Query(value = """
      SELECT *, 
             (6371 * acos(cos(radians(:lat)) * cos(radians(r.latitude))
             * cos(radians(r.longitude) - radians(:lon))
             + sin(radians(:lat)) * sin(radians(r.latitude)))) AS distance
      FROM recruit r
      WHERE r.is_deleted = false
      AND (:keyword IS NULL OR r.title LIKE CONCAT('%', :keyword, '%'))
      AND (:radius IS NULL OR 
          (6371 * acos(cos(radians(:lat)) * cos(radians(r.latitude))
          * cos(radians(r.longitude) - radians(:lon))
          + sin(radians(:lat)) * sin(radians(r.latitude)))) <= :radius
      )
      """,
      countQuery = "SELECT count(*) FROM recruit r WHERE r.is_deleted = false AND (:keyword IS NULL OR r.title LIKE CONCAT('%', :keyword, '%'))",
      nativeQuery = true)
  Slice<Recruit> findRecruitsWithFilters(
      @Param("lat") Double myLat,
      @Param("lon") Double myLon,
      @Param("radius") Double radius,
      @Param("keyword") String keyword,
      Pageable pageable
  );
}