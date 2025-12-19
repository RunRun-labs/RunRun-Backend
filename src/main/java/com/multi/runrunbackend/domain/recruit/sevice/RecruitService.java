package com.multi.runrunbackend.domain.recruit.sevice;

import com.multi.runrunbackend.common.exception.custom.ForbiddenException;
import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.recruit.dto.req.RecruitCreateReqDto;
import com.multi.runrunbackend.domain.recruit.dto.req.RecruitListReqDto;
import com.multi.runrunbackend.domain.recruit.dto.req.RecruitUpdateReqDto;
import com.multi.runrunbackend.domain.recruit.dto.res.RecruitCreateResDto;
import com.multi.runrunbackend.domain.recruit.dto.res.RecruitDetailResDto;
import com.multi.runrunbackend.domain.recruit.dto.res.RecruitListResDto;
import com.multi.runrunbackend.domain.recruit.entity.Recruit;
import com.multi.runrunbackend.domain.recruit.repository.RecruitRepository;
import com.multi.runrunbackend.domain.recruit.repository.RecruitUserRepository;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
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
  private final RecruitUserRepository recruitUserRepository;
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
    String sortBy = (req.getSortBy() != null && !req.getSortBy().isEmpty())
        ? req.getSortBy()
        : "latest";

    Sort sort = switch (sortBy) {
      case "distance" -> JpaSort.unsafe(Sort.Direction.ASC, "distance");
      case "meetingSoon" -> Sort.by(Sort.Direction.ASC, "meeting_at");
      default -> Sort.by(Sort.Direction.DESC, "created_at");
    };

    Pageable dynamicPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
        sort);

    Slice<Recruit> recruits = recruitRepository.findRecruitsWithFilters(
        req.getLatitude(),
        req.getLongitude(),
        req.getRadiusKm(),
        req.getKeyword(),
        dynamicPageable
    );

    return recruits.map(recruit -> {
      Double dist = (req.getLatitude() != null && req.getLongitude() != null)
          ? calculateDistance(req.getLatitude(), req.getLongitude(), recruit.getLatitude(),
          recruit.getLongitude())
          : null;
      return RecruitListResDto.from(recruit, dist);
    });
  }

  private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
    double theta = lon1 - lon2;
    double dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) +
        Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(
            Math.toRadians(theta));
    dist = Math.acos(dist);
    dist = Math.toDegrees(dist);
    dist = dist * 60 * 1.1515 * 1.609344;
    return dist;
  }


  public RecruitDetailResDto getRecruitDetail(Long recruitId, User currentUser) {
    Recruit recruit = recruitRepository.findById(recruitId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.RECRUIT_NOT_FOUND));

    if (recruit.getIsDeleted()) {
      throw new NotFoundException(ErrorCode.INVALID_RECRUIT);
    }

    Long currentUserId = currentUser != null ? currentUser.getId() : null;

    boolean isParticipant = false;
    if (currentUser != null) {
      isParticipant = recruitUserRepository.existsByRecruitAndUser(recruit, currentUser);
    }

    return RecruitDetailResDto.from(recruit, currentUserId, isParticipant);
  }

  @Transactional
  public void updateRecruit(Long recruitId, User user, RecruitUpdateReqDto req) {
    Recruit recruit = recruitRepository.findById(recruitId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.RECRUIT_NOT_FOUND));
    if (!recruit.getUser().getId().equals(user.getId())) {
      throw new ForbiddenException(ErrorCode.RECRUIT_UPDATE_DENIED);
    }

    if (recruit.getCurrentParticipants() > 1) {
      throw new ForbiddenException(ErrorCode.RECRUIT_HAS_PARTICIPANTS);
    }

    recruit.update(req);
  }

  @Transactional
  public void deleteRecruit(Long recruitId, User user) {
    Recruit recruit = recruitRepository.findById(recruitId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.RECRUIT_NOT_FOUND));

    if (!recruit.getUser().getId().equals(user.getId())) {
      throw new ForbiddenException(ErrorCode.RECRUIT_DELETE_DENIED);
    }

    recruit.delete();
  }
}