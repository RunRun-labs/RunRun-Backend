package com.multi.runrunbackend.domain.recruit.repository;

import com.multi.runrunbackend.domain.recruit.entity.Recruit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author : KIMGWANGHO
 * @description : Please explain the class!!!
 * @filename : RecruitRepository
 * @since : 2025-12-17 수요일
 */

@Repository
public interface RecruitRepository extends JpaRepository<Recruit, Long> {

}