package com.multi.runrunbackend.domain.crew.service;

import com.multi.runrunbackend.common.exception.custom.BusinessException;
import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.crew.constant.CrewRole;
import com.multi.runrunbackend.domain.crew.constant.JoinStatus;
import com.multi.runrunbackend.domain.crew.dto.req.CrewJoinReqDto;
import com.multi.runrunbackend.domain.crew.dto.res.CrewJoinRequestResDto;
import com.multi.runrunbackend.domain.crew.entity.Crew;
import com.multi.runrunbackend.domain.crew.entity.CrewJoinRequest;
import com.multi.runrunbackend.domain.crew.entity.CrewUser;
import com.multi.runrunbackend.domain.crew.repository.CrewJoinRequestRepository;
import com.multi.runrunbackend.domain.crew.repository.CrewRepository;
import com.multi.runrunbackend.domain.crew.repository.CrewUserRepository;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * @param crewId  가입 신청할 크루 ID
     * @param loginId 가입 신청하는 회원 ID
     * @param reqDto  가입 신청 정보 (자기소개, 거리, 페이스, 지역)
     * @description : 회원이 크루에 가입 신청 (이미 가입했거나 대기중인 경우 예외 발생) TODO 포인트 구현시, 포인트 100P가 차감
     */
    @Transactional
    public void requestJoin(Long crewId, String loginId, CrewJoinReqDto reqDto) {
        //크루 조회
        Crew crew = findCrewById(crewId);

        // 회원 조회
        User user = findUserByLoginId(loginId);

        // 이미 가입한 크루원인지 확인
        boolean alreadyJoined = crewUserRepository.existsByCrewIdAndUserIdAndIsDeletedFalse(crewId, user.getId());
        if (alreadyJoined) {
            throw new BusinessException(ErrorCode.ALREADY_JOINED_CREW);
        }

        // 이미 대기중인 신청이 있는지 확인
        boolean alreadyRequested = crewJoinRequestRepository.existsByCrewAndUserAndJoinStatus(
                crew, user, JoinStatus.PENDING
        );
        if (alreadyRequested) {
            throw new BusinessException(ErrorCode.ALREADY_REQUESTED_JOIN);
        }

        // 가입 신청 생성
        CrewJoinRequest joinRequest = reqDto.toEntity(crew, user);
        crewJoinRequestRepository.save(joinRequest);

        log.info("크루 가입 신청 완료 - crewId: {}, loginId: {}", crewId, loginId);
    }

    /**
     * @param crewId  크루 ID
     * @param loginId 신청 취소하는 회원 ID
     * @description : 신청자가 대기중인 가입 신청을 취소 TODO (포인트 구현시, 차감된 포인트 100P가 환불)
     */
    @Transactional
    public void cancelJoinRequest(Long crewId, String loginId) {
        //크루 조회
        Crew crew = findCrewById(crewId);

        // 회원 조회
        User user = findUserByLoginId(loginId);

        // 대기중인 가입 신청 조회
        CrewJoinRequest joinRequest = crewJoinRequestRepository.findByCrewAndUserAndJoinStatus(
                        crew, user, JoinStatus.PENDING)
                .orElseThrow(() -> new NotFoundException(ErrorCode.JOIN_REQUEST_NOT_FOUND));

        // 가입 신청 취소로 상태 변경
        joinRequest.cancel();

        log.info("크루 가입 신청 취소 완료 - crewId: {}, loginId: {}", crewId, loginId);
    }

    /**
     * @param crewId  크루 ID
     * @param loginId 조회하는 크루장 ID
     * @description : 크루장이 대기중인 가입 신청 목록을 조회 (크루장 또는 부크루장만 조회 가능)
     */
    public List<CrewJoinRequestResDto> getJoinRequestList(Long crewId, String loginId) {
        //크루 조회
        Crew crew = findCrewById(crewId);

        // 회원 조회
        User user = findUserByLoginId(loginId);

        // 크루장 또는 부크루장 권한 확인
        CrewUser crewUser = crewUserRepository.findByCrewAndUserAndIsDeletedFalse(crew, user)
                .orElseThrow(() -> new NotFoundException(ErrorCode.CREW_MEMBER_NOT_FOUND));

        if (!crewUser.getRole().equals(CrewRole.LEADER)
                && !crewUser.getRole().equals(CrewRole.SUB_LEADER)) {
            throw new BusinessException(ErrorCode.NOT_CREW_LEADER_OR_SUB_LEADER);
        }

        // 대기중인 가입 신청 목록 조회
        List<CrewJoinRequest> joinRequests = crewJoinRequestRepository
                .findAllByCrewAndJoinStatusAndIsDeletedFalse(crew, JoinStatus.PENDING);

        // DTO로 변환하여 반환
        return joinRequests.stream()
                .map(CrewJoinRequestResDto::toDto)
                .collect(Collectors.toList());
    }

    /**
     * @param crewId        크루 ID
     * @param loginId       승인하는 크루장 로그인 ID
     * @param joinRequestId 처리할 가입 신청 ID
     * @description : 크루장이 가입 신청을 승인 (승인 시 크루원으로 추가)
     */
    @Transactional
    public void approveJoinRequest(Long crewId, String loginId, Long joinRequestId) {
        //크루 조회
        Crew crew = findCrewById(crewId);

        // 크루장 조회
        User leader = findUserByLoginId(loginId);

        // 크루장 또는 부크루장 권한 확인
        validateLeaderOrSubLeader(crew, leader);

        // 가입 신청 조회
        CrewJoinRequest joinRequest = crewJoinRequestRepository.findByIdAndIsDeletedFalse(joinRequestId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.JOIN_REQUEST_NOT_FOUND));

        // 크루 ID 일치 확인
        if (!joinRequest.getCrew().getId().equals(crewId)) {
            throw new BusinessException(ErrorCode.JOIN_REQUEST_NOT_FOUND);
        }

        // 승인 처리
        joinRequest.approve();

        // 크루원으로 추가
        CrewUser newCrewUser = CrewUser.toEntity(crew, joinRequest.getUser(), CrewRole.MEMBER);
        crewUserRepository.save(newCrewUser);

        log.info("크루 가입 승인 완료 - crewId: {}, newMemberId: {}",
                crewId, joinRequest.getUser().getId());

    }

    /**
     * @param crewId        크루 ID
     * @param loginId       거절하는 크루장 로그인 ID
     * @param joinRequestId 거절할 가입 신청 ID
     * @description : 크루장이 가입 신청을 거절 TODO (포인트 구현 시, 포인트가 환불 예정)
     */
    @Transactional
    public void rejectJoinRequest(Long crewId, String loginId, Long joinRequestId) {
        // 크루 조회
        Crew crew = findCrewById(crewId);

        // 크루장 조회
        User leader = findUserByLoginId(loginId);

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
    private User findUserByLoginId(String loginId) {
        return userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
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

}
