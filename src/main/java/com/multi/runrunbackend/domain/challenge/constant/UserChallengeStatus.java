package com.multi.runrunbackend.domain.challenge.constant;

/**
 * * 사용자의 챌린지 참여 상태를 나타내는 Enum + * JOINED(참여), IN_PROGRESS(진행중), COMPLETED(완료), FAILED(실패),
 * CANCELED(취소)
 *
 * @author : 김용원
 * @filename : UserChallengeStatus
 * @since : 25. 12. 17. 오전 10:29 수요일
 */
public enum UserChallengeStatus {
    JOINED,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    CANCELED
}
