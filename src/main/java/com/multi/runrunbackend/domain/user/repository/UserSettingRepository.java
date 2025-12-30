package com.multi.runrunbackend.domain.user.repository;

import com.multi.runrunbackend.domain.user.entity.UserSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 *
 * @author : kimyongwon
 * @description : 사용자 ID로 설정을 조회하는 메서드
 * @filename : UserSettingRepository
 * @since : 25. 12. 28. 오후 10:10 일요일
 */
public interface UserSettingRepository extends JpaRepository<UserSetting, Long> {

    Optional<UserSetting> findByUserId(Long userId);
}