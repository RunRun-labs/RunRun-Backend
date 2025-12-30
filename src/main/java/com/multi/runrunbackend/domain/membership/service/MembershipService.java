package com.multi.runrunbackend.domain.membership.service;


import com.multi.runrunbackend.common.exception.custom.BadRequestException;
import com.multi.runrunbackend.common.exception.custom.BusinessException;
import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.membership.constant.MembershipGrade;
import com.multi.runrunbackend.domain.membership.constant.MembershipStatus;
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
     * @description : 멤버십 메인 조회
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
     * @description : 멤버십 해지 신청
     */
    @Transactional
    public void cancelMembership(CustomUser principal) {

        User user = getUserOrThrow(principal);

        Membership membership = membershipRepository.findByUser(user)
                .orElseThrow(() -> new NotFoundException(ErrorCode.MEMBERSHIP_NOT_FOUND));

        // 프리미엄 멤버십이 아니면 해지 불가
        if (membership.getMembershipGrade() != MembershipGrade.PREMIUM) {
            throw new BadRequestException(ErrorCode.MEMBERSHIP_NOT_PREMIUM);
        }

        // 이미 해지 신청된 상태면 에러
        if (membership.getMembershipStatus() == MembershipStatus.CANCELED) {
            throw new BusinessException(ErrorCode.MEMBERSHIP_ALREADY_CANCELED);
        }

        // 멤버십 해지 처리
        membership.cancel();

        // 종료일 설정 (다음 결제일까지 사용 가능)
        if (membership.getNextBillingDate() != null) {
            membership.setEndDate(membership.getNextBillingDate());
            log.info("멤버십 해지 신청 완료 - 사용자 ID: {}, 종료일: {}",
                    user.getId(), membership.getEndDate());
        } else {
            membership.expire();
            log.info("멤버십 즉시 만료 처리 - 사용자 ID: {}", user.getId());
        }
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
