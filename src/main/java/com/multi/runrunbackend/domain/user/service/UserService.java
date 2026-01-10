package com.multi.runrunbackend.domain.user.service;

import com.multi.runrunbackend.common.exception.custom.*;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.common.file.FileDomainType;
import com.multi.runrunbackend.common.file.storage.FileStorage;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.point.service.PointService;
import com.multi.runrunbackend.domain.user.dto.req.UserUpdateReqDto;
import com.multi.runrunbackend.domain.user.dto.res.AttendanceCheckResDto;
import com.multi.runrunbackend.domain.user.dto.res.AttendanceStatusResDto;
import com.multi.runrunbackend.domain.user.dto.res.UserProfileResDto;
import com.multi.runrunbackend.domain.user.dto.res.UserResDto;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.entity.UserAttendance;
import com.multi.runrunbackend.domain.user.repository.UserAttendanceRepository;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author : kimyongwon
 * @description : 사용자 도메인의 핵심 비즈니스 로직을 담당하는 서비스 클래스. 컨트롤러로부터 전달받은 요청을 도메인 규칙에 맞게 수행한다. 주요 책임: - 로그인
 * 사용자 조회 - 사용자 프로필/계정 정보 수정 - 사용자 존재 여부 검증 및 예외 처리
 * @filename : UserService
 * @since : 25. 12. 18. 오후 4:23 목요일
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserBlockService userBlockService;
    private final FileStorage fileStorage;
    private static final long MAX_PROFILE_IMAGE_SIZE = 1L * 1024 * 1024;
    private final UserAttendanceRepository userAttendanceRepository;
    private final PointService pointService;


    @Transactional(readOnly = true)
    public UserResDto getUser(CustomUser principal) {
        User user = getUserByPrincipal(principal);
        return UserResDto.from(user);
    }

    @Transactional(readOnly = true)
    public UserProfileResDto getUserProfile(
            Long targetUserId,
            CustomUser principal
    ) {

        userBlockService.validateUserBlockStatus(targetUserId, principal);

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

        return UserProfileResDto.from(target);
    }

    @Transactional
    public void updateUser(UserUpdateReqDto req, MultipartFile file, CustomUser principal) {
        User user = getUserByPrincipal(principal);

        updateEmailIfChanged(req, user);
        updateNameIfChanged(req, user);
        updateBodyInfo(req, user);
        updateProfileImage(req, file, user);
    }

    @Transactional
    public void deleteUser(CustomUser principal) {
        User user = getUserByPrincipal(principal);
        if (user.getIsDeleted()) {
            throw new DuplicateException(ErrorCode.USER_ALREADY_DELETED);
        }
        user.deleteAccount();
    }


    private User getUserByPrincipal(CustomUser principal) {
        if (principal == null) {
            throw new TokenException(ErrorCode.UNAUTHORIZED);
        }
        String loginId = principal.getLoginId();

        return userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
    }


    private void updateEmailIfChanged(UserUpdateReqDto req, User user) {
        if (!req.getUserEmail().equals(user.getEmail())) {
            validateDuplicateEmail(req.getUserEmail());
            user.updateAccount(req.getUserEmail(), user.getName());
        }
    }

    private void updateNameIfChanged(UserUpdateReqDto req, User user) {
        if (!req.getUserName().equals(user.getName())) {
            user.updateAccount(user.getEmail(), req.getUserName());
        }
    }

    private void updateBodyInfo(UserUpdateReqDto req, User user) {
        user.updateProfile(req.getHeightCm(), req.getWeightKg());
    }

    private void updateProfileImage(UserUpdateReqDto req, MultipartFile file, User user) {
        String finalUrl = req.getProfileImageUrl();

        if (file != null && !file.isEmpty()) {
            validateProfileImage(file);
            String key = fileStorage.uploadIfChanged(file, FileDomainType.PROFILE, user.getId(), user.getProfileImageUrl());
            finalUrl = fileStorage.toHttpsUrl(key);
        }

        user.updateProfileImage(finalUrl);
    }

    private void validateDuplicateEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateException(ErrorCode.DUPLICATE_EMAIL);
        }
    }

    private void validateProfileImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileUploadException(ErrorCode.FILE_EMPTY);
        }

        if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            throw new FileUploadException(ErrorCode.FILE_NOT_IMAGE);
        }

        if (file.getSize() > MAX_PROFILE_IMAGE_SIZE) {
            throw new FileUploadException(ErrorCode.FILE_SIZE_EXCEEDED);
        }
    }

    // ========================================
    // 출석 이벤트 메서드
    // ========================================

    /**
     * 출석 체크 (하루 1회, 50P 적립)
     */
    @Transactional
    public AttendanceCheckResDto checkAttendance(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

        LocalDate today = LocalDate.now();

        // 이미 오늘 출석했는지 확인
        if (userAttendanceRepository.existsByUserAndAttendanceDate(user, today)) {
            throw new BusinessException(ErrorCode.ALREADY_ATTENDED_TODAY);
        }

        // 출석 기록 저장
        UserAttendance attendance = UserAttendance.create(user, today);
        userAttendanceRepository.save(attendance);

        // 포인트 적립
        pointService.earnPointsForAttendance(userId);

        // 이번 달 출석 일수 조회
        int monthlyCount = userAttendanceRepository.countMonthlyAttendance(
                user,
                today.getYear(),
                today.getMonthValue()
        );

        log.info(" 출석 체크 완료: userId={}, date={}, monthlyCount={}",
                userId, today, monthlyCount);

        return AttendanceCheckResDto.builder()
                .attendanceDate(today)
                .pointsEarned(50)
                .monthlyCount(monthlyCount)
                .message("출석 완료! 50P가 적립되었습니다.")
                .build();
    }

    /**
     * 출석 현황 조회
     */
    @Transactional(readOnly = true)
    public AttendanceStatusResDto getAttendanceStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

        LocalDate today = LocalDate.now();

        // 오늘 출석 여부
        boolean attendedToday = userAttendanceRepository.existsByUserAndAttendanceDate(user, today);

        // 이번 달 출석 일수
        int monthlyCount = userAttendanceRepository.countMonthlyAttendance(
                user,
                today.getYear(),
                today.getMonthValue()
        );

        // 이번 달 출석한 날짜 목록
        List<LocalDate> attendedDates = userAttendanceRepository.findMonthlyAttendanceDates(
                user,
                today.getYear(),
                today.getMonthValue()
        );

        // LocalDate를 day 숫자로 변환
        List<Integer> attendedDays = attendedDates.stream()
                .map(LocalDate::getDayOfMonth)
                .collect(Collectors.toList());

        return AttendanceStatusResDto.builder()
                .attendedToday(attendedToday)
                .monthlyCount(monthlyCount)
                .attendedDays(attendedDays)
                .todayDate(today)
                .currentYear(today.getYear())
                .currentMonth(today.getMonthValue())
                .build();
    }
}