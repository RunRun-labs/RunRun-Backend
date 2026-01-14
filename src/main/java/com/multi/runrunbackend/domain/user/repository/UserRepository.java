package com.multi.runrunbackend.domain.user.repository;

import com.multi.runrunbackend.domain.user.entity.User;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;


public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByLoginId(String memberId);

    Optional<User> findById(Long memberNo);

    boolean existsByEmail(String email);
    
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
