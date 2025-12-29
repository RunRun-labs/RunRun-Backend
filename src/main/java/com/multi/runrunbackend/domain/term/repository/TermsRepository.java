package com.multi.runrunbackend.domain.term.repository;

import com.multi.runrunbackend.domain.term.constant.TermsType;
import com.multi.runrunbackend.domain.term.entity.Terms;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 *
 * @author : kimyongwon
 * @description : 이용약관 조회를 위한 레포지토리
 * @filename : TermsRepository
 * @since : 25. 12. 29. 오전 9:51 월요일
 */
public interface TermsRepository extends JpaRepository<Terms, Long> {

    Optional<Terms> findTopByTypeOrderByIdDesc(TermsType type);

    boolean existsByTypeAndVersion(TermsType type, String version);
}