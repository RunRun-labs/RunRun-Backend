package com.multi.runrunbackend.domain.recruit.sevice;

import com.multi.runrunbackend.domain.recruit.dto.req.RecruitCreateReqDto;
import com.multi.runrunbackend.domain.recruit.dto.req.RecruitListReqDto;
import com.multi.runrunbackend.domain.recruit.dto.res.RecruitCreateResDto;
import com.multi.runrunbackend.domain.recruit.dto.res.RecruitListResDto;
import com.multi.runrunbackend.domain.recruit.entity.Recruit;
import com.multi.runrunbackend.domain.recruit.repository.RecruitRepository;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author : KIMGWANGHO
 * @description : 러닝 모집글(Recruit) 생성 및 관리에 필요한 비즈니스 로직을 수행하는 서비스 클래스
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

  public Slice<RecruitListResDto> getRecruitList(RecruitListReqDto req, Pageable pageable) {

    Slice<Recruit> recruits = recruitRepository.findRecruitsByRadius(
        req.getLatitude(),
        req.getLongitude(),
        req.getRadiusKm(),
        pageable
    );

    return recruits.map(recruit -> {
      Double distance = null;
      if (req.getLatitude() != null && req.getLongitude() != null) {
        distance = calculateDistance(
            req.getLatitude(), req.getLongitude(),
            recruit.getLatitude(), recruit.getLongitude()
        );
      }
      return RecruitListResDto.from(recruit, distance);
    });
  }


  private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
    double theta = lon1 - lon2;
    double dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) +
        Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(
            Math.toRadians(theta));
    dist = Math.acos(dist);
    dist = Math.toDegrees(dist);
    dist = dist * 60 * 1.1515 * 1.609344; // Mile -> km 변환
    return dist;
  }
}