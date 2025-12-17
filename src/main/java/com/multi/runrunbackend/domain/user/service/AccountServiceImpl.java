package com.multi.runrunbackend.domain.user.service;

import com.multi.runrunbackend.domain.user.context.UserContext;
import com.multi.runrunbackend.domain.user.dto.req.AccountUpdateReqDto;
import com.multi.runrunbackend.domain.user.dto.res.AccountResDto;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 *
 * @author : kimyongwon
 * @description : Please explain the class!!!
 * @filename : AccountServiceImpl
 * @since : 25. 12. 17. 오후 11:49 수요일
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AccountServiceImpl implements AccountService {

    private final UserRepository userRepository;
    private final UserContext userContext;

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public AccountResDto getMyAccount() {
        User user = getCurrentUser();
        return AccountResDto.from(user);
    }

    @Override
    public void updateAccount(AccountUpdateReqDto dto) {
        User user = getCurrentUser();

        // 이메일 변경 시 중복 체크
        if (!user.getEmail().equals(dto.getEmail())
                && userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalStateException("이미 사용 중인 이메일입니다.");
        }

        user.updateAccount(dto.getEmail(), dto.getName());
    }

    private User getCurrentUser() {
        Long userId = userContext.getUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 사용자입니다."));
    }
}