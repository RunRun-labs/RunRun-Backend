package com.multi.runrunbackend.domain.user.controller;

import com.multi.runrunbackend.domain.user.dto.req.AccountUpdateReqDto;
import com.multi.runrunbackend.domain.user.dto.res.AccountResDto;
import com.multi.runrunbackend.domain.user.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author : kimyongwon
 * @description : 사용자 계정 정보 조회 및 수정 API를 제공한다.
 * @filename : AccountController
 * @since : 25. 12. 17. 오후 11:50 수요일
 */
@RestController
@RequestMapping("/api/v1/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    /**
     * 계정 정보 조회
     */
    @GetMapping("/me")
    public ResponseEntity<AccountResDto> getMyAccount() {
        return ResponseEntity.ok(accountService.getMyAccount());
    }

    /**
     * 계정 정보 수정 (이메일/이름)
     */
    @PutMapping("/me")
    public ResponseEntity<Void> updateAccount(
            @RequestBody AccountUpdateReqDto dto
    ) {
        accountService.updateAccount(dto);
        return ResponseEntity.ok().build();
    }
}