package com.multi.runrunbackend.domain.user.entity;

import com.multi.runrunbackend.common.entitiy.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 *
 * @author : kimyongwon
 * @description : 사용자 개인 환경 설정 엔티티 - 알림 허용 여부 - 야간 알림 허용 여부 - TTS 안내 간격
 * @filename : UserSetting
 * @since : 25. 12. 17. 오전 11:46 수요일
 */
@Entity
@Table(
        name = "user_settings",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSetting extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "notification_enabled", nullable = false)
    private boolean notificationEnabled = true;

    @Column(name = "night_notification_enabled", nullable = false)
    private boolean nightNotificationEnabled = false;


    public static UserSetting createDefault(User user) {
        UserSetting setting = new UserSetting();
        setting.user = user;
        return setting;
    }

    public void updateNotification(boolean enabled, boolean nightEnabled) {
        this.notificationEnabled = enabled;
        this.nightNotificationEnabled = nightEnabled;
    }


}