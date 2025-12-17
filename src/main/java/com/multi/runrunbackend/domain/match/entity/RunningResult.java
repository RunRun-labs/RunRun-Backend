package com.multi.runrunbackend.domain.match.entity;

import com.multi.runrunbackend.common.entitiy.BaseEntity;
import com.multi.runrunbackend.domain.course.entity.Course;
import com.multi.runrunbackend.domain.match.constant.RunStatus;
import com.multi.runrunbackend.domain.match.constant.RunningType;
import com.multi.runrunbackend.domain.user.entity.User;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;

/**
 * @author : chang
 * @description : 런닝결과 엔티티
 * @filename : RunningResult
 * @since : 2025-12-17 수요일
 */
@Entity
@Table(name = "running_result")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RunningResult extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @Column(name = "total_distance", nullable = false, precision = 7, scale = 2)
    private BigDecimal totalDistance;

    @Column(name = "total_time", nullable = false)
    private Integer totalTime;

    @Column(name = "avg_pace", nullable = false, precision = 6, scale = 2)
    private BigDecimal avgPace;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "run_status", nullable = false, length = 20)
    private RunStatus runStatus;

    @Type(JsonBinaryType.class)
    @Column(name = "split_pace", nullable = false, columnDefinition = "jsonb")
    private List<Map<String, Object>> splitPace;

    @Enumerated(EnumType.STRING)
    @Column(name = "running_type", nullable = false, length = 20)
    private RunningType runningType;


}