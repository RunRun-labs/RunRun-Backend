package com.multi.runrunbackend.domain.crew.service;

import com.multi.runrunbackend.common.exception.custom.BusinessException;
import com.multi.runrunbackend.common.exception.custom.ForbiddenException;
import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.common.file.storage.FileStorage;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.crew.constant.CrewRecruitStatus;
import com.multi.runrunbackend.domain.crew.constant.CrewRole;
import com.multi.runrunbackend.domain.crew.constant.JoinStatus;
import com.multi.runrunbackend.domain.crew.dto.req.CrewJoinReqDto;
import com.multi.runrunbackend.domain.crew.dto.res.CrewJoinRequestResDto;
import com.multi.runrunbackend.domain.crew.dto.res.CrewUserResDto;
import com.multi.runrunbackend.domain.crew.entity.Crew;
import com.multi.runrunbackend.domain.crew.entity.CrewJoinRequest;
import com.multi.runrunbackend.domain.crew.entity.CrewUser;
import com.multi.runrunbackend.domain.crew.repository.CrewJoinRequestRepository;
import com.multi.runrunbackend.domain.crew.repository.CrewRepository;
import com.multi.runrunbackend.domain.crew.repository.CrewUserRepository;
import com.multi.runrunbackend.domain.membership.constant.MembershipStatus;
import com.multi.runrunbackend.domain.membership.repository.MembershipRepository;
import com.multi.runrunbackend.domain.notification.constant.NotificationType;
import com.multi.runrunbackend.domain.notification.constant.RelatedType;
import com.multi.runrunbackend.domain.notification.service.NotificationService;
import com.multi.runrunbackend.domain.point.constant.PointPolicy;
import com.multi.runrunbackend.domain.point.service.PointService;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author : BoKyung
 * @description : 크루 가입 Service
 * @filename : CrewJoinService
 * @since : 25. 12. 22. 월요일
 */

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CrewJoinService {

    private final CrewRepository crewRepository;
    private final CrewUserRepository crewUserRepository;
    private final CrewJoinRequestRepository crewJoinRequestRepository;
    private final UserRepository userRepository;
    private final CrewChatService crewChatService;
    private final PointService pointService;
    private final FileStorage fileStorage;
    private final MembershipRepository membershipRepository;
    private final NotificationService notificationService;

    private static final int CREW_JOIN_POINT = 100;

    /**
     * @param crewId    가입 신청할 크루 ID
     * @param principal 가입 신청하는 회원
     * @param reqDto    가입 신청 정보 (자기소개, 거리, 페이스, 지역)
     * @description : 회원이 크루에 가입 신청 (이미 가입했거나 대기중인 경우 예외 발생) TODO 포인트 구현시, 포인트 100P가 차감
     */
    @Transactional
    public void requestJoin(Long crewId, CustomUser principal, CrewJoinReqDto reqDto) {
        //크루 조회
        Crew crew = findCrewById(crewId);

        // 회원 조회
        User user = getUserOrThrow(principal);

        // 포인트 검증 (100P 필요)
        pointService.validateSufficientPoints(user.getId(), CREW_JOIN_POINT);

        // 모집 상태 확인 - 모집 마감이면 신청 불가
        if (crew.getCrewRecruitStatus() == CrewRecruitStatus.CLOSED) {
            throw new BusinessException(ErrorCode.CREW_RECRUITMENT_CLOSED);
        }

        // 1인 1크루 검증(다른 크루)
        validateNotInAnotherCrew(user.getId());

        // 이미 가입한 크루원인지 확인(해당 크루)
        // TODO: 현재는 1인 1크루 정책으로 인해 항상 false이나, 향후 변경 시를 대비해 유지하는 걸로 판단
        boolean alreadyJoined = crewUserRepository.existsByCrewIdAndUserIdAndIsDeletedFalse(crewId,
                user.getId());
        if (alreadyJoined) {
            throw new BusinessException(ErrorCode.ALREADY_JOINED_CREW);
        }

        // 이미 대기중인 신청이 있는지 확인
        boolean alreadyRequested = crewJoinRequestRepository.existsByCrewAndUserAndJoinStatus(
                crew, user, JoinStatus.PENDING
        );
        if (alreadyRequested) {
            throw new BusinessException(ErrorCode.ALREADY_REQUESTED);
        }

        // 가입 신청 생성
        CrewJoinRequest joinRequest = CrewJoinRequest.create(
                crew,
                user,
                reqDto.getIntroduction(),
                reqDto.getDistance(),
                reqDto.getPace(),
                reqDto.getRegion()
        );

        crewJoinRequestRepository.save(joinRequest);

        try {
            List<CrewUser> leaders = crewUserRepository
                    .findByCrewAndRoleAndIsDeletedFalse(crew, CrewRole.LEADER);

            if (!leaders.isEmpty()) {
                CrewUser leader = leaders.get(0);
                notificationService.create(
                        leader.getUser(),
                        "크루 가입 신청",
                        user.getName() + "님이 크루 가입을 신청했습니다.",
                        NotificationType.CREW,
                        RelatedType.CREW_JOIN_REQUEST,
                        joinRequest.getId()
                );
                log.info("크루 가입 신청 알림 발송 완료 - crewId: {}, joinRequestId: {}, leaderId: {}",
                        crewId, joinRequest.getId(), leader.getUser().getId());
            } else {
                log.warn("크루장을 찾을 수 없어 알림을 발송하지 않음 - crewId: {}", crewId);
            }
        } catch (Exception e) {
            log.error("크루 가입 신청 알림 발송 실패 - crewId: {}, joinRequestId: {}",
                    crewId, joinRequest.getId(), e);
        }

        log.info("크루 가입 신청 완료 - crewId: {}, userId: {}", crewId, user.getId());
    }

    /**
     * @param crewId    크루 ID
     * @param principal 신청 취소하는 회원
     * @description : 신청자가 대기중인 가입 신청을 취소 TODO (포인트 구현시, 차감된 포인트 100P가 환불)
     */
    @Transactional
    public void cancelJoinRequest(Long crewId, CustomUser principal) {
        //크루 조회
        Crew crew = findCrewById(crewId);

        // 회원 조회
        User user = getUserOrThrow(principal);

        // 대기중인 가입 신청 조회
        CrewJoinRequest joinRequest = crewJoinRequestRepository.findByCrewAndUserAndJoinStatus(
                        crew, user, JoinStatus.PENDING)
                .orElseThrow(() -> new NotFoundException(ErrorCode.JOIN_REQUEST_NOT_FOUND));

        // 가입 신청 취소로 상태 변경
        joinRequest.cancel();

        log.info("크루 가입 신청 취소 완료 - crewId: {}, userId: {}", crewId, user.getId());
    }

    /**
     * @param crewId    크루 ID
     * @param principal 조회하는 크루장
     * @description : 크루장이 대기중인 가입 신청 목록을 조회 (크루장 또는 부크루장만 조회 가능)
     */
    public List<CrewJoinRequestResDto> getJoinRequestList(Long crewId, CustomUser principal) {
        //크루 조회
        Crew crew = findCrewById(crewId);

        // 회원 조회
        User user = getUserOrThrow(principal);

        // 크루장 또는 부크루장 권한 확인
        validateLeaderOrSubLeader(crew, user);

        // 대기중인 가입 신청 목록 조회
        List<CrewJoinRequest> joinRequests = crewJoinRequestRepository
                .findAllByCrewAndJoinStatusAndIsDeletedFalse(crew, JoinStatus.PENDING);

        // DTO로 변환하여 반환
        return joinRequests.stream()
                .map(CrewJoinRequestResDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * @param crewId        크루 ID
     * @param principal     가입 신청하는 회원
     * @param joinRequestId 처리할 가입 신청 ID
     * @description : 크루장이 가입 신청을 승인 (승인 시 크루원으로 추가)
     */
    @Transactional
    public void approveJoinRequest(Long crewId, CustomUser principal, Long joinRequestId) {
        //크루 조회
        Crew crew = findCrewById(crewId);

        // 크루장 조회
        User leader = getUserOrThrow(principal);

        // 크루장 또는 부크루장 권한 확인
        validateLeaderOrSubLeader(crew, leader);

        // 가입 신청 조회
        CrewJoinRequest joinRequest = crewJoinRequestRepository.findByIdAndIsDeletedFalse(joinRequestId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.JOIN_REQUEST_NOT_FOUND));

        // 크루 ID 일치 확인
        if (!joinRequest.getCrew().getId().equals(crewId)) {
            throw new BusinessException(ErrorCode.JOIN_REQUEST_NOT_FOUND);
        }

        Long userId = joinRequest.getUser().getId();

        // 승인 시점에 다시 1인 1크루 정책 확인 (동시성 이슈 방지)
        validateNotInAnotherCrew(userId);

        // 승인 처리
        joinRequest.approve();

        // 포인트 차감(100p)
        pointService.deductPointsForCrewJoin(
                userId,
                PointPolicy.CREW_JOIN.getPoints(),
                PointPolicy.CREW_JOIN.getDescription()
        );

        // 재검증 - 포인트 차감 중 다른 크루 승인된 경우 감지
        if (crewUserRepository.existsByUserIdAndIsDeletedFalse(userId)) {
            throw new BusinessException(ErrorCode.ALREADY_JOINED_CREW);
        }

        // 해당 사용자의 다른 크루 대기 중인 신청 모두 취소
        cancelOtherPendingRequestsForUser(userId, crewId);

        // 크루원으로 추가
        CrewUser crewUser = CrewUser.create(crew, joinRequest.getUser(), CrewRole.MEMBER);
        crewUserRepository.save(crewUser);

        // 채팅방 참여자 추가
        crewChatService.addUserToChatRoom(crewId, joinRequest.getUser());

        User newMember = joinRequest.getUser();
        try {
            List<CrewUser> allMembers = crewUserRepository
                    .findByCrewAndIsDeletedFalse(crew);

            for (CrewUser member : allMembers) {
                try {
                    notificationService.create(
                            member.getUser(),
                            "새 크루원 가입",
                            newMember.getName() + "님이 크루에 가입했습니다.",
                            NotificationType.CREW,
                            RelatedType.CREW,
                            crewId
                    );
                    log.debug("크루원 가입 알림 발송 완료 - crewId: {}, receiverId: {}, newMemberId: {}",
                            crewId, member.getUser().getId(), newMember.getId());
                } catch (Exception e) {
                    log.error("크루원 가입 알림 생성 실패 - crewId: {}, receiverId: {}, newMemberId: {}",
                            crewId, member.getUser().getId(), newMember.getId(), e);
                }
            }
            log.info("크루원 전체에게 가입 알림 발송 완료 - crewId: {}, newMemberId: {}, totalMembers: {}",
                    crewId, newMember.getId(), allMembers.size());
        } catch (Exception e) {
            log.error("크루원 전체 알림 발송 중 오류 발생 - crewId: {}, newMemberId: {}",
                    crewId, newMember.getId(), e);
        }

        log.info("크루 가입 승인 완료 - crewId: {}, newMemberId: {}",
                crewId, userId);

    }

    /**
     * @param crewId        크루 ID
     * @param principal     가입 신청하는 회원
     * @param joinRequestId 거절할 가입 신청 ID
     * @description : 크루장이 가입 신청을 거절
     */
    @Transactional
    public void rejectJoinRequest(Long crewId, CustomUser principal, Long joinRequestId) {
        // 크루 조회
        Crew crew = findCrewById(crewId);

        // 크루장 조회
        User leader = getUserOrThrow(principal);

        // 크루장 또는 부크루장 권한 확인
        validateLeaderOrSubLeader(crew, leader);

        // 가입 신청 조회
        CrewJoinRequest joinRequest = crewJoinRequestRepository.findByIdAndIsDeletedFalse(joinRequestId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.JOIN_REQUEST_NOT_FOUND));

        // 크루 ID 일치 확인
        if (!joinRequest.getCrew().getId().equals(crewId)) {
            throw new BusinessException(ErrorCode.JOIN_REQUEST_NOT_FOUND);
        }

        // 거절 처리
        joinRequest.reject();

        log.info("크루 가입 거절 완료 - crewId: {}, rejectedUserId: {}",
                crewId, joinRequest.getUser().getId());
    }

    /**
     * @param crewId    크루 ID
     * @param principal 가입 신청하는 회원
     * @description : 크루원이 크루 탈퇴 (승인된 크루원만 탈퇴 가능, 크루장은 탈퇴 불가 -> 위임 또는 해체 필요)
     */
    @Transactional
    public void leaveCrew(Long crewId, CustomUser principal) {
        // 크루 조회
        Crew crew = findCrewById(crewId);

        // 회원 조회
        User user = getUserOrThrow(principal);

        // 크루원인지 확인
        CrewUser crewUser = crewUserRepository.findByCrewAndUserAndIsDeletedFalse(crew, user)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREW_MEMBER_NOT_FOUND));

        try {
            List<CrewUser> allMembers = crewUserRepository
                    .findByCrewAndIsDeletedFalse(crew);

            String message = user.getName() + "님이 크루를 탈퇴했습니다.";

            for (CrewUser member : allMembers) {
                if (member.getUser().getId().equals(user.getId())) {
                    continue;
                }

                try {
                    notificationService.create(
                            member.getUser(),
                            "크루원 탈퇴",
                            message,
                            NotificationType.CREW,
                            RelatedType.CREW_USERS,
                            crewId
                    );
                    log.debug("크루원 탈퇴 알림 발송 완료 - crewId: {}, receiverId: {}, leavingUserId: {}",
                            crewId, member.getUser().getId(), user.getId());
                } catch (Exception e) {
                    log.error("크루원 탈퇴 알림 생성 실패 - crewId: {}, receiverId: {}, leavingUserId: {}",
                            crewId, member.getUser().getId(), user.getId(), e);
                }
            }
            log.info("크루원 전체에게 탈퇴 알림 발송 완료 - crewId: {}, leavingUserId: {}, totalMembers: {}",
                    crewId, user.getId(), allMembers.size());
        } catch (Exception e) {
            log.error("크루원 전체 알림 발송 중 오류 발생 - crewId: {}, leavingUserId: {}",
                    crewId, user.getId(), e);
        }

        // 크루장인 경우 특별 처리
        if (crewUser.getRole().equals(CrewRole.LEADER)) {
            // 부크루장 중 멤버십 있는 사람 찾기
            List<CrewUser> subLeaders = crewUserRepository
                    .findByCrewAndRoleAndIsDeletedFalse(crew, CrewRole.SUB_LEADER);

            CrewUser newLeader = null;
            for (CrewUser subLeader : subLeaders) {
                // 멤버십 확인
                boolean hasPremium = membershipRepository
                        .existsByUser_IdAndMembershipStatusIn(
                                subLeader.getUser().getId(),
                                List.of(MembershipStatus.ACTIVE, MembershipStatus.CANCELED)
                        );

                if (hasPremium) {
                    newLeader = subLeader;
                    break;
                }
            }

            if (newLeader == null) {
                // 위임할 사람 없음 -> 탈퇴 불가
                throw new BusinessException(ErrorCode.CANNOT_LEAVE_WITHOUT_SUCCESSOR);
            }

            // 자동 위임
            newLeader.changeRole(CrewRole.LEADER);
            log.info("크루장 자동 위임 - crewId: {}, 기존: {}, 새로운: {}",
                    crewId, user.getId(), newLeader.getUser().getId());
        }

        // CrewUser soft delete 처리
        crewUser.delete();

        // 승인된 가입 신청도 함께 처리 (APPROVED 상태의 가입 신청을 찾아서 soft delete)
        crewJoinRequestRepository.findByCrewAndUserAndJoinStatus(crew, user, JoinStatus.APPROVED)
                .ifPresent(joinRequest -> joinRequest.delete());

        // 채팅방 참여자 제거
        crewChatService.removeUserFromChatRoom(crewId, user);

        log.info("크루 탈퇴 완료 - crewId: {}, userId: {}, role: {}",
                crewId, user.getId(), crewUser.getRole());
    }

    /**
     * @param crewId    크루 ID
     * @param principal 조회하는 사용자
     * @description : 크루원 목록 조회 (역할 순서로 정렬: LEADER -> SUB_LEADER -> STAFF -> MEMBER, 멤버십 정보 포함)
     */
    public List<CrewUserResDto> getCrewUserList(Long crewId, CustomUser principal) {
        // 크루 조회
        Crew crew = findCrewById(crewId);

        // 사용자 조회
        User user = getUserOrThrow(principal);

        // 해당 크루의 크루원인지 확인
        validateCrewUser(crew, user);

        // 크루원 목록 + 참여 횟수를 한 번에 조회
        List<Object[]> results = crewUserRepository
                .findAllWithParticipationCountAndLastActivity(crewId);

        // DTO 변환 + S3 URL 변환 + 멤버십 정보 추가
        return results.stream()
                .map(result -> {
                    CrewUser crewUser = (CrewUser) result[0];
                    Long participationCount = (Long) result[1];
                    LocalDateTime lastActivityDate = (LocalDateTime) result[2];

                    // 멤버십 확인
                    boolean hasMembership = membershipRepository
                            .existsByUser_IdAndMembershipStatusIn(
                                    crewUser.getUser().getId(),
                                    List.of(MembershipStatus.ACTIVE, MembershipStatus.CANCELED)
                            );

                    // S3 URL 변환
                    String profileImageUrl = crewUser.getUser().getProfileImageUrl();
                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        // S3 key면 HTTPS URL로 변환
                        if (!profileImageUrl.startsWith("http")) {
                            profileImageUrl = fileStorage.toHttpsUrl(profileImageUrl);
                        }
                    }

                    // DTO 생성 (멤버십 정보 포함)
                    return CrewUserResDto.builder()
                            .userId(crewUser.getUser().getId())
                            .userName(crewUser.getUser().getName())
                            .profileImageUrl(profileImageUrl)
                            .role(crewUser.getRole())
                            .createdAt(crewUser.getCreatedAt())
                            .participationCount(participationCount.intValue())
                            .lastActivityDate(lastActivityDate)
                            .hasMembership(hasMembership)
                            .build();
                })
                .toList();

    }

    /**
     * @param crewId    크루 ID
     * @param userId    권한을 변경할 사용자 ID
     * @param newRole   변경할 역할
     * @param principal 크루장
     * @description : 크루장이 크루원의 권한을 변경
     */
    @Transactional
    public void updateUserRole(Long crewId, Long userId, CrewRole newRole, CustomUser principal) {
        // 크루 조회
        Crew crew = findCrewById(crewId);

        // 사용자 조회(권한 변경을 직접 하는 사람 = 크루장)
        User requester = getUserOrThrow(principal);

        // 크루장 권한 검증 - 크루장만 가능
        validateLeaderRole(crew, requester);

        // 권한 변경할 대상 조회
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

        // targetUser가 이 크루의 크루원인지 검증
        CrewUser targetCrewUser = crewUserRepository
                .findByCrewAndUserAndIsDeletedFalse(crew, targetUser)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_CREW_USER));

        CrewRole oldRole = targetCrewUser.getRole(); // 이전 역할 저장

        // 크루장으로 변경하는 경우 -> 기존 크루장을 일반 멤버로 강등
        if (newRole == CrewRole.LEADER) {
            // 일반 멤버는 크루장이 될 수 없음 (부크루장/운영진만 가능)
            if (targetCrewUser.getRole() == CrewRole.MEMBER) {
                throw new BusinessException(ErrorCode.CANNOT_ASSIGN_LEADER_TO_MEMBER);
            }

            // 멤버십 확인
            boolean hasPremium = membershipRepository
                    .existsByUser_IdAndMembershipStatusIn(
                            targetUser.getId(),
                            List.of(MembershipStatus.ACTIVE, MembershipStatus.CANCELED)
                    );

            if (!hasPremium) {
                throw new BusinessException(ErrorCode.LEADER_REQUIRES_PREMIUM);
            }

            // 기존 크루장 찾기
            CrewUser currentLeader = crewUserRepository
                    .findByCrewAndUserAndIsDeletedFalse(crew, requester)
                    .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_CREW_USER));

            // 기존 크루장을 일반 멤버로 변경
            currentLeader.changeRole(CrewRole.MEMBER);

            log.info("크루장 권한 위임 - 기존 크루장 userId: {} → 일반 멤버", requester.getId());
        }

        // 권한 변경
        targetCrewUser.changeRole(newRole);

        // ✅ 채팅 시스템 메시지 전송 (모든 역할 변경에 대해)
        try {
            String roleName = getRoleNameInKorean(newRole);
            crewChatService.sendRoleChangeMessageByCrewId(crewId, targetUser.getName(), roleName);
            log.info("✅ 역할 변경 채팅 시스템 메시지 전송: crewId={}, userId={}, role={}",
                    crewId, userId, roleName);
        } catch (Exception e) {
            log.error("❌ 역할 변경 채팅 메시지 전송 실패: crewId={}, userId={}", crewId, userId, e);
        }

        // ❌ 임시 주석 처리 - 알림 전송 비활성화

        if (newRole == CrewRole.SUB_LEADER || newRole == CrewRole.STAFF) {
            try {
                List<CrewUser> allMembers = crewUserRepository
                        .findByCrewAndIsDeletedFalse(crew);

                String roleName = newRole == CrewRole.SUB_LEADER ? "부크루장" : "운영진";
                String message = targetUser.getName() + "님이 " + roleName + "으로 임명되었습니다.";

                for (CrewUser member : allMembers) {
                    // 권한이 변경된 본인은 제외
                    if (member.getUser().getId().equals(userId)) {
                        continue;
                    }

                    try {
                        notificationService.create(
                                member.getUser(),
                                "크루원 권한 변경",
                                message,
                                NotificationType.CREW,
                                RelatedType.CREW_USERS,
                                crewId
                        );
                        log.debug(
                                "크루원 권한 변경 알림 발송 완료 - crewId: {}, receiverId: {}, targetUserId: {}, newRole: {}",
                                crewId, member.getUser().getId(), userId, newRole);
                    } catch (Exception e) {
                        log.error(
                                "크루원 권한 변경 알림 생성 실패 - crewId: {}, receiverId: {}, targetUserId: {}, newRole: {}",
                                crewId, member.getUser().getId(), userId, newRole, e);
                    }
                }
                log.info(
                        "크루원 전체에게 권한 변경 알림 발송 완료 - crewId: {}, targetUserId: {}, newRole: {}, totalMembers: {}",
                        crewId, userId, newRole, allMembers.size());
            } catch (Exception e) {
                log.error("크루원 전체 알림 발송 중 오류 발생 - crewId: {}, targetUserId: {}, newRole: {}",
                        crewId, userId, newRole, e);
            }
        }
        // ❌ 임시 주석 처리 끝

        log.info("크루원 권한 변경 완료 - crewId: {}, userId: {}, oldRole: {}, newRole: {}",
                crewId, userId, oldRole, newRole);
    }

    /**
     * 공통 메서드
     */

    /**
     * 크루 조회
     */
    private Crew findCrewById(Long crewId) {
        return crewRepository.findById(crewId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.CREW_NOT_FOUND));
    }

    /**
     * 사용자 조회
     */
    private User getUserOrThrow(CustomUser customUser) {
        if (customUser == null || customUser.getUsername() == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        return userRepository.findByLoginId(customUser.getUsername())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * 크루장 또는 부크루장 권한 검증
     */
    private void validateLeaderOrSubLeader(Crew crew, User user) {
        CrewUser crewUser = crewUserRepository.findByCrewAndUserAndIsDeletedFalse(crew, user)
                .orElseThrow(() -> new NotFoundException(ErrorCode.CREW_MEMBER_NOT_FOUND));

        if (!crewUser.getRole().equals(CrewRole.LEADER)
                && !crewUser.getRole().equals(CrewRole.SUB_LEADER)) {
            throw new BusinessException(ErrorCode.NOT_CREW_LEADER_OR_SUB_LEADER);
        }
    }

    /**
     * 크루장 권한 검증 (크루장만 가능)
     */
    private void validateLeaderRole(Crew crew, User user) {
        CrewUser crewUser = crewUserRepository.findByCrewAndUserAndIsDeletedFalse(crew, user)
                .orElseThrow(() -> new NotFoundException(ErrorCode.CREW_MEMBER_NOT_FOUND));

        if (!crewUser.getRole().equals(CrewRole.LEADER)) {
            throw new BusinessException(ErrorCode.NOT_CREW_LEADER);
        }
    }

    /**
     * 1인 1크루 검증 - 다른 크루에 이미 가입했는지 확인
     */
    private void validateNotInAnotherCrew(Long userId) {
        boolean isInAnotherCrew = crewUserRepository
                .existsByUserIdAndIsDeletedFalse(userId);

        if (isInAnotherCrew) {
            throw new BusinessException(ErrorCode.ALREADY_JOINED_CREW);
        }
    }

    /**
     * @description : 해당 사용자의 다른 크루 대기 중인 신청 모두 자동 취소
     */
    private void cancelOtherPendingRequestsForUser(Long userId, Long currentCrewId) {
        // 해당 사용자의 대기 중인 신청 조회
        List<CrewJoinRequest> otherPendingRequests = crewJoinRequestRepository
                .findAllByUserIdAndJoinStatusAndIsDeletedFalse(userId, JoinStatus.PENDING);

        // 현재 크루 제외하고 필터링
        List<CrewJoinRequest> requestsToCancel = otherPendingRequests.stream()
                .filter(request -> !request.getCrew().getId().equals(currentCrewId))
                .collect(Collectors.toList());

        // 취소 처리
        requestsToCancel.forEach(CrewJoinRequest::cancel);

        if (!requestsToCancel.isEmpty()) {
            log.info("다른 크루 대기 신청 자동 취소 완료 - userId: {}, 취소된 신청 수: {}",
                    userId, requestsToCancel.size());
        }
    }

    /**
     * @description : 크루원 권한 검증 (해당 크루의 크루원인지 확인)
     */
    private void validateCrewUser(Crew crew, User user) {
        boolean isCrewUser = crewUserRepository
                .existsByCrewIdAndUserIdAndIsDeletedFalse(crew.getId(), user.getId());

        if (!isCrewUser) {
            throw new ForbiddenException(ErrorCode.NOT_CREW_USER);
        }
    }

    /**
     * ✅ 역할을 한글로 변환
     */
    private String getRoleNameInKorean(CrewRole role) {
        switch (role) {
            case LEADER:
                return "크루장";
            case SUB_LEADER:
                return "부크루장";
            case STAFF:
                return "운영진";
            case MEMBER:
                return "일반 멤버";
            default:
                return role.name();
        }
    }
}
