package com.multi.runrunbackend.domain.recruit.repository;

import com.multi.runrunbackend.domain.recruit.constant.RecruitStatus;
import com.multi.runrunbackend.domain.recruit.entity.Recruit;
import com.multi.runrunbackend.domain.recruit.entity.RecruitUser;
import com.multi.runrunbackend.domain.user.entity.User;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @author : KIMGWANGHO
 * @description : RecruitUser 엔티티에 대한 CRUD 및 커스텀 조회를 담당
 * @filename : RecruitUserRepository
 * @since : 2025-12-21 수요일
 */
@Repository
public interface RecruitUserRepository extends JpaRepository<RecruitUser, Long> {

  Optional<RecruitUser> findByRecruitAndUser(Recruit recruit, User user);

  boolean existsByRecruitAndUser(Recruit recruit, User user);

  Optional<RecruitUser> findFirstByRecruitAndUserNotOrderByCreatedAtAsc(Recruit recruit, User user);

  List<RecruitUser> findAllByRecruitId(Long recruitId);

  /**
   * 사용자가 특정 날짜에 참가한 모집글이 있는지 확인 (RECRUITING 또는 MATCHED 상태만 체크)
   */
  @Query("SELECT COUNT(ru) > 0 FROM RecruitUser ru " +
      "WHERE ru.user = :user " +
      "AND DATE(ru.recruit.meetingAt) = :date " +
      "AND ru.recruit.status IN :statuses")
  boolean existsByUserAndMeetingDate(
      @Param("user") User user,
      @Param("date") LocalDate date,
      @Param("statuses") List<RecruitStatus> statuses
  );
}


