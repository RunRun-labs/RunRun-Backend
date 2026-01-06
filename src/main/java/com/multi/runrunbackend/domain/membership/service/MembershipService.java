package com.multi.runrunbackend.domain.membership.service;


import com.multi.runrunbackend.common.exception.custom.BusinessException;
import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
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

import java.time.LocalDateTime;
import java.util.List;

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
     * @description : 멤버십 구독 (프리미엄 업그레이드)
     * @author : BoKyung
     * @since : 25. 12. 30. 월요일
     */
    @Transactional
    public void subscribeMembership(CustomUser principal) {

        User user = getUserOrThrow(principal);

        // 멤버십 존재 여부 확인
        if (!membershipRepository.existsByUser_Id(user.getId())) {
            // 멤버십 없으면 → 프리미엄 멤버십 생성
            Membership membership = Membership.create(user);
            membershipRepository.save(membership);
            log.info("프리미엄 멤버십 생성 완료 - 사용자 ID: {}", user.getId());

        } else {
            // 멤버십 있으면 → 이미 구독 중인지 확인
            Membership membership = membershipRepository.findByUser(user)
                    .orElseThrow(() -> new NotFoundException(ErrorCode.MEMBERSHIP_NOT_FOUND));

            MembershipStatus status = membership.getMembershipStatus();

            if (status == MembershipStatus.ACTIVE) {
                // 이미 활성 상태 → 에러
                throw new BusinessException(ErrorCode.MEMBERSHIP_ALREADY_PREMIUM);

            } else if (status == MembershipStatus.CANCELED) {
                // 해지 신청 상태 → 해지 취소
                membership.cancelCancellation();
                log.info("멤버십 해지 취소 - 사용자 ID: {}", user.getId());

            } else if (status == MembershipStatus.EXPIRED) {
                // 만료 상태 → 재활성화
                membership.reactivate();
                log.info("프리미엄 멤버십 재활성화 - 사용자 ID: {}", user.getId());
            }
        }
    }

    /**
     * @description : 멤버십 만료 처리 (스케줄러-> 매일 자정)
     */
    @Transactional
    public void processExpiredMemberships() {

        // 현재 시간 (yyyy-mm-dd hh:mm:ss)
        LocalDateTime now = LocalDateTime.now();

        // 해지된 상태 + 종료일이 이미 지남(현재 시간보다 이전)인 멤버십 조회
        List<Membership> expiredMemberships = membershipRepository
                .findByMembershipStatusAndEndDateBefore(MembershipStatus.CANCELED, now);

        if (expiredMemberships.isEmpty()) {
            log.info("만료 처리할 멤버십 없음");
            return;
        }

        // 조회된 멤버십 하나씩 꺼내서 처리
        for (Membership membership : expiredMemberships) {
            try {

                // 만료 상태로 바꾸기
                membership.expire();
                log.info("멤버십 만료 처리 완료 - 사용자 ID: {}",
                        membership.getUser().getId());
            } catch (Exception e) {
                log.error("멤버십 만료 처리 실패 - 사용자 ID: {}",
                        membership.getUser().getId(), e);
            }
        }

        // 몇 건 처리했는지 로그 처리
        log.info("총 {}건의 멤버십 만료 처리 완료", expiredMemberships.size());
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
