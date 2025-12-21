package com.multi.runrunbackend.domain.challenge.service;

import com.multi.runrunbackend.common.exception.custom.FileUploadException;
import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.custom.TokenException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.common.file.FileDomainType;
import com.multi.runrunbackend.common.file.storage.FileStorage;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.challenge.constant.UserChallengeStatus;
import com.multi.runrunbackend.domain.challenge.dto.req.ChallengeCreateReqDto;
import com.multi.runrunbackend.domain.challenge.dto.req.ChallengeUpdateReqDto;
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
@Transactional
public class ChallengeService {

    private final ChallengeRepository challengeRepository;
    private final UserChallengeRepository userChallengeRepository;
    private final UserRepository userRepository;
    private final FileStorage fileStorage;

    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024; // 5MB

    // 챌린지 생성 (ADMIN)
    public ChallengeResDto createChallenge(ChallengeCreateReqDto req, MultipartFile imageFile, CustomUser principal) {
        User user = getUserByPrincipal(principal);
        // TODO: 필요 시 user.getRole() 등을 통해 관리자 권한 추가 검증
        // validateAdminRole(user); // 실제 권한 체크 필요 시 주석 해제

        Challenge savedChallenge = saveChallenge(req);

        uploadImageIfPresent(imageFile, savedChallenge);

        return ChallengeResDto.from(savedChallenge);
    }

    // 챌린지 수정 (ADMIN)
    public void updateChallenge(Long challengeId, ChallengeUpdateReqDto req, MultipartFile imageFile, CustomUser principal) {
        User user = getUserByPrincipal(principal);
        // validateAdminRole(user);

        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.INVALID_REQUEST));

        // 정보 업데이트
        challenge.update(
                req.getTitle(),
                req.getChallengeType(),
                req.getTargetValue(),
                req.getDescription(),
                req.getStartDate(),
                req.getEndDate()
        );

        // 이미지 파일이 새로 들어온 경우 교체
        if (imageFile != null && !imageFile.isEmpty()) {
            validateFile(imageFile);
            // 기존 이미지가 있다면 스토리지에서 삭제
            if (challenge.getImageUrl() != null) fileStorage.delete(challenge.getImageUrl());

            String imageUrl = fileStorage.upload(imageFile, FileDomainType.CHALLENGE_IMAGE, challenge.getId());
            challenge.updateImageUrl(imageUrl);
        }
    }

    // 챌린지 삭제 (ADMIN)
    public void deleteChallenge(Long challengeId, CustomUser principal) {
        User user = getUserByPrincipal(principal);
        // validateAdminRole(user);

        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.INVALID_REQUEST));


        challengeRepository.delete(challenge);
    }

    // 챌린지 목록 조회 (로그인 시 상태 포함)
    @Transactional(readOnly = true)
    public List<ChallengeResDto> getChallengeList(CustomUser principal) {
        List<Challenge> challenges = challengeRepository.findAll();
        List<ChallengeResDto> resDtos = challenges.stream()
                .map(ChallengeResDto::from)
                .collect(Collectors.toList());

        if (principal != null) {
            User user = getUserByPrincipal(principal);
            mapUserChallengeStatus(user.getId(), resDtos);
        }

        return resDtos;
    }


    private User getUserByPrincipal(CustomUser principal) {
        if (principal == null) {
            throw new TokenException(ErrorCode.UNAUTHORIZED);
        }
        String loginId = principal.getLoginId();

        return userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
    }

    private Challenge saveChallenge(ChallengeCreateReqDto req) {
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
            String imageUrl = fileStorage.upload(file, FileDomainType.CHALLENGE_IMAGE, challenge.getId());
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