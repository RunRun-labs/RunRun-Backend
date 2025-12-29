package com.multi.runrunbackend.domain.tts.constant;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : TtsCue
 * @since : 2025. 12. 24. Wednesday
 */
public enum TtsCue {
    // ===== 러닝 시작/종료/동기부여 =====
    START_RUN,               // 러닝을 시작합니다 화이팅하세요
    ARRIVED_DESTINATION,     // 목적지에 도착했어요~
    END_RUN,                 // 러닝을 종료합니다 고생하셨습니다
    MOTIVATE_GOOD_JOB,       // 힘내세요 잘 달리고있어요

    COUNTDOWN_3,//사아아암~~~
    COUNTDOWN_2,// 이이이~~~~
    COUNTDOWN_1, //이이일~~~~~

    // ===== 뛴 거리(누적) =====
    DIST_DONE_1KM, // 3KM! 통과~~!! 잘 하고 있어요~~!!
    DIST_DONE_2KM,
    DIST_DONE_3KM,
    DIST_DONE_4KM,
    DIST_DONE_5KM,
    DIST_DONE_6KM,
    DIST_DONE_7KM,
    DIST_DONE_8KM,
    DIST_DONE_9KM,
    DIST_DONE_10KM,

    // ===== 남은 거리 =====
    DIST_REMAIN_500M,
    DIST_REMAIN_300M,
    DIST_REMAIN_100M,

    // ===== 페이스(분:초) =====
    PACE_1M00,
    PACE_1M30,
    PACE_2M00,
    PACE_2M30,
    PACE_3M00,
    PACE_3M30,
    PACE_4M00,
    PACE_4M30,
    PACE_5M00,
    PACE_5M30,
    PACE_6M00,
    PACE_6M30,
    PACE_7M00,
    PACE_7M30,
    PACE_8M00,
    PACE_8M30,
    PACE_9M30,
    PACE_10M_PLUS,

    // ===== 경로 이상 =====
    OFF_ROUTE,               // 경로를 이탈했어요~ 다시 돌아가주세요
    BACK_ON_ROUTE,           // 경로를 복귀했습니다~~
    SPEED_TOO_FAST,          // 속도가 너무 빨라요

    // ===== 온라인 매칭 =====
    OPPONENT_AHEAD,          // 상대가 앞서고 있어요~
    YOU_AHEAD,               // 현재 앞서고 있어요~
    WIN,                     // 승리했어요~ 축하드려요~
    RANK_2,                  // 2등이에요~
    RANK_3,                  // 3등이에요~
    RANK_LAST,               // 꼴지에요… 분발하세요

    // ===== 고스트런 =====
    GHOST_AHEAD,             // 고스트가 앞서고 있어요~
    GHOST_BEHIND             // 고스트보다 빠르게 가고있어요~
}
