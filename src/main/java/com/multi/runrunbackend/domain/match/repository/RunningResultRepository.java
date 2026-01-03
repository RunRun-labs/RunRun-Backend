package com.multi.runrunbackend.domain.match.repository;

import com.multi.runrunbackend.domain.match.constant.RunStatus;
import com.multi.runrunbackend.domain.match.entity.RunningResult;
import com.multi.runrunbackend.domain.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 런닝 결과 Repository
 *
 * @author : chang
 * @since : 2024-12-23
 */
public interface RunningResultRepository extends JpaRepository<RunningResult, Long> {

    /**
     * 사용자의 최신 런닝 결과 조회 - 런닝 종료 후 결과 모달 표시용
     *
     * @param user 사용자 엔티티
     * @return 최신 런닝 결과
     */
    Optional<RunningResult> findTopByUserOrderByCreatedAtDesc(User user);

    /**
     * 특정 사용자의 특정 런닝 결과 조회
     * 자기 러닝 결과만 공유하기 위함
     */
    Optional<RunningResult> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT r FROM RunningResult r WHERE r.course.id = :courseId AND r.runStatus = :runStatus AND r.isDeleted = false ORDER BY r.createdAt DESC")
    List<RunningResult> findGhostCandidates(@Param("courseId") Long courseId,
                                            @Param("runStatus") RunStatus runStatus);


    @Query("SELECT r FROM RunningResult r " +
            "WHERE r.user.id = :userId " +
            "AND r.runStatus = :runStatus " +
            "AND r.isDeleted = false " +
            "AND (:minDistance IS NULL OR r.totalDistance > :minDistance) " +
            "AND (:maxDistance IS NULL OR r.totalDistance <= :maxDistance) " +
            "ORDER BY r.createdAt DESC")
    Slice<RunningResult> findMySoloRecordsByDistance(
            @Param("userId") Long userId,
            @Param("runStatus") RunStatus runStatus,
            @Param("minDistance") BigDecimal minDistance,
            @Param("maxDistance") BigDecimal maxDistance,
            Pageable pageable
    );
}
