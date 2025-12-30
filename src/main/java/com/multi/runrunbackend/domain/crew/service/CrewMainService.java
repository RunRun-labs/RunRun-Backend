package com.multi.runrunbackend.domain.crew.service;

import com.multi.runrunbackend.common.exception.custom.BusinessException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.crew.constant.CrewRole;
import com.multi.runrunbackend.domain.crew.dto.res.CrewMainResDto;
import com.multi.runrunbackend.domain.crew.entity.Crew;
import com.multi.runrunbackend.domain.crew.entity.CrewUser;
import com.multi.runrunbackend.domain.crew.repository.CrewUserRepository;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CrewMainService {

    private final UserRepository userRepository;
    private final CrewUserRepository crewUserRepository;

    /**
     * @description : 크루 메인 화면 정보 조회
     */
    public CrewMainResDto getCrewMainInfo(CustomUser principal) {
        // 사용자 조회
        User user = getUserOrThrow(principal);

        // 사용자가 가입한 크루 조회 (삭제되지 않은 크루만)
        Optional<CrewUser> crewUserOpt = crewUserRepository
                .findByUserIdAndIsDeletedFalse(user.getId());

        // 크루에 가입하지 않은 경우
        if (crewUserOpt.isEmpty()) {
            return CrewMainResDto.builder()
                    .hasJoinedCrew(false)
                    .build();
        }

        CrewUser crewUser = crewUserOpt.get();
        Crew crew = crewUser.getCrew();

        // 크루 권한 확인
        CrewRole role = crewUser.getRole();
        boolean isLeader = role == CrewRole.LEADER;
        boolean canManage = role == CrewRole.LEADER || role == CrewRole.SUB_LEADER;

        return CrewMainResDto.builder()
                .hasJoinedCrew(true)
                .crewId(crew.getId())
                .crewName(crew.getCrewName())
                .role(role)
                .isLeader(isLeader)
                .canManageJoinRequests(canManage)
                .build();
    }

    /**
     * @description : 사용자 조회
     */
    private User getUserOrThrow(CustomUser customUser) {
        if (customUser == null || customUser.getUsername() == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        return userRepository.findByLoginId(customUser.getUsername())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
