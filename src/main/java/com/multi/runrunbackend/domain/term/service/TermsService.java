package com.multi.runrunbackend.domain.term.service;

import com.multi.runrunbackend.common.exception.custom.ForbiddenException;
import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.custom.TokenException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.term.constant.TermsType;
import com.multi.runrunbackend.domain.term.dto.req.TermsReqDto;
import com.multi.runrunbackend.domain.term.dto.res.TermsResDto;
import com.multi.runrunbackend.domain.term.entity.Terms;
import com.multi.runrunbackend.domain.term.repository.TermsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author : kimyongwon
 * @description : 특정 타입의 가장 최신 버전의 약관을 조회하고,
 * 새로운 버전의 약관을 저장하는 서비스
 * @filename : TermsService
 * @since : 25. 12. 29. 오전 10:05 월요일
 */
@Service
@RequiredArgsConstructor
public class TermsService {

    private final TermsRepository termsRepository;


    @Transactional(readOnly = true)
    public TermsResDto getLatestTerms(TermsType type) {
        Terms terms = termsRepository.findTopByTypeOrderByIdDesc(type)
                .orElseThrow(() -> new NotFoundException(ErrorCode.INVALID_REQUEST)); // 약관이 없을 경우 예외 처리

        return TermsResDto.from(terms);
    }


    @Transactional
    public void createTerms(TermsReqDto req, CustomUser principal) {
        validateAdminRole(principal);

        // 엔티티의 from 메서드를 사용하여 생성
        Terms terms = Terms.toEntity(req);

        termsRepository.save(terms);
    }

    private void validateAdminRole(CustomUser principal) {
        if (principal == null) {
            throw new TokenException(ErrorCode.UNAUTHORIZED);
        }

        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            throw new ForbiddenException(ErrorCode.TERMS_ACCESS_DENIED);
        }
    }
}