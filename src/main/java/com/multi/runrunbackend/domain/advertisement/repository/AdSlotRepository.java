package com.multi.runrunbackend.domain.advertisement.repository;

import com.multi.runrunbackend.domain.advertisement.constant.AdSlotType;
import com.multi.runrunbackend.domain.advertisement.entity.AdSlot;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : AdSlotRepository
 * @since : 2026. 1. 11. Sunday
 */
@Repository
public interface AdSlotRepository extends JpaRepository<AdSlot, Long>,
    JpaSpecificationExecutor<AdSlot> {

    Optional<AdSlot> findBySlotType(AdSlotType slotType);

    Page<AdSlot> findByNameContainingIgnoreCase(String keyword, Pageable pageable);
}
