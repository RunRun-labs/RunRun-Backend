package com.multi.runrunbackend.domain.membership.service;


import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.membership.dto.res.MembershipMainResDto;
import com.multi.runrunbackend.domain.membership.entity.Membership;
import com.multi.runrunbackend.domain.membership.repository.MembershipRepository;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author : BoKyung
 * @description : 멤버십 관리 서비스
 * @filename : MembershipService
 * @since : 25. 12. 30. 월요일
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MembershipService {

    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;

    /**
     * 멤버십 메인 조회
     */
    @Transactional(readOnly = true)
    public MembershipMainResDto getMembership(CustomUser principal) {

        // 사용자 조회
        User user = getUserOrThrow(principal);

        // 멤버십 조회
        Membership membership = membershipRepository.findByUser(user)
                .orElseThrow(() -> new NotFoundException(ErrorCode.MEMBERSHIP_NOT_FOUND));

        // DTO로 변환해서 반환
        return MembershipMainResDto.fromEntity(membership);
    }

    /**
     * 사용자 조회
     */
    private User getUserOrThrow(CustomUser principal) {
        if (principal == null || principal.getLoginId() == null) {
            throw new NotFoundException(ErrorCode.USER_NOT_FOUND);
        }
        return userRepository.findByLoginId(principal.getLoginId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
    }
}
