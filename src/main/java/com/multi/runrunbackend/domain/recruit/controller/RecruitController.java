package com.multi.runrunbackend.domain.recruit.controller;

import com.multi.runrunbackend.common.response.ApiResponse;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.recruit.dto.req.RecruitCreateReqDto;
import com.multi.runrunbackend.domain.recruit.sevice.RecruitService;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author : KIMGWANGHO
 * @description : Please explain the class!!!
 * @filename : RecruitController
 * @since : 2025-12-17 수요일
 */
@RestController
@RequestMapping("/api/recruits")
@RequiredArgsConstructor
public class RecruitController {

  private final RecruitService recruitService;
  private final UserRepository userRepository;

  @PostMapping
  public ResponseEntity<ApiResponse> createRecruit(
      @AuthenticationPrincipal UserDetails userDetails,
      @Valid @RequestBody RecruitCreateReqDto request
  ) {
    String loginId = userDetails.getUsername();

    if (loginId == null && userDetails instanceof CustomUser) {
      CustomUser customUser = (CustomUser) userDetails;
      loginId = customUser.getEmail();
    }

    String finalLoginId = loginId;
    User user = userRepository.findByLoginId(loginId)
        .orElseThrow(() -> new UsernameNotFoundException(
            "유저를 찾을 수 없습니다. ID: " + finalLoginId));

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(ApiResponse.success("모집글 생성 성공", recruitService.createRecruit(user, request)));
  }
}