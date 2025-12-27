package com.multi.runrunbackend.domain.rating.repository;

import com.multi.runrunbackend.common.constant.DistanceType;
import com.multi.runrunbackend.domain.rating.Rating;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author : KIMGWANGHO
 * @description : Please explain the class!!!
 * @filename : RatinigRepository
 * @since : 2025-12-27 토요일
 */
public interface RatingRepository extends JpaRepository<Rating, Long> {

  Optional<Rating> findByUserIdAndDistanceType(Long userId, DistanceType distanceType);
}
