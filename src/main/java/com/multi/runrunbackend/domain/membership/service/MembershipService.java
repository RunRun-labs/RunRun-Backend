package com.multi.runrunbackend.domain.membership.service;


import com.multi.runrunbackend.common.exception.custom.BusinessException;
import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.crew.constant.CrewRole;
import com.multi.runrunbackend.domain.crew.entity.Crew;
import com.multi.runrunbackend.domain.crew.entity.CrewUser;
import com.multi.runrunbackend.domain.crew.repository.CrewUserRepository;
import com.multi.runrunbackend.domain.crew.service.CrewService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    private final CrewUserRepository crewUserRepository;
    private final CrewService crewService;

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
            // nextBillingDate 없으면 즉시 만료
            membership.expire();

            // 크루장이면 즉시 위임 프로세스 시작
            Optional<CrewUser> crewLeaderOpt = crewUserRepository
                    .findByUserIdAndRoleAndIsDeletedFalse(user.getId(), CrewRole.LEADER);

            if (crewLeaderOpt.isPresent()) {
                Long crewId = crewLeaderOpt.get().getCrew().getId();
                crewService.handleLeaderMembershipExpiry(crewId, user.getId());
            }

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
     * @description : 멤버십 만료 처리
     */
    @Transactional
    public void processExpiredMemberships() {
        log.info("=== 멤버십 만료 처리 스케줄러 시작 ===");

        LocalDateTime now = LocalDateTime.now();

        // CANCELED 상태 조회
        List<Membership> canceledMemberships = membershipRepository
                .findByMembershipStatusAndEndDateBefore(
                        MembershipStatus.CANCELED,
                        now
                );

        // EXPIRED 상태 조회
        List<Membership> expiredMembershipsOnly = membershipRepository
                .findByMembershipStatusAndEndDateBefore(
                        MembershipStatus.EXPIRED,
                        now
                );

        // 두 리스트 합치기
        List<Membership> allExpiredMemberships = new ArrayList<>();
        allExpiredMemberships.addAll(canceledMemberships);
        allExpiredMemberships.addAll(expiredMembershipsOnly);

        log.info("만료 대상 멤버십: {}건 (CANCELED: {}건, EXPIRED: {}건)",
                allExpiredMemberships.size(),
                canceledMemberships.size(),
                expiredMembershipsOnly.size()
        );

        int processedCount = 0;

        // 각 멤버십 처리
        for (Membership membership : allExpiredMemberships) {
            try {
                // 멤버십 상태를 EXPIRED로 변경
                membership.expire();

                log.info("멤버십 만료 처리 완료 - 사용자 ID: {}", membership.getUser().getId());

                // 크루장인 경우 크루 처리
                Optional<CrewUser> crewLeaderOpt = crewUserRepository
                        .findByUserAndRoleAndIsDeletedFalse(
                                membership.getUser(),
                                CrewRole.LEADER
                        );

                if (crewLeaderOpt.isPresent()) {
                    CrewUser crewLeader = crewLeaderOpt.get();
                    Crew crew = crewLeader.getCrew();

                    // 크루 유효성 확인
                    if (crew != null && !crew.getIsDeleted()) {
                        log.info("크루장 멤버십 만료 - crewId: {}", crew.getId());

                        try {
                            crewService.handleLeaderMembershipExpiry(
                                    crew.getId(),
                                    membership.getUser().getId()
                            );
                        } catch (jakarta.persistence.EntityNotFoundException e) {
                            log.warn("크루가 이미 삭제됨 - userId: {}, 사유: {}",
                                    membership.getUser().getId(), e.getMessage());
                        } catch (Exception e) {
                            log.error("크루 처리 실패 - crewId: {}", crew.getId(), e);
                        }
                    } else {
                        log.info("크루가 이미 삭제됨 - userId: {}", membership.getUser().getId());
                    }
                }

                processedCount++;

            } catch (Exception e) {
                log.error("멤버십 만료 처리 실패 - membershipId: {}, 사유: {}",
                        membership.getId(), e.getMessage(), e);
            }
        }

        log.info("총 {}건의 멤버십 만료 처리 완료", processedCount);
        log.info("=== 멤버십 만료 처리 스케줄러 종료 ===");
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
