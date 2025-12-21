package com.multi.runrunbackend.domain.recruit.repository;

import com.multi.runrunbackend.domain.recruit.entity.Recruit;
import com.multi.runrunbackend.domain.recruit.entity.RecruitUser;
import com.multi.runrunbackend.domain.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
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
}


