package com.multi.runrunbackend.domain.course.service;

import com.multi.runrunbackend.common.exception.custom.BadRequestException;
import com.multi.runrunbackend.common.exception.custom.BusinessException;
import com.multi.runrunbackend.common.exception.custom.FileUploadException;
import com.multi.runrunbackend.common.exception.custom.ForbiddenException;
import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.common.file.FileDomainType;
import com.multi.runrunbackend.common.file.storage.FileStorage;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.course.constant.CourseStatus;
import com.multi.runrunbackend.domain.course.dto.req.CourseCreateReqDto;
import com.multi.runrunbackend.domain.course.dto.req.CourseListReqDto;
import com.multi.runrunbackend.domain.course.dto.req.CourseSirenReqDto;
import com.multi.runrunbackend.domain.course.dto.req.CourseUpdateReqDto;
import com.multi.runrunbackend.domain.course.dto.req.CursorPage;
import com.multi.runrunbackend.domain.course.dto.req.RouteRequestDto;
import com.multi.runrunbackend.domain.course.dto.res.CourseCreateResDto;
import com.multi.runrunbackend.domain.course.dto.res.CourseDetailResDto;
import com.multi.runrunbackend.domain.course.dto.res.CourseListResDto;
import com.multi.runrunbackend.domain.course.dto.res.CoursePathResDto;
import com.multi.runrunbackend.domain.course.dto.res.CourseUpdateResDto;
import com.multi.runrunbackend.domain.course.dto.res.RouteResDto;
import com.multi.runrunbackend.domain.course.entity.Course;
import com.multi.runrunbackend.domain.course.entity.CourseFavorite;
import com.multi.runrunbackend.domain.course.entity.CourseLike;
import com.multi.runrunbackend.domain.course.entity.CourseSiren;
import com.multi.runrunbackend.domain.course.repository.CourseFavoriteRepository;
import com.multi.runrunbackend.domain.course.repository.CourseLikeRepository;
import com.multi.runrunbackend.domain.course.repository.CourseRepository;
import com.multi.runrunbackend.domain.course.repository.CourseRepositoryCustom;
import com.multi.runrunbackend.domain.course.repository.CourseSirenRepository;
import com.multi.runrunbackend.domain.course.util.GeometryParser;
import com.multi.runrunbackend.domain.course.util.mapbox.MapboxCourseThumbnailGenerator;
import com.multi.runrunbackend.domain.course.util.route.CoursePathProcessor;
import com.multi.runrunbackend.domain.course.util.route.RoutePlanner;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.LineString;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author : kyungsoo
 * @description :
 * @filename : CourseController
 * @since : 2025. 12. 18. Thursday
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CourseService {

  private final WebClient tmapWebClient;
  private final UserRepository userRepository;
  private final CourseRepository courseRepository;
  private final FileStorage s3FileStorage;
  private final CourseRepositoryCustom courseRepositoryCustom;
  private final GeometryParser geometryParser;
  private final CoursePathProcessor pathProcessor;
  private final CourseLikeRepository courseLikeRepository;
  private final CourseFavoriteRepository courseFavoriteRepository;
  private final CourseSirenRepository courseSirenRepository;
  private final MapboxCourseThumbnailGenerator mapboxCourseThumbnailGenerator;
  private final RoutePlanner routePlanner;

  @Transactional
  public CourseCreateResDto createCourse(
      CourseCreateReqDto req,
      MultipartFile imageFile,
      CustomUser principal
  ) {

    User user = getUserOrThrow(principal);

    LineString parsedPath = geometryParser.parseLineString(req.getPath());
    LineString cleanedPath = pathProcessor.simplifyForStore(parsedPath);

    String imageUrl = resolveImageUrl(imageFile, FileDomainType.COURSE_IMAGE, user.getId());

    String thumbnailUrl = mapboxCourseThumbnailGenerator.generateAndUpload(parsedPath,
        user.getId());
    if (thumbnailUrl == null || thumbnailUrl.isBlank()) {
      thumbnailUrl = (imageUrl != null) ? imageUrl : "";
    }

    Course course = Course.create(
        user,
        req,
        cleanedPath,
        imageUrl,
        thumbnailUrl,
        req.getCourseRegisterType()
    );

    Course saved = courseRepository.save(course);
    return CourseCreateResDto.builder()
        .id(saved.getId())
        .build();
  }

  @Transactional
  public CourseUpdateResDto updateCourse(
      CustomUser principal,
      Long courseId,
      CourseUpdateReqDto req,
      MultipartFile imageFile
  ) {
    User user = getUserOrThrow(principal);

    Course course = courseRepository.findById(courseId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.COURSE_NOT_FOUND));

    if (course.getStatus() != CourseStatus.ACTIVE) {
      throw new ForbiddenException(ErrorCode.COURSE_NOT_ACTIVE);
    }
    if (!course.getUser().getId().equals(user.getId())) {
      throw new ForbiddenException(ErrorCode.COURSE_FORBIDDEN);
    }
    String imageUrl = resolveChangedImageUrl(imageFile, FileDomainType.COURSE_IMAGE,
        user.getId(), course);
    LineString cleanedPath = course.getPath();
    String thumbnailUrl = course.getThumbnailUrl();

    boolean hasNewPath = (req.getPath() != null && !req.getPath().isBlank());

    if (hasNewPath) {
      LineString parsedPath = geometryParser.parseLineString(req.getPath());
      cleanedPath = pathProcessor.simplifyForStore(parsedPath);

      thumbnailUrl = mapboxCourseThumbnailGenerator.generateAndUpload(parsedPath,
          user.getId());
    }

    if (thumbnailUrl == null || thumbnailUrl.isBlank()) {
      thumbnailUrl = (imageUrl != null) ? imageUrl : course.getThumbnailUrl();
    }

    course.update(
        user,
        req,
        cleanedPath,
        imageUrl,
        thumbnailUrl,
        req.getCourseRegisterType()
    );

    return CourseUpdateResDto.from(course);
  }

  @Transactional
  public void deleteCourse(CustomUser principal, Long courseId) {
    User user = getUserOrThrow(principal);

    Course course = courseRepository.findById(courseId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.COURSE_NOT_FOUND));

    if (course.getStatus() != CourseStatus.ACTIVE) {
      throw new ForbiddenException(ErrorCode.COURSE_NOT_ACTIVE);
    }
    if (!course.getUser().getId().equals(user.getId())) {
      throw new ForbiddenException(ErrorCode.COURSE_FORBIDDEN);
    }
    course.delete();
  }

  @Transactional(readOnly = true)
  public CourseDetailResDto getCourse(CustomUser principal, Long courseId) {
    User user = getUserOrThrow(principal);

    Course course = courseRepository.findById(courseId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.COURSE_NOT_FOUND));

    boolean isLiked = courseLikeRepository.existsByCourse_IdAndUser_Id(courseId, user.getId());

    boolean isFavorited = courseFavoriteRepository.existsByCourse_IdAndUser_Id(courseId,
        user.getId());
    course.resolveUrl(s3FileStorage.toHttpsUrl(course.getImageUrl()),
        s3FileStorage.toHttpsUrl(course.getThumbnailUrl()));

    return CourseDetailResDto.fromEntity(course, user, isLiked, isFavorited);
  }


  @Transactional(readOnly = true)
  public CoursePathResDto getCoursePath(CustomUser principal, Long courseId) {

    getUserOrThrow(principal);

    Course course = courseRepository.findById(courseId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.COURSE_NOT_FOUND));

    return CoursePathResDto.from(course);
  }

  @Transactional(readOnly = true)
  public CursorPage<CourseListResDto> getCourseList(CustomUser principal, CourseListReqDto req) {
    User user = getUserOrThrow(principal);

    CursorPage<CourseListResDto> page = courseRepositoryCustom.searchCourses(req, user.getId());

    page.setItems(
        page.getItems().stream()
            .map(dto -> {
              dto.resolveThumbnailUrl(
                  s3FileStorage.toHttpsUrl(dto.getThumbnailUrl()));
              return dto;
            })
            .toList()
    );
    return page;

  }

  @Transactional
  public void likeCourse(CustomUser principal, Long courseId) {

    User user = getUserOrThrow(principal);

    Course course = courseRepository.findById(courseId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.COURSE_NOT_FOUND));
    if (course.getStatus() != CourseStatus.ACTIVE) {
      throw new BusinessException(ErrorCode.COURSE_NOT_AVAILABLE);
    }
    if (course.getUser().getId().equals(user.getId())) {
      throw new BusinessException(ErrorCode.CANNOT_LIKE_OWN_COURSE);
    }

    if (courseLikeRepository.existsByCourse_IdAndUser_Id(user.getId(), courseId)) {
      throw new BusinessException(ErrorCode.ALREADY_LIKED_COURSE);
    }
    courseLikeRepository.save(CourseLike.create(user, course));

    courseRepository.increaseLikeCount(courseId);
  }

  @Transactional
  public void unLikeCourse(CustomUser principal, Long courseId) {
    User user = getUserOrThrow(principal);

    Course course = courseRepository.findById(courseId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.COURSE_NOT_FOUND));

    if (course.getStatus() != CourseStatus.ACTIVE) {
      throw new BusinessException(ErrorCode.COURSE_NOT_AVAILABLE);
    }

    int deleted = courseLikeRepository
        .deleteByCourseIdAndUserId(courseId, user.getId());

    if (deleted == 0) {
      throw new BadRequestException(ErrorCode.NOT_LIKED);
    }

    int updated = courseRepository.decreaseLikeCount(courseId);

    if (updated == 0) {
      log.warn("likeCount already zero. courseId={}", courseId);
    }

  }

  @Transactional
  public void favoriteCourse(CustomUser principal, Long courseId) {
    User user = getUserOrThrow(principal);

    Course course = courseRepository.findById(courseId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.COURSE_NOT_FOUND));

    if (course.getStatus() != CourseStatus.ACTIVE) {
      throw new BusinessException(ErrorCode.COURSE_NOT_AVAILABLE);
    }
    if (course.getUser().getId().equals(user.getId())) {
      throw new BusinessException(ErrorCode.CANNOT_FAVORITE_OWN_COURSE);
    }

    if (courseFavoriteRepository.existsByCourse_IdAndUser_Id(user.getId(), courseId)) {
      throw new BusinessException(ErrorCode.ALREADY_FAVORITE_COURSE);
    }
    courseFavoriteRepository.save(CourseFavorite.create(user, course));

    courseRepository.increaseFavoriteCount(courseId);
  }

  @Transactional
  public void unFavoriteCourse(CustomUser principal, Long courseId) {
    User user = getUserOrThrow(principal);

    Course course = courseRepository.findById(courseId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.COURSE_NOT_FOUND));

    if (course.getStatus() != CourseStatus.ACTIVE) {
      throw new BusinessException(ErrorCode.COURSE_NOT_AVAILABLE);
    }

    int deleted = courseFavoriteRepository
        .deleteByCourseIdAndUserId(courseId, user.getId());

    if (deleted == 0) {
      throw new BadRequestException(ErrorCode.NOT_FAVORITE);
    }

    int updated = courseRepository.decreaseFavoriteCount(courseId);

    if (updated == 0) {
      log.warn("favoriteCount already zero. courseId={}", courseId);
    }

  }

  public void sirenCourse(CustomUser principal, Long courseId, CourseSirenReqDto req) {
    User user = getUserOrThrow(principal);

    Course course = courseRepository.findById(courseId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.COURSE_NOT_FOUND));

    if (course.getStatus() != CourseStatus.ACTIVE) {
      throw new BusinessException(ErrorCode.COURSE_NOT_AVAILABLE);
    }

    if (course.getUser().getId().equals(user.getId())) {
      throw new BusinessException(ErrorCode.CANNOT_SIREN_OWN_COURSE);
    }

    if (courseSirenRepository.existsByCourse_IdAndUser_Id(courseId, user.getId())) {
      throw new BusinessException(ErrorCode.ALREADY_SIREN_COURSE);
    }
    courseSirenRepository.save(CourseSiren.create(user, course, req));

  }

  public RouteResDto oneWay(RouteRequestDto req) {
    return routePlanner.oneWay(req);
  }

  public RouteResDto roundTrip(RouteRequestDto req) {
    return routePlanner.roundTrip(req);
  }


  private User getUserOrThrow(CustomUser principal) {
    if (principal == null || principal.getLoginId() == null) {
      throw new NotFoundException(ErrorCode.USER_NOT_FOUND);
    }
    return userRepository.findByLoginId(principal.getLoginId())
        .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
  }

  private String resolveImageUrl(MultipartFile file, FileDomainType domainType, Long refId) {
    if (file == null) {
      throw new FileUploadException(ErrorCode.FILE_REQUIRED);
    }
    if (file.isEmpty()) {
      throw new FileUploadException(ErrorCode.FILE_EMPTY);
    }
    return s3FileStorage.upload(file, domainType, refId);
  }

  private String resolveChangedImageUrl(MultipartFile file, FileDomainType domainType,
      Long refId, Course course) {
    if (file == null || file.isEmpty()) {
      return course.getImageUrl();
    }

    return s3FileStorage.uploadIfChanged(file, domainType, refId, course.getImageUrl());
  }
}
