package com.multi.runrunbackend.domain.crew.service;

import com.multi.runrunbackend.common.exception.custom.BusinessException;
import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.crew.constant.JoinStatus;
import com.multi.runrunbackend.domain.crew.dto.req.CrewJoinReqDto;
import com.multi.runrunbackend.domain.crew.entity.Crew;
import com.multi.runrunbackend.domain.crew.entity.CrewJoinRequest;
import com.multi.runrunbackend.domain.crew.repository.CrewJoinRequestRepository;
import com.multi.runrunbackend.domain.crew.repository.CrewRepository;
import com.multi.runrunbackend.domain.crew.repository.CrewUserRepository;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
//    private final UserPointRepository userPointRepository;

    private static final int JOIN_REQUEST_POINT = 100;

    /**
     * @param crewId  가입 신청할 크루 ID
     * @param loginId 가입 신청하는 회원 ID
     * @param reqDto  가입 신청 정보 (자기소개, 거리, 페이스, 지역)
     * @description : 회원이 크루에 가입 신청(포인트 100P가 차감되며, 이미 가입했거나 대기중인 경우 예외 발생)
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

//        // 포인트 조회 및 차감
//        UserPoint userPoint = userPointRepository.findByUser(user)
//                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
//
//        if (userPoint.getTotalPoint() < JOIN_REQUEST_POINT) {
//            throw new BusinessException(ErrorCode.INSUFFICIENT_POINTS);
//        }

//        userPoint.subtractPoint(JOIN_REQUEST_POINT);

        // 가입 신청 생성
        CrewJoinRequest joinRequest = reqDto.toEntity(crew, user);
        crewJoinRequestRepository.save(joinRequest);

        log.info("크루 가입 신청 완료 - crewId: {}, loginId: {}", crewId, loginId);
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

}
