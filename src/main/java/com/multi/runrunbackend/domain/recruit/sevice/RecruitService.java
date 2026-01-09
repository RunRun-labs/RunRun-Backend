package com.multi.runrunbackend.domain.recruit.sevice;

import com.multi.runrunbackend.common.exception.custom.ForbiddenException;
import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.custom.ValidationException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.course.entity.Course;
import com.multi.runrunbackend.domain.course.repository.CourseRepository;
import com.multi.runrunbackend.domain.match.service.MatchSessionService;
import com.multi.runrunbackend.domain.recruit.constant.GenderLimit;
import com.multi.runrunbackend.domain.recruit.constant.RecruitStatus;
import com.multi.runrunbackend.domain.recruit.dto.req.RecruitCreateReqDto;
import com.multi.runrunbackend.domain.recruit.dto.req.RecruitListReqDto;
import com.multi.runrunbackend.domain.recruit.dto.req.RecruitUpdateReqDto;
import com.multi.runrunbackend.domain.recruit.dto.res.RecruitCreateResDto;
import com.multi.runrunbackend.domain.recruit.dto.res.RecruitDetailResDto;
import com.multi.runrunbackend.domain.recruit.dto.res.RecruitListResDto;
import com.multi.runrunbackend.domain.recruit.entity.Recruit;
import com.multi.runrunbackend.domain.recruit.entity.RecruitUser;
import com.multi.runrunbackend.domain.recruit.repository.RecruitRepository;
import com.multi.runrunbackend.domain.recruit.repository.RecruitUserRepository;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
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
    private final MatchSessionService matchSessionService;
    private final CourseRepository courseRepository;


    @Transactional
    public RecruitCreateResDto createRecruit(CustomUser principal, RecruitCreateReqDto request) {

        User user = getUser(principal);

        request.validate();

        if (request.getGenderLimit() != GenderLimit.BOTH) {
            if (!request.getGenderLimit().name().equals(user.getGender())) {
                throw new ValidationException(ErrorCode.GENDER_RESTRICTION);
            }
        }

        int hostAge = calculateAge(user.getBirthDate());
        if (hostAge < request.getAgeMin() || hostAge > request.getAgeMax()) {
            throw new ValidationException(ErrorCode.AGE_RESTRICTION);
        }

        Course course = null;
        if (request.getCourseId() != null) {
            course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.INVALID_REQUEST));
        }

        Recruit recruit = request.toEntity(user, course);  //코스

        recruitRepository.save(recruit);

        return RecruitCreateResDto.from(recruit);
    }

    public Slice<RecruitListResDto> getRecruitList(RecruitListReqDto req, CustomUser principal,
        Pageable pageable) {

        User user = getUser(principal);

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

        LocalDateTime startOfDay = null;
        LocalDateTime endOfDay = null;

        if (req.getDate() != null) {
            startOfDay = req.getDate().atStartOfDay();
            endOfDay = req.getDate().atTime(23, 59, 59);
        }

        Double searchRadius = req.getRadiusKm();

        if (req.getRegion() != null && !req.getRegion().isEmpty()) {
            searchRadius = null;
        }

        Slice<Recruit> recruits = recruitRepository.findRecruitsWithFilters(
            req.getLatitude(),
            req.getLongitude(),
            searchRadius,
            req.getKeyword(),
            startOfDay,
            endOfDay,
            req.getRegion(),
            req.getIsParticipated(),
            user.getId(),
            dynamicPageable
        );

        return recruits.map(recruit -> {
            Double dist = (req.getLatitude() != null && req.getLongitude() != null)
                ? calculateDistance(req.getLatitude(), req.getLongitude(), recruit.getLatitude(),
                recruit.getLongitude())
                : null;
            return RecruitListResDto.from(recruit, dist, user.getId());
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


    public RecruitDetailResDto getRecruitDetail(Long recruitId, CustomUser principal) {
        Recruit recruit = getActiveRecruitOrThrow(recruitId);
        User user = getUser(principal);
        Long userId = user.getId();
        boolean isParticipant = false;
        isParticipant = recruitUserRepository.existsByRecruitAndUser(recruit, user);

        return RecruitDetailResDto.from(recruit, userId, isParticipant);
    }

    @Transactional
    public void updateRecruit(Long recruitId, CustomUser principal, RecruitUpdateReqDto req) {
        User user = getUser(principal);
        Recruit recruit = getActiveRecruitOrThrow(recruitId);

        if (!recruit.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException(ErrorCode.RECRUIT_UPDATE_DENIED);
        }

        if (recruit.getCurrentParticipants() > 1) {
            throw new ForbiddenException(ErrorCode.RECRUIT_HAS_PARTICIPANTS);
        }

        req.validate();

        if (req.getGenderLimit() != null && req.getGenderLimit() != GenderLimit.BOTH) {
            if (!req.getGenderLimit().name().equals(user.getGender())) {
                throw new ValidationException(ErrorCode.GENDER_RESTRICTION);
            }
        }

        int hostAge = calculateAge(user.getBirthDate());
        if (hostAge < req.getAgeMin() || hostAge > req.getAgeMax()) {
            throw new ValidationException(ErrorCode.AGE_RESTRICTION);
        }

        recruit.update(req);
    }

    @Transactional
    public void deleteRecruit(Long recruitId, CustomUser principal) {
        User user = getUser(principal);
        Recruit recruit = getActiveRecruitOrThrow(recruitId);

        if (!recruit.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException(ErrorCode.RECRUIT_DELETE_DENIED);
        }

        if (recruit.getCurrentParticipants() > 1) {
            throw new ForbiddenException(ErrorCode.RECRUIT_HAS_PARTICIPANTS);
        }

        recruitRepository.delete(recruit);
    }

    @Transactional
    public void joinRecruit(Long recruitId, CustomUser principal) {
        User user = getUser(principal);
        Recruit recruit = getActiveRecruitOrThrow(recruitId);

        if (recruitUserRepository.existsByRecruitAndUser(recruit, user)) {
            throw new ValidationException(ErrorCode.ALREADY_PARTICIPATED);
        }

        if (recruit.getCurrentParticipants() >= recruit.getMaxParticipants()) {
            throw new ValidationException(ErrorCode.RECRUIT_FULL);
        }

        if (LocalDateTime.now().plusHours(1).isAfter(recruit.getMeetingAt())) {
            throw new ValidationException(ErrorCode.RECRUIT_TIME_OVER);
        }

        if (recruit.getGenderLimit() != GenderLimit.BOTH) {
            if (!recruit.getGenderLimit().name().equals(user.getGender())) {
                throw new ValidationException(ErrorCode.GENDER_RESTRICTION);
            }
        }

        int userAge = calculateAge(user.getBirthDate());

        if (userAge < recruit.getAgeMin() || userAge > recruit.getAgeMax()) {
            throw new ValidationException(ErrorCode.AGE_RESTRICTION);
        }

        RecruitUser recruitUser = RecruitUser.builder()
            .recruit(recruit)
            .user(user)
            .build();
        recruitUserRepository.save(recruitUser);

        recruit.increaseParticipants();

        if (recruit.getCurrentParticipants().equals(recruit.getMaxParticipants())) {
            matchSessionService.createOfflineSessionBySystem(recruit.getId());
        }
    }

    @Transactional
    public void leaveRecruit(Long recruitId, CustomUser principal) {
        User user = getUser(principal);
        Recruit recruit = getActiveRecruitOrThrow(recruitId);

        RecruitUser recruitUser = recruitUserRepository.findByRecruitAndUser(recruit, user)
            .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_PARTICIPATED));

        if (recruit.getUser().getId().equals(user.getId())) {
            Optional<RecruitUser> nextLeader = recruitUserRepository
                .findFirstByRecruitAndUserNotOrderByCreatedAtAsc(recruit, user);

            if (nextLeader.isPresent()) {
                recruit.changeHost(nextLeader.get().getUser());
            } else {
                recruitRepository.delete(recruit);
            }
        }

        recruitUserRepository.delete(recruitUser);
        recruit.decreaseParticipants();

    }

    private Recruit getActiveRecruitOrThrow(Long recruitId) {
        Recruit recruit = recruitRepository.findById(recruitId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.RECRUIT_NOT_FOUND));

        if (recruit.getStatus() == RecruitStatus.COMPLETED) {
            throw new NotFoundException(ErrorCode.INVALID_RECRUIT);
        }
        return recruit;
    }

    private int calculateAge(java.time.LocalDate birthDate) {
        if (birthDate == null) {
            throw new ValidationException(ErrorCode.BIRTHDATE_REQUIRED);
        }
        return java.time.Period.between(birthDate, java.time.LocalDate.now()).getYears();
    }

    private User getUser(CustomUser principal) {
        return userRepository.findByLoginId(principal.getLoginId())
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
    }

}