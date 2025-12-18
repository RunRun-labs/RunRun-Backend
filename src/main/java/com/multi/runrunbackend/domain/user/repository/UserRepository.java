package com.multi.runrunbackend.domain.user.repository;

import com.multi.runrunbackend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByLoginId(String memberId);

    Optional<User> findById(Long memberNo);

    boolean existsByEmail(String email);
}
