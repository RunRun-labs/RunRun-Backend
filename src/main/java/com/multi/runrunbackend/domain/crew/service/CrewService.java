package com.multi.runrunbackend.domain.crew.service;

import com.multi.runrunbackend.common.exception.custom.BusinessException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.common.jwt.provider.TokenProvider;
import com.multi.runrunbackend.domain.crew.dto.req.CrewCreateReqDto;
import com.multi.runrunbackend.domain.crew.dto.req.CrewStatusChangeReqDto;
import com.multi.runrunbackend.domain.crew.dto.req.CrewUpdateReqDto;
import com.multi.runrunbackend.domain.crew.dto.res.CrewActivityResDto;
import com.multi.runrunbackend.domain.crew.dto.res.CrewDetailResDto;
import com.multi.runrunbackend.domain.crew.dto.res.CrewListPageResDto;
import com.multi.runrunbackend.domain.crew.dto.res.CrewListResDto;
import com.multi.runrunbackend.domain.crew.entity.Crew;
import com.multi.runrunbackend.domain.crew.entity.CrewActivity;
import com.multi.runrunbackend.domain.crew.entity.CrewRole;
import com.multi.runrunbackend.domain.crew.entity.CrewUser;
import com.multi.runrunbackend.domain.crew.repository.CrewActivityRepository;
import com.multi.runrunbackend.domain.crew.repository.CrewRepository;
import com.multi.runrunbackend.domain.crew.repository.CrewUserRepository;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
    private final UserRepository userRepository;
    private final TokenProvider tokenProvider;

    /**
     * @param reqDto 크루 생성 요청 DTO
     * @description : 크루 생성
     */
    @Transactional
    public Long createCrew(String loginId, CrewCreateReqDto reqDto) {
        // 1. 사용자 조회
        User user = findUserByLoginId(loginId);

        // 2. 프리미엄 멤버십인지 검증
//       validatePremiumMembership(user.get);

        // 3. 1인 1크루 생성 제한 검증
        validateNotAlreadyLeader(user.getId());

        // 4. 크루명 중복 확인
        validateCrewNameNotDuplicate(reqDto.getCrewName());

        // 5. 이미지 URL이 없을 경우 기본값 설정
        String imageUrl = reqDto.getCrewImageUrl();
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            // 기본 이미지 URL 설정 (SVG 데이터 URI 사용)
            // 회색 배경에 "Crew Image" 텍스트가 있는 간단한 SVG
            imageUrl = "data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgZmlsbD0iI0Y1RjVGNSIvPjx0ZXh0IHg9IjUwJSIgeT0iNTAlIiBmb250LWZhbWlseT0iQXJpYWwiIGZvbnQtc2l6ZT0iMjAiIGZpbGw9IiM5OTk5OTkiIHRleHQtYW5jaG9yPSJtaWRkbGUiIGR5PSIuM2VtIj5DcmV3IEltYWdlPC90ZXh0Pjwvc3ZnPg==";
        }

        // 6. 크루 생성 (이미지 URL 포함)
        CrewCreateReqDto reqDtoWithImage = CrewCreateReqDto.builder()
                .crewName(reqDto.getCrewName())
                .crewDescription(reqDto.getCrewDescription())
                .crewImageUrl(imageUrl)
                .region(reqDto.getRegion())
                .distance(reqDto.getDistance())
                .averagePace(reqDto.getAveragePace())
                .activityTime(reqDto.getActivityTime())
                .build();
        Crew crew = reqDtoWithImage.toEntity(user);
        crewRepository.save(crew);

        // 6. 크루장 자동 등록
        CrewUser crewLeader = CrewUser.toEntity(crew, user, CrewRole.LEADER);
        crewUserRepository.save(crewLeader);

        return crew.getId();
    }

    /**
     * @param crewId  크루 ID
     * @param loginId 사용자 ID
     * @param reqDto  크루 수정 요청 DTO
     * @description : 크루 수정
     */
    @Transactional
    public void updateCrew(Long crewId, String loginId, CrewUpdateReqDto reqDto) {
        // 1. 사용자 조회
        User user = findUserByLoginId(loginId);

        // 2. 크루 조회
        Crew crew = findCrewById(crewId);

        // 3. 크루장 권한 검증
        validateCrewLeader(crewId, user.getId());

        // 4. 해체되지 않은 크루인지 검증
        crew.validateNotDisbanded();

        // 5. 크루 정보 수정
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
     * @param crewId  크루 ID
     * @param loginId 사용자 로그인 ID (이메일)
     * @description : 크루 삭제 (해체)
     */
    @Transactional
    public void deleteCrew(Long crewId, String loginId) {
        // 1. 사용자 조회
        User user = findUserByLoginId(loginId);

        // 2. 크루 조회
        Crew crew = findCrewById(crewId);

        // 3. 크루장 권한 검증
        validateCrewLeader(crewId, user.getId());

        // 4. 크루 해체 (soft delete)
        crew.softDelete();

        // 5. 모든 크루원 soft delete
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

        // 여러 필터를 조합할 수 있도록 동적 쿼리 사용
        // keyword, region, distance를 모두 조합 가능
        boolean hasFilters = (keyword != null && !keyword.isBlank())
                || (distance != null && !distance.isBlank())
                || (averagePace != null && !averagePace.isBlank())
                || (recruiting != null);

        if (hasFilters) {
            // 일반 필터 쿼리 사용
            crews = crewRepository.findAllWithFilters(
                    cursor, keyword, distance, averagePace, recruiting, pageable);
        } else {
            // 필터가 없는 경우
            crews = crewRepository.findAllByIdLessThanOrderByIdDesc(cursor, pageable);
        }

        List<CrewListResDto> crewListResDtos = crews.stream()
                .map(crew -> {
                    Long memberCount = crewUserRepository.countByCrewIdAndIsDeletedFalse(crew.getId());
                    return CrewListResDto.toDto(crew, memberCount);
                })
                .collect(Collectors.toList());

        return CrewListPageResDto.toDtoPage(crewListResDtos, size);
    }

    /**
     * @param crewId 크루 ID
     * @description : 크루 상세 조회
     */
    public CrewDetailResDto getCrewDetail(Long crewId) {
        // 1. 크루 조회
        Crew crew = findCrewById(crewId);

        // 2. 크루원 수 조회
        Long memberCount = crewUserRepository.countByCrewIdAndIsDeletedFalse(crewId);

        // 3. 최근 활동 내역 조회 (최대 5개)
        Pageable pageable = PageRequest.of(0, 5);
        List<CrewActivity> recentActivities = crewActivityRepository
                .findTop5ByCrewIdOrderByCreatedAtDesc(crewId, pageable);

        List<CrewActivityResDto> activityResDtos = recentActivities.stream()
                .map(CrewActivityResDto::toEntity)
                .collect(Collectors.toList());

        return CrewDetailResDto.toEntity(crew, memberCount, activityResDtos);
    }

    /**
     * @param loginId 사용자 로그인 ID (이메일)
     * @param crewId  크루 ID
     * @param reqDto  모집 상태 변경 요청 DTO
     * @description : 크루 모집 상태 변경
     */
    @Transactional
    public void updateRecruitStatus(String loginId, Long crewId, CrewStatusChangeReqDto reqDto) {
        // 1. 사용자 조회
        User user = findUserByLoginId(loginId);

        // 2. 크루 조회
        Crew crew = findCrewById(crewId);

        // 3. 크루장 권한 검증
        validateCrewLeader(crewId, user.getId());

        // 4. 모집 상태 변경 (해체 여부 검증 포함)
        crew.updateRecruitStatus(reqDto.getRecruitStatus());
    }

/**
 * @description : 메서드 모음
 */

    /**
     * @description : 사용자 조회
     */
    private User findUserByLoginId(String loginId) {
        return userRepository.findByLoginId(loginId)
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
                .existsByCrewIdAndUserIdAndRole(crewId, userId, CrewRole.LEADER);

        if (!isLeader) {
            throw new BusinessException(ErrorCode.NOT_CREW_LEADER);
        }
    }

}
