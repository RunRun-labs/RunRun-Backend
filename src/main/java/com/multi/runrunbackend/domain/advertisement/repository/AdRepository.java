package com.multi.runrunbackend.domain.advertisement.repository;

import com.multi.runrunbackend.domain.advertisement.entity.Ad;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : AdRepository
 * @since : 2026. 1. 11. Sunday
 */
@Repository
public interface AdRepository extends JpaRepository<Ad, Long> {

    Page<Ad> findByNameContainingIgnoreCase(String keyword, Pageable pageable);
}
