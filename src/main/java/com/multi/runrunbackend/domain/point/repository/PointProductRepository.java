package com.multi.runrunbackend.domain.point.repository;

import com.multi.runrunbackend.domain.point.entity.PointProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author : BoKyung
 * @description : 포인트 상품 Repository
 * @filename : PointProductRepository
 * @since : 2026. 01. 02. 금요일
 */
@Repository
public interface PointProductRepository extends JpaRepository<PointProduct, Long> {

    List<PointProduct> findByIsDeletedFalseAndIsAvailableTrue();
}
