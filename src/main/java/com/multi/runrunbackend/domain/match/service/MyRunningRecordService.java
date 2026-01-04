package com.multi.runrunbackend.domain.match.service;

import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.custom.TokenException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.match.constant.RunStatus;
import com.multi.runrunbackend.domain.match.dto.res.MyRunningRecordResDto;
import com.multi.runrunbackend.domain.match.repository.RunningResultRepository;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author : kimyongwon
 * @description : 마이페이지 - 내 러닝 기록 조회 Service
 * @filename : MyRunningRecordService
 * @since : 26. 1. 4. 오후 7:29 일요일
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyRunningRecordService {

    private final RunningResultRepository runningResultRepository;
    private final UserRepository userRepository;

    public Slice<MyRunningRecordResDto> getMyRunningRecords(
            CustomUser principal,
            Pageable pageable
    ) {
        User user = getUserByPrincipal(principal);

        return runningResultRepository
                .findByUserIdAndRunStatus(
                        user.getId(),
                        RunStatus.COMPLETED,
                        pageable
                )
                .map(MyRunningRecordResDto::from);
    }

    private User getUserByPrincipal(CustomUser principal) {
        if (principal == null) {
            throw new TokenException(ErrorCode.UNAUTHORIZED);
        }

        return userRepository.findByLoginId(principal.getLoginId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
    }
}