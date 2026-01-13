package com.multi.runrunbackend.domain.match.service;

import com.multi.runrunbackend.common.exception.custom.ForbiddenException;
import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.custom.TokenException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.common.file.storage.FileStorage;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.friend.entity.Friend;
import com.multi.runrunbackend.domain.friend.repository.FriendRepository;
import com.multi.runrunbackend.domain.match.constant.RunStatus;
import com.multi.runrunbackend.domain.match.constant.RunningType;
import com.multi.runrunbackend.domain.match.dto.res.ProfileRunningHistoryResDto;
import com.multi.runrunbackend.domain.match.entity.RunningResult;
import com.multi.runrunbackend.domain.match.repository.BattleResultRepository;
import com.multi.runrunbackend.domain.match.repository.RunningResultRepository;
import com.multi.runrunbackend.domain.user.constant.ProfileVisibility;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.entity.UserSetting;
import com.multi.runrunbackend.domain.user.repository.UserBlockRepository;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import com.multi.runrunbackend.domain.user.repository.UserSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 * @author : kimyongwon
 * @description : 프로필 러닝 기록 조회 Service
 * @filename : ProfileRunningHistoryService
 * @since : 26. 1. 4. 오후 7:29 일요일
 */
@Service
@RequiredArgsConstructor
public class ProfileRunningHistoryService {

    private final RunningResultRepository runningResultRepository;
    private final UserRepository userRepository;
    private final UserBlockRepository userBlockRepository;
    private final UserSettingRepository userSettingRepository;
    private final FriendRepository friendRepository;
    private final FileStorage fileStorage;
    private final BattleResultRepository battleResultRepository;

    private static final List<RunStatus> VISIBLE_STATUSES = List.of(RunStatus.COMPLETED, RunStatus.TIME_OUT);

    /**
     * 내 러닝 기록 조회
     */
    @Transactional(readOnly = true)
    public Slice<ProfileRunningHistoryResDto> getMyRunningRecords(
            CustomUser principal,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    ) {
        User me = getUserByPrincipal(principal);

        java.time.LocalDateTime start = (startDate != null) ? startDate.atStartOfDay() : null;
        java.time.LocalDateTime end = (endDate != null) ? endDate.atTime(java.time.LocalTime.MAX) : null;

        Slice<RunningResult> slice = (start != null || end != null)
                ? runningResultRepository.findMyRecordsByStatuses(
                me.getId(),
                VISIBLE_STATUSES,
                null,
                null,
                start,
                end,
                pageable
        )
                : runningResultRepository.findByUserAndRunStatusInAndIsDeletedFalse(
                me,
                VISIBLE_STATUSES,
                pageable
        );

        return mapSliceWithOnlineBattleRanking(slice);
    }

    /**
     * 타 사용자 러닝 기록 조회
     */
    @Transactional(readOnly = true)
    public Slice<ProfileRunningHistoryResDto> getUserRunningRecords(
            Long userId,
            CustomUser principal,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    ) {
        User me = getUserByPrincipal(principal);

        User target = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));


        validateProfileAccess(me, target);

        java.time.LocalDateTime start = (startDate != null) ? startDate.atStartOfDay() : null;
        java.time.LocalDateTime end = (endDate != null) ? endDate.atTime(java.time.LocalTime.MAX) : null;

        Slice<RunningResult> slice = (start != null || end != null)
                ? runningResultRepository.findMyRecordsByStatuses(
                target.getId(),
                VISIBLE_STATUSES,
                null,
                null,
                start,
                end,
                pageable
        )
                : runningResultRepository.findByUserAndRunStatusInAndIsDeletedFalse(
                target,
                VISIBLE_STATUSES,
                pageable
        );

        return mapSliceWithOnlineBattleRanking(slice);
    }

    /**
     * 공유되지 않은 러닝 기록 조회 (피드 공유용)
     */
    @Transactional(readOnly = true)
    public Slice<ProfileRunningHistoryResDto> getUnsharedRunningRecords(
            CustomUser principal,
            Pageable pageable
    ) {
        User me = getUserByPrincipal(principal);

        return runningResultRepository
                .findUnsharedCompletedRecords(me, RunStatus.COMPLETED, pageable)
                .map(r -> ProfileRunningHistoryResDto.from(r, fileStorage));
    }

    /**
     * 러닝 기록 상세 조회 (단일)
     */
    @Transactional(readOnly = true)
    public ProfileRunningHistoryResDto getRunningRecordDetail(
            Long recordId,
            CustomUser principal
    ) {
        User me = getUserByPrincipal(principal);

        RunningResult result = runningResultRepository.findByIdAndIsDeletedFalse(recordId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.RUNNING_RESULT_NOT_FOUND));


        if (!result.getUser().getId().equals(me.getId())) {
            throw new ForbiddenException(ErrorCode.RUNNING_RESULT_FORBIDDEN);
        }

        // 온라인 배틀인 경우 랭킹도 조회
        Integer ranking = null;
        if (result.getRunningType() == RunningType.ONLINEBATTLE) {
            List<Object[]> rankingResults = battleResultRepository.findRankingByRunningResultIds(Set.of(recordId));
            if (!rankingResults.isEmpty()) {
                ranking = (Integer) rankingResults.get(0)[1];
            }
        }

        return ProfileRunningHistoryResDto.from(result, fileStorage, ranking);
    }

    /**
     * 러닝 기록 삭제
     */
    @Transactional
    public void deleteRunningRecord(Long recordId, CustomUser principal) {
        User user = getUserByPrincipal(principal);

        RunningResult result = runningResultRepository.findByIdAndIsDeletedFalse(recordId)
                .orElseThrow(() ->
                        new NotFoundException(ErrorCode.RUNNING_RESULT_NOT_FOUND)
                );


        if (!result.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException(ErrorCode.RUNNING_RESULT_FORBIDDEN);
        }

        result.delete(); // soft delete
    }

    /**
     * 프로필 접근 권한 검증
     */
    private void validateProfileAccess(User me, User target) {

        if (me.getId().equals(target.getId())) {
            return;
        }


        if (userBlockRepository.existsByBlockerAndBlockedUser(me, target)
                || userBlockRepository.existsByBlockerAndBlockedUser(target, me)) {
            throw new ForbiddenException(ErrorCode.USER_BLOCKED);
        }

        UserSetting setting =
                userSettingRepository.findByUserId(target.getId())
                        .orElse(UserSetting.createDefault(target));

        ProfileVisibility visibility = setting.getProfileVisibility();

        switch (visibility) {
            case PUBLIC -> {

            }

            case FRIENDS_ONLY -> {
                boolean isFriend =
                        friendRepository.findBetweenUsers(me, target)
                                .filter(Friend::isAccepted)
                                .isPresent();

                if (!isFriend) {
                    throw new ForbiddenException(ErrorCode.PROFILE_FRIENDS_ONLY);
                }
            }

            case PRIVATE -> {
                throw new ForbiddenException(ErrorCode.PROFILE_PRIVATE);
            }
        }
    }

    private User getUserByPrincipal(CustomUser principal) {
        if (principal == null) {
            throw new TokenException(ErrorCode.UNAUTHORIZED);
        }

        return userRepository.findByLoginId(principal.getLoginId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
    }

    private Slice<ProfileRunningHistoryResDto> mapSliceWithOnlineBattleRanking(Slice<RunningResult> slice) {
        // ONLINEBATTLE 기록들만 runningResultId 수집
        Set<Long> onlineBattleIds = slice.getContent().stream()
                .filter(r -> r.getRunningType() == RunningType.ONLINEBATTLE)
                .map(RunningResult::getId)
                .collect(Collectors.toSet());

        System.out.println("[DEBUG] 온라인 배틀 기록 ID 목록: " + onlineBattleIds);

        Map<Long, Integer> rankingByRunningResultId = Map.of();
        if (!onlineBattleIds.isEmpty()) {
            List<Object[]> rankingResults = battleResultRepository.findRankingByRunningResultIds(onlineBattleIds);
            System.out.println("[DEBUG] BattleResult에서 조회된 랭킹 결과 개수: " + rankingResults.size());
            
            rankingByRunningResultId = rankingResults.stream()
                    .collect(Collectors.toMap(
                            row -> (Long) row[0],
                            row -> (Integer) row[1],
                            (a, b) -> a
                    ));
            
            System.out.println("[DEBUG] 최종 랭킹 맵: " + rankingByRunningResultId);
        }

        final Map<Long, Integer> finalRankingMap = rankingByRunningResultId;

        return slice.map(r -> {
            Integer rank = (r.getRunningType() == RunningType.ONLINEBATTLE)
                    ? finalRankingMap.get(r.getId())
                    : null;

            if (r.getRunningType() == RunningType.ONLINEBATTLE) {
                System.out.println("[DEBUG] RunningResult ID: " + r.getId() + ", 조회된 랭킹: " + rank);
            }

            return ProfileRunningHistoryResDto.from(r, fileStorage, rank);
        });
    }
}