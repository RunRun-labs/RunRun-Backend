package com.multi.runrunbackend.domain.recruit.entity;

import com.multi.runrunbackend.common.entitiy.BaseTimeEntity;
import com.multi.runrunbackend.domain.course.entity.Course;
import com.multi.runrunbackend.domain.recruit.constant.GenderLimit;
import com.multi.runrunbackend.domain.recruit.constant.RecruitStatus;
import com.multi.runrunbackend.domain.recruit.dto.req.RecruitUpdateReqDto;
import com.multi.runrunbackend.domain.user.entity.User;
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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author : KIMGWANGHO
 * @description : 모집글 관리 엔티티
 * @filename : Recruit
 * @since : 2025-12-17 수요일
 */
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "recruit")
public class Recruit extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "course_id")
  private Course course;

  @Column(length = 100, nullable = false)
  private String title;

  @Column(columnDefinition = "TEXT", nullable = false)
  private String content;

  @Column(name = "meeting_place", length = 255, nullable = false)
  private String meetingPlace;

  @Column(nullable = false)
  private Double latitude;

  @Column(nullable = false)
  private Double longitude;

  @Column(name = "target_distance")
  private Double targetDistance;

  @Column(name = "target_pace", length = 20, nullable = false)
  private String targetPace;

  @Column(name = "max_participants", nullable = false)
  private Integer maxParticipants;

  @Column(name = "age_min", nullable = false)
  private Integer ageMin;

  @Column(name = "age_max", nullable = false)
  private Integer ageMax;

  @Enumerated(EnumType.STRING)
  @Column(name = "gender_limit", length = 10, nullable = false)
  private GenderLimit genderLimit;

  @Column(name = "meeting_at", nullable = false)
  private LocalDateTime meetingAt;

  @Column(name = "current_participants", nullable = false)
  @Builder.Default
  private Integer currentParticipants = 1;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  @Builder.Default
  private RecruitStatus status = RecruitStatus.RECRUITING;

  public void increaseParticipants() {
    this.currentParticipants++;
  }

  public void decreaseParticipants() {
    if (this.currentParticipants > 0) {
      this.currentParticipants--;
    }
  }

  public void update(RecruitUpdateReqDto req) {
    this.title = req.getTitle();
    this.content = req.getContent();
    this.meetingAt = req.getMeetingAt();
    this.meetingPlace = req.getMeetingPlace();
    this.latitude = req.getLatitude();
    this.longitude = req.getLongitude();
    this.targetDistance = req.getTargetDistance();
    this.targetPace = req.getTargetPace();
    this.maxParticipants = req.getMaxParticipants();

    if (req.getGenderLimit() != null) {
      this.genderLimit = req.getGenderLimit();
    }
  }

  public void changeHost(User newHost) {
    this.user = newHost;
  }

  public void updateStatus(RecruitStatus recruitStatus) {
    this.status = recruitStatus;
  }
}
