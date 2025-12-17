package com.multi.runrunbackend.domain.recruit.sevice;

import com.multi.runrunbackend.domain.recruit.dto.req.RecruitCreateReqDto;
import com.multi.runrunbackend.domain.recruit.dto.res.RecruitCreateResDto;
import com.multi.runrunbackend.domain.recruit.entity.Recruit;
import com.multi.runrunbackend.domain.recruit.repository.RecruitRepository;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author : KIMGWANGHO
 * @description : Please explain the class!!!
 * @filename : RecruitService
 * @since : 2025-12-17 수요일
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecruitService {

  private final RecruitRepository recruitRepository;
  private final UserRepository userRepository;
//  private final CourseRepository courseRepository;


  @Transactional
  public RecruitCreateResDto createRecruit(User user, RecruitCreateReqDto request) {
    request.validate();

//    Course course = null;
//    if (request.getCourseId() != null) {
//      course = courseRepository.findById(request.getCourseId())
//          .orElseThrow(() -> new NotFoundException(ErrorCode.INVALID_REQUEST));
//    }

    Recruit recruit = request.toEntity(user, null);  //코스

    recruitRepository.save(recruit);

    return RecruitCreateResDto.from(recruit);
  }
}