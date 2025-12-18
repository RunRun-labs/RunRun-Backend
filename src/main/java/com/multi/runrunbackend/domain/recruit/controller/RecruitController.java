package com.multi.runrunbackend.domain.recruit.controller;

import com.multi.runrunbackend.common.response.ApiResponse;
import com.multi.runrunbackend.domain.recruit.dto.req.RecruitCreateReqDto;
import com.multi.runrunbackend.domain.recruit.dto.req.RecruitListReqDto;
import com.multi.runrunbackend.domain.recruit.sevice.RecruitService;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author : KIMGWANGHO
 * @description : 러닝 모집글(Recruit) 관련 API 요청(생성, 조회, 수정, 삭제)을 처리하는 컨트롤러 클래스
 * @filename : RecruitController
 * @since : 2025-12-17 수요일
 */
@RestController
@RequestMapping("/api/recruit")
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

    User user = userRepository.findByLoginId(loginId)
        .orElseThrow(() -> new UsernameNotFoundException(
            "유저를 찾을 수 없습니다. ID: " + loginId));

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(ApiResponse.success("모집글 생성 성공", recruitService.createRecruit(user, request)));
  }

  @GetMapping
  public ResponseEntity<ApiResponse> getRecruitList(
      @ModelAttribute RecruitListReqDto request,
      @PageableDefault(size = 10, sort = "created_at", direction = Sort.Direction.DESC) Pageable pageable
  ) {

    return ResponseEntity.ok(
        ApiResponse.success("모집글 목록 조회 성공", recruitService.getRecruitList(request, pageable)));
  }


}