package com.multi.runrunbackend.domain.challenge.service;

import com.multi.runrunbackend.common.exception.custom.*;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.common.file.FileDomainType;
import com.multi.runrunbackend.common.file.storage.FileStorage;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.challenge.constant.UserChallengeStatus;
import com.multi.runrunbackend.domain.challenge.dto.req.ChallengeReqDto;
import com.multi.runrunbackend.domain.challenge.dto.res.ChallengeResDto;
import com.multi.runrunbackend.domain.challenge.entity.Challenge;
import com.multi.runrunbackend.domain.challenge.entity.UserChallenge;
import com.multi.runrunbackend.domain.challenge.repository.ChallengeRepository;
import com.multi.runrunbackend.domain.challenge.repository.UserChallengeRepository;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 * @author : kimyongwon
 * @description : 챌린지 도메인의 핵심 비즈니스 로직을 담당하는 서비스 클래스.
 * 챌린지 생성,수정, 삭제, 목록 조회, 사용자별 참여 상태 매핑을 처리하며,
 * 사용자 인증 정보(CustomUser)를 기반으로
 * 챌린지와 사용자(UserChallenge) 간의 관계를 조합한다.
 * 또한 챌린지 이미지 업로드 및 파일 검증 로직을 포함하여
 * 챌린지 생성 시 필요한 부가 처리를 책임진다.
 * @filename : ChallengeService
 * @since : 25. 12. 21. 오후 9:25 일요일
 */
@Service
@RequiredArgsConstructor
public class ChallengeService {

    private final ChallengeRepository challengeRepository;
    private final UserChallengeRepository userChallengeRepository;
    private final UserRepository userRepository;
    private final FileStorage fileStorage;

    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024; // 5MB

    @Transactional
    public ChallengeResDto createChallenge(ChallengeReqDto req, MultipartFile imageFile, CustomUser principal) {
        validateAdminRole(principal);

        User user = getUserByPrincipal(principal);

        Challenge savedChallenge = saveChallenge(req);

        uploadImageIfPresent(imageFile, savedChallenge);

        return ChallengeResDto.from(savedChallenge);
    }

    @Transactional
    public void updateChallenge(Long challengeId, ChallengeReqDto req, MultipartFile imageFile, CustomUser principal) {
        validateAdminRole(principal);

        User user = getUserByPrincipal(principal);
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.INVALID_REQUEST));


        long participantCount = userChallengeRepository.countByChallengeId(challengeId);
        boolean isStarted = !challenge.getStartDate().isAfter(LocalDate.now()); // 시작일 <= 오늘

        if (participantCount > 0 || isStarted) {

            if (!challenge.getTargetValue().equals(req.getTargetValue())) {
                throw new InvalidRequestException(ErrorCode.CHALLENGE_CANNOT_UPDATE);
            }

            if (challenge.getChallengeType() != req.getChallengeType()) {
                throw new InvalidRequestException(ErrorCode.CHALLENGE_CANNOT_UPDATE);
            }

            if (!challenge.getStartDate().equals(req.getStartDate()) ||
                    !challenge.getEndDate().equals(req.getEndDate())) {
                throw new InvalidRequestException(ErrorCode.CHALLENGE_CANNOT_UPDATE);
            }
            // 제목이나 설명은 수정 허용
        }

        challenge.update(
                req.getTitle(),
                req.getChallengeType(),
                req.getTargetValue(),
                req.getDescription(),
                req.getStartDate(),
                req.getEndDate()
        );

        if (imageFile != null && !imageFile.isEmpty()) {
            validateFile(imageFile);
            String key = fileStorage.uploadIfChanged(
                    imageFile,
                    FileDomainType.CHALLENGE_IMAGE,
                    challenge.getId(),
                    challenge.getImageUrl()
            );

            String imageUrl = fileStorage.toHttpsUrl(key);
            challenge.updateImageUrl(imageUrl);
        }
    }

    @Transactional
    public void deleteChallenge(Long challengeId, CustomUser principal) {
        validateAdminRole(principal);

        User user = getUserByPrincipal(principal);

        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.INVALID_REQUEST));


        LocalDate today = LocalDate.now();
        boolean isInProgress = !challenge.getStartDate().isAfter(today) && !challenge.getEndDate().isBefore(today);

        if (isInProgress) {
            throw new InvalidRequestException(ErrorCode.CHALLENGE_CANNOT_DELETE);
        }

        long participantCount = userChallengeRepository.countByChallengeId(challengeId);
        if (participantCount > 0) {
            throw new InvalidRequestException(ErrorCode.CHALLENGE_CANNOT_DELETE);
        }

        challenge.deleteChallenge();
    }

    @Transactional(readOnly = true)
    public List<ChallengeResDto> getChallengeList(CustomUser principal) {
        List<Challenge> challenges = challengeRepository.findAll();
        List<ChallengeResDto> resDtos = challenges.stream()
                .map(ChallengeResDto::from)
                .collect(Collectors.toList());

        resDtos.forEach(dto -> {
            long count = userChallengeRepository.countByChallengeId(dto.getId());
            dto.setParticipantCount(count);
        });

        if (principal != null) {
            User user = getUserByPrincipal(principal);
            mapUserChallengeStatus(user.getId(), resDtos);
        }

        return resDtos;
    }

    @Transactional(readOnly = true)
    public ChallengeResDto getChallengeDetail(Long challengeId, CustomUser principal) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.INVALID_REQUEST));

        ChallengeResDto resDto = ChallengeResDto.from(challenge);

        long count = userChallengeRepository.countByChallengeId(challengeId);
        resDto.setParticipantCount(count);

        if (principal != null) {
            User user = getUserByPrincipal(principal);
            userChallengeRepository.findByUserId(user.getId()).stream()
                    .filter(uc -> uc.getChallenge().getId().equals(challengeId))
                    .findFirst()
                    .ifPresent(uc -> {
                        resDto.setMyStatus(uc.getStatus());
                        resDto.setProgressValue(uc.getProgressValue()); // 진행도 설정
                    });
        }

        return resDto;
    }

    @Transactional
    public void joinChallenge(Long challengeId, CustomUser principal) {
        User user = getUserByPrincipal(principal);
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.INVALID_REQUEST));

        // 중복 참여 체크
        boolean alreadyJoined = userChallengeRepository.findByUserId(user.getId()).stream()
                .anyMatch(uc -> uc.getChallenge().getId().equals(challengeId)
                        && (uc.getStatus() == UserChallengeStatus.JOINED
                        || uc.getStatus() == UserChallengeStatus.IN_PROGRESS));

        if (alreadyJoined) {
            throw new BusinessException(ErrorCode.ALREADY_JOINED) {
            };
        }

        UserChallenge userChallenge = UserChallenge.join(user, challenge);
        userChallengeRepository.save(userChallenge);
    }

    @Transactional
    public void cancelChallenge(Long challengeId, CustomUser principal) {
        User user = getUserByPrincipal(principal);

        UserChallenge userChallenge = userChallengeRepository.findByUserId(user.getId()).stream()
                .filter(uc -> uc.getChallenge().getId().equals(challengeId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(ErrorCode.INVALID_REQUEST));

        userChallengeRepository.delete(userChallenge);
    }


    private User getUserByPrincipal(CustomUser principal) {
        if (principal == null) {
            throw new TokenException(ErrorCode.UNAUTHORIZED);
        }
        String loginId = principal.getLoginId();

        return userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
    }

    private void validateAdminRole(CustomUser principal) {
        if (principal == null) {
            throw new TokenException(ErrorCode.UNAUTHORIZED);
        }

        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            throw new ForbiddenException(ErrorCode.CHALLENGE_FORBIDDEN);
        }
    }


    private Challenge saveChallenge(ChallengeReqDto req) {
        Challenge challenge = Challenge.builder()
                .title(req.getTitle())
                .challengeType(req.getChallengeType())
                .targetValue(req.getTargetValue())
                .description(req.getDescription())
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .build();
        return challengeRepository.save(challenge);
    }

    private void uploadImageIfPresent(MultipartFile file, Challenge challenge) {
        if (file != null && !file.isEmpty()) {
            validateFile(file);
            String key = fileStorage.upload(file, FileDomainType.CHALLENGE_IMAGE, challenge.getId());
            String imageUrl = fileStorage.toHttpsUrl(key);
            challenge.updateImageUrl(imageUrl);
        }
    }

    private void mapUserChallengeStatus(Long userId, List<ChallengeResDto> resDtos) {
        List<UserChallenge> myChallenges = userChallengeRepository.findByUserId(userId);

        Map<Long, UserChallengeStatus> statusMap = myChallenges.stream()
                .collect(Collectors.toMap(
                        uc -> uc.getChallenge().getId(),
                        UserChallenge::getStatus,
                        (existing, replacement) -> existing
                ));

        resDtos.forEach(dto -> {
            if (statusMap.containsKey(dto.getId())) {
                dto.setMyStatus(statusMap.get(dto.getId()));
            }
        });
    }

    private void validateFile(MultipartFile file) {
        if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            throw new FileUploadException(ErrorCode.FILE_NOT_IMAGE);
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new FileUploadException(ErrorCode.FILE_SIZE_EXCEEDED);
        }
    }
}