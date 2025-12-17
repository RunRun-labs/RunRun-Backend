package com.multi.runrunbackend.domain.user.service;

import com.multi.runrunbackend.domain.user.dto.req.AccountUpdateReqDto;
import com.multi.runrunbackend.domain.user.dto.res.AccountResDto;

/**
 *
 * @author : kimyongwon
 * @description : 사용자 계정 관련 비즈니스 로직을 담당한다.
 * @filename : AccountService
 * @since : 25. 12. 17. 오후 11:48 수요일
 */
public interface AccountService {

    AccountResDto getMyAccount();

    void updateAccount(AccountUpdateReqDto dto);
}
