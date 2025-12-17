package com.multi.runrunbackend.domain.crew.service;

import com.multi.runrunbackend.common.exception.custom.BusinessException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.crew.dto.req.CrewCreateReqDto;
import com.multi.runrunbackend.domain.crew.dto.req.CrewUpdateReqDto;
import com.multi.runrunbackend.domain.crew.entity.Crew;
import com.multi.runrunbackend.domain.crew.entity.CrewRole;
import com.multi.runrunbackend.domain.crew.entity.CrewUser;
import com.multi.runrunbackend.domain.crew.repository.CrewActivityRepository;
import com.multi.runrunbackend.domain.crew.repository.CrewRepository;
import com.multi.runrunbackend.domain.crew.repository.CrewUserRepository;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final UserRepository userRepository;
//    private final MembershipRepository membershipRepository;

    /**
     * @param userId 사용자 ID
     * @param reqDto 크루 생성 요청 DTO
     * @description : 크루 생성
     */
    @Transactional
    public Long createCrew(Long userId, CrewCreateReqDto reqDto) {

        // 1. 사용자 조회
        User user = findUserById(userId);

        // 2. 프리미엄 멤버십 검증
//        validatePremiumMembership(userId);

        // 3. 1인 1크루 생성 제한 검증
        validateNotAlreadyLeader(userId);

        // 4. 크루명 중복 확인
        validateCrewNameNotDuplicate(reqDto.getCrewName());

        // 5. 크루 생성
        Crew crew = reqDto.toEntity(user);
        crewRepository.save(crew);

        // 6. 크루장 자동 등록
        CrewUser crewLeader = CrewUser.toEntity(crew, user, CrewRole.LEADER);
        crewUserRepository.save(crewLeader);

        return crew.getId();
    }

    /**
     * @param crewId 크루 ID
     * @param userId 사용자 ID
     * @param reqDto 크루 수정 요청 DTO
     * @description : 크루 수정
     */
    @Transactional
    public void updateCrew(Long crewId, Long userId, CrewUpdateReqDto reqDto) {
        // 1. 크루 조회
        Crew crew = findCrewById(crewId);

        // 2. 크루장 권한 검증
        validateCrewLeader(crewId, userId);

        // 3. 해체되지 않은 크루인지 검증
        crew.validateNotDisbanded();

        // 4. 크루 정보 수정
        crew.updateCrew(
                reqDto.getCrewDescription(),
                reqDto.getCrewImageUrl(),
                reqDto.getRegion(),
                reqDto.getDistance(),
                reqDto.getActivityTime()
        );
    }

/**
 * @description : 메서드 모음
 */

    /**
     * @description : 사용자 조회
     */
    private User findUserById(Long userId) {
        return userRepository.findById(userId)
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
     * @description : 프리미엄 멤버십 검증
     */
//    private void validatePremiumMembership(Long userId) {
//        Membership membership = membershipRepository.findByUserId(userId)
//                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBERSHIP_NOT_FOUND));
//
//        if (!"PREMIUM".equals(membership.getMembershipGrade())) {
//            throw new BusinessException(ErrorCode.NOT_PREMIUM_MEMBER);
//        }
//    }

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
                .existsByCrewIdAndUserIdAndRole(crewId, userId, CrewRole.LEADER);

        if (!isLeader) {
            throw new BusinessException(ErrorCode.NOT_CREW_LEADER);
        }
    }

}
