package com.multi.runrunbackend.domain.crew.service;

import com.multi.runrunbackend.common.exception.custom.BusinessException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.crew.constant.*;
import com.multi.runrunbackend.domain.crew.dto.req.CrewCreateReqDto;
import com.multi.runrunbackend.domain.crew.dto.req.CrewStatusChangeReqDto;
import com.multi.runrunbackend.domain.crew.dto.req.CrewUpdateReqDto;
import com.multi.runrunbackend.domain.crew.dto.res.*;
import com.multi.runrunbackend.domain.crew.entity.Crew;
import com.multi.runrunbackend.domain.crew.entity.CrewActivity;
import com.multi.runrunbackend.domain.crew.entity.CrewJoinRequest;
import com.multi.runrunbackend.domain.crew.entity.CrewUser;
import com.multi.runrunbackend.domain.crew.repository.CrewActivityRepository;
import com.multi.runrunbackend.domain.crew.repository.CrewJoinRequestRepository;
import com.multi.runrunbackend.domain.crew.repository.CrewRepository;
import com.multi.runrunbackend.domain.crew.repository.CrewUserRepository;
import com.multi.runrunbackend.domain.membership.constant.MembershipStatus;
import com.multi.runrunbackend.domain.membership.repository.MembershipRepository;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author : BoKyung
 * @description : 크루 Service
 * @filename : CrewService
 * @since : 25. 12. 18. 목요일
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CrewService {

    private final CrewRepository crewRepository;
    private final CrewUserRepository crewUserRepository;
    private final CrewActivityRepository crewActivityRepository;
    private final CrewJoinRequestRepository crewJoinRequestRepository;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;

    /**
     * @param reqDto 크루 생성 요청 DTO
     * @description : 크루 생성
     */
    @Transactional
    public Long createCrew(CustomUser principal, CrewCreateReqDto reqDto) {

        // 사용자 조회
        User user = getUserOrThrow(principal);

        // 프리미엄 멤버십인지 검증
        validatePremiumMembership(user);

        // 1인 1크루 생성 제한 검증
        validateNotAlreadyLeader(user.getId());

        // 크루명 중복 확인
        validateCrewNameNotDuplicate(reqDto.getCrewName());

        // 크루 생성
        Crew crew = Crew.create(user, reqDto);
        crewRepository.save(crew);

        // 크루장 자동 등록
        CrewUser crewLeader = CrewUser.create(crew, user, CrewRole.LEADER);
        crewUserRepository.save(crewLeader);

        cancelOtherPendingRequestsForUser(user.getId(), crew.getId());

        return crew.getId();
    }

    /**
     * @param crewId    크루 ID
     * @param principal 사용자 ID
     * @param reqDto    크루 수정 요청 DTO
     * @description : 크루 수정
     */
    @Transactional
    public void updateCrew(Long crewId, CustomUser principal, CrewUpdateReqDto reqDto) {
        // 사용자 조회
        User user = getUserOrThrow(principal);

        // 크루 조회
        Crew crew = findCrewById(crewId);

        // 크루장 권한 검증
        validateCrewLeader(crewId, user.getId());

        // 해체되지 않은 크루인지 검증
        crew.validateNotDisbanded();

        // 크루 정보 수정
        crew.updateCrew(
                reqDto.getCrewDescription(),
                reqDto.getCrewImageUrl(),
                reqDto.getRegion(),
                reqDto.getDistance(),
                reqDto.getAveragePace(),
                reqDto.getActivityTime()
        );
    }

    /**
     * @param crewId    크루 ID
     * @param principal 사용자 로그인 ID
     * @description : 크루 삭제 (해체)
     */
    @Transactional
    public void deleteCrew(Long crewId, CustomUser principal) {
        // 사용자 조회
        User user = getUserOrThrow(principal);

        // 크루 조회
        Crew crew = findCrewById(crewId);

        // 크루장 권한 검증
        validateCrewLeader(crewId, user.getId());

        // 크루 해체 (soft delete)
        crew.softDelete();

        // 모든 크루원 soft delete
        List<CrewUser> crewUsers = crewUserRepository.findAllByCrewIdAndIsDeletedFalse(crewId);
        crewUsers.forEach(CrewUser::delete);
    }

    /**
     * @param cursor      마지막 조회 크루 ID
     * @param size        페이지 크기 -> 5
     * @param distance    거리 필터
     * @param averagePace 평균 페이스 필터
     * @param recruiting  모집중 우선 정렬
     * @param keyword     크루명 검색
     * @description : 크루 목록 조회 (커서 기반 페이징)
     */
    public CrewListPageResDto getCrewList(Long cursor, int size,
                                          String distance, String averagePace, Boolean recruiting, String keyword) {
        Pageable pageable = PageRequest.of(0, size);
        List<Crew> crews;

        boolean hasFilters = (keyword != null && !keyword.isBlank())
                || (distance != null && !distance.isBlank())
                || (averagePace != null && !averagePace.isBlank())
                || (recruiting != null);

        if (hasFilters) {

            crews = crewRepository.findAllWithFilters(
                    cursor, keyword, distance, averagePace, recruiting, CrewStatus.ACTIVE,
                    CrewRecruitStatus.RECRUITING,
                    CrewRecruitStatus.CLOSED, pageable);
        } else {
            // 필터가 없는 경우
            crews = crewRepository.findAllByIdLessThanOrderByIdDesc(cursor, CrewStatus.ACTIVE, pageable);
        }

        List<CrewListResDto> crewListResDtos = crews.stream()
                .map(crew -> {
                    Long memberCount = crewUserRepository.countByCrewIdAndIsDeletedFalse(crew.getId());
                    return CrewListResDto.fromEntity(crew, memberCount);
                })
                .collect(Collectors.toList());

        return CrewListPageResDto.toDtoPage(crewListResDtos, size);
    }

    /**
     * @param crewId 크루 ID
     * @description : 크루 상세 조회
     */
    public CrewDetailResDto getCrewDetail(Long crewId) {
        // 크루 조회
        Crew crew = findCrewById(crewId);

        // 크루원 수 조회
        Long memberCount = crewUserRepository.countByCrewIdAndIsDeletedFalse(crewId);

        // 최근 활동 내역 조회 (최대 5개)
        Pageable pageable = PageRequest.of(0, 5);
        List<CrewActivity> recentActivities = crewActivityRepository
                .findTop5ByCrewIdOrderByCreatedAtDesc(crewId, pageable);

        List<CrewActivityResDto> activityResDtos = recentActivities.stream()
                .map(CrewActivityResDto::fromEntity)
                .collect(Collectors.toList());

        return CrewDetailResDto.fromEntity(crew, memberCount, activityResDtos);
    }

    /**
     * @param principal 사용자 로그인 ID
     * @param crewId    크루 ID
     * @param reqDto    모집 상태 변경 요청 DTO
     * @description : 크루 모집 상태 변경
     */
    @Transactional
    public void updateRecruitStatus(CustomUser principal, Long crewId, CrewStatusChangeReqDto reqDto) {
        // 사용자 조회
        User user = getUserOrThrow(principal);

        // 크루 조회
        Crew crew = findCrewById(crewId);

        // 크루장 권한 검증
        validateCrewLeader(crewId, user.getId());

        // 모집 상태 변경 (해체 여부 검증 포함)
        crew.updateRecruitStatus(reqDto.getRecruitStatus());
    }

/**
 * @description : 메서드 모음
 */

    /**
     * @param customUser 인증된 사용자 정보
     * @description : CustomUser에서 User 엔티티 조회
     */
    private User getUserOrThrow(CustomUser customUser) {
        if (customUser == null || customUser.getUsername() == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        return userRepository.findByLoginId(customUser.getUsername())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * @description : 크루 조회
     */
    private Crew findCrewById(Long crewId) {
        return crewRepository.findById(crewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREW_NOT_FOUND));
    }

    /**
     * @description : 1인 1크루 생성 제한 검증
     */
    private void validateNotAlreadyLeader(Long userId) {
        boolean isAlreadyLeader = crewUserRepository
                .existsByUserIdAndRoleAndIsDeletedFalse(userId, CrewRole.LEADER);

        if (isAlreadyLeader) {
            throw new BusinessException(ErrorCode.ALREADY_CREW_LEADER);
        }
    }

    /**
     * @description : 크루명 중복 확인
     */
    private void validateCrewNameNotDuplicate(String crewName) {
        if (crewRepository.existsByCrewName(crewName)) {
            throw new BusinessException(ErrorCode.CREW_ALREADY_EXISTS);
        }
    }

    /**
     * @description : 크루장 권한 검증
     */
    private void validateCrewLeader(Long crewId, Long userId) {
        boolean isLeader = crewUserRepository
                .existsByCrewIdAndUserIdAndRoleAndIsDeletedFalse(crewId, userId, CrewRole.LEADER);

        if (!isLeader) {
            throw new BusinessException(ErrorCode.NOT_CREW_LEADER);
        }
    }

    /**
     * @param crewId    크루 ID
     * @param principal 사용자 로그인
     * @description : 특정 사용자가 특정 크루에 가입 신청한 상태를 조회 <- 프론트 UI 분기를 위한 상태 값으로 반환
     */
    @Transactional(readOnly = true)
    public CrewAppliedResDto getAppliedStatus(Long crewId, CustomUser principal) {
        // 크루 조회
        Crew crew = findCrewById(crewId);

        // 사용자 조회
        User user = getUserOrThrow(principal);

        // 크루원인지 먼저 확인
        Optional<CrewUser> crewUserOpt = crewUserRepository
                .findByCrewAndUserAndIsDeletedFalse(crew, user);

        if (crewUserOpt.isPresent()) {
            // 크루원이면 무조건 APPROVED!
            return CrewAppliedResDto.builder()
                    .crewJoinState(CrewJoinState.APPROVED)
                    .build();
        }

        // 가입 신청 기록 조회
        Optional<CrewJoinRequest> joinRequestOpt = crewJoinRequestRepository
                .findByCrewIdAndUserId(crewId, user.getId());

        // 신청 기록 없음 → NOT_APPLIED
        if (joinRequestOpt.isEmpty()) {
            return CrewAppliedResDto.builder()
                    .crewJoinState(CrewJoinState.NOT_APPLIED)
                    .build();
        }

        // 신청 기록 있음 → 상태 확인
        JoinStatus status = joinRequestOpt.get().getJoinStatus();

        // PENDING만 대기 상태
        if (status == JoinStatus.PENDING) {
            return CrewAppliedResDto.builder()
                    .crewJoinState(CrewJoinState.PENDING)
                    .build();
        }

        // 나머지 (REJECTED, CANCELED) → 재신청 가능
        return CrewAppliedResDto.builder()
                .crewJoinState(CrewJoinState.CAN_REAPPLY)
                .build();
    }

    /**
     * @param userId        사용자 ID
     * @param currentCrewId 현재 크루 ID (제외할 크루)
     * @description : 해당 사용자의 다른 크루 대기 중인 신청 모두 자동 취소 (크루 생성 시 호출)
     */
    private void cancelOtherPendingRequestsForUser(Long userId, Long currentCrewId) {
        // 해당 사용자의 대기 중인 신청 조회
        List<CrewJoinRequest> otherPendingRequests = crewJoinRequestRepository
                .findAllByUserIdAndJoinStatusAndIsDeletedFalse(userId, JoinStatus.PENDING);

        // 현재 크루 제외하고 모두 취소 처리
        otherPendingRequests.stream()
                .filter(request -> !request.getCrew().getId().equals(currentCrewId))
                .forEach(CrewJoinRequest::cancel);
    }

    /**
     * @description : 멤버십 여부
     */
    private void validatePremiumMembership(User user) {
        // ACTIVE 또는 CANCELED 상태의 멤버십만 확인
        boolean hasValidMembership = membershipRepository
                .existsByUser_IdAndMembershipStatusIn(
                        user.getId(),
                        List.of(MembershipStatus.ACTIVE, MembershipStatus.CANCELED)
                );

        if (!hasValidMembership) {
            throw new BusinessException(ErrorCode.MEMBERSHIP_REQUIRED);
        }
    }
}