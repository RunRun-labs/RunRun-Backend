package com.multi.runrunbackend.domain.recruit.controller;

import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.common.response.ApiResponse;
import com.multi.runrunbackend.domain.recruit.dto.req.RecruitCreateReqDto;
import com.multi.runrunbackend.domain.recruit.dto.req.RecruitListReqDto;
import com.multi.runrunbackend.domain.recruit.dto.req.RecruitUpdateReqDto;
import com.multi.runrunbackend.domain.recruit.sevice.RecruitService;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
    User user = getUser(userDetails);
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(ApiResponse.success("모집글 생성 성공", recruitService.createRecruit(user, request)));
  }

  @GetMapping
  public ResponseEntity<ApiResponse> getRecruitList(
      @ModelAttribute RecruitListReqDto request,
      @PageableDefault(size = 10) Pageable pageable
  ) {
    return ResponseEntity.ok(
        ApiResponse.success("모집글 목록 조회 성공", recruitService.getRecruitList(request, pageable)));
  }


  @GetMapping("/{recruitId}")
  public ResponseEntity<ApiResponse> getRecruitDetail(
      @AuthenticationPrincipal UserDetails userDetails,
      @PathVariable Long recruitId
  ) {
    User user = getUser(userDetails);

    return ResponseEntity.ok(
        ApiResponse.success("모집글 상세 조회 성공",
            recruitService.getRecruitDetail(recruitId, user)));
  }

  @PutMapping("/{recruitId}")
  public ResponseEntity<ApiResponse> updateRecruit(
      @AuthenticationPrincipal UserDetails userDetails,
      @PathVariable Long recruitId,
      @Valid @RequestBody RecruitUpdateReqDto request
  ) {
    User user = getUser(userDetails);
    recruitService.updateRecruit(recruitId, user, request);
    return ResponseEntity.ok(ApiResponse.success("모집글 수정 성공", null));
  }


  @DeleteMapping("/{recruitId}")
  public ResponseEntity<ApiResponse> deleteRecruit(
      @AuthenticationPrincipal UserDetails userDetails,
      @PathVariable Long recruitId
  ) {
    User user = getUser(userDetails);
    recruitService.deleteRecruit(recruitId, user);
    return ResponseEntity.ok(ApiResponse.success("모집글 삭제 성공", null));
  }

  private User getUser(UserDetails userDetails) {
    return userRepository.findByLoginId(userDetails.getUsername())
        .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
  }
}