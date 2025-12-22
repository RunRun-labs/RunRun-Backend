package com.multi.runrunbackend.common.jwt.service;

import com.multi.runrunbackend.common.exception.custom.RefreshTokenException;
import com.multi.runrunbackend.common.exception.custom.TokenException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.common.jwt.dto.TokenDto;
import com.multi.runrunbackend.common.jwt.provider.TokenProvider;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class TokenService {

    @Value("${jwt.refresh-token-expire-time}")
    private long REFRESH_TOKEN_EXPIRE_TIME;

    private final TokenProvider tokenProvider;
    private final RedisTemplate<String, String> redisTemplate;
    private final UserRepository userRepository;


    @Transactional(noRollbackFor = RefreshTokenException.class)
    public <T> TokenDto createToken(T t) {
        String LoginId;
        List<String> roles;
        String accessToken;
        String refreshToken;
        Long userId;

        if (t instanceof String) {
            String jwt = tokenProvider.resolveToken((String) t);
            Claims claims = tokenProvider.parseClames(jwt);

            LoginId = claims.getSubject();
            String savedToken = redisTemplate.opsForValue().get(LoginId);
            if (savedToken == null) {
                log.warn("[TokenService] Redis에 리프레시 토큰이 존재하지 않음 (만료 또는 로그아웃)");
                throw new RefreshTokenException(ErrorCode.REFRESH_TOKEN_EXPIRED);
            }
            if (!savedToken.equals(jwt)) {
                log.error("[TokenService] 리프레시 토큰 불일치");
                throw new RefreshTokenException(ErrorCode.INVALID_REFRESH_TOKEN);
            }

            Optional<User> user = userRepository.findByLoginId(LoginId);
            userId = user.get().getId();
            String role = user.get().getRole();
            roles = Arrays.asList(role.split(","));

            log.info("Map MemberId >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> {}", LoginId);
            log.info("Map Roles >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> {}", roles);

            refreshToken = handleRefreshToken(LoginId);

            accessToken = createAccessToken(LoginId, roles, userId);
            redisTemplate.opsForValue()
                .set(LoginId, refreshToken, Duration.ofMinutes(REFRESH_TOKEN_EXPIRE_TIME));

            log.info("[TokenService] 리프레시 토큰 재발급 완료 - memberId: {}", LoginId);
        } else if (t instanceof Map) {
            Map<String, Object> data = (Map<String, Object>) t;
            LoginId = (String) data.get("loginId");
            roles = (List<String>) data.get("roles");

            log.info("Map MemberEmail >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> {}", LoginId);
            log.info("Map Roles >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> {}", roles);
            refreshToken = handleRefreshToken(LoginId);
            User user = userRepository.findByLoginId(LoginId)
                .orElseThrow(() -> new RuntimeException("회원 정보가 없습니다"));
            userId = user.getId();

            accessToken = createAccessToken(LoginId, roles, userId);

        } else {
            throw new IllegalArgumentException("Invalid token type !!");
        }

        return TokenDto.builder().accessToken(accessToken).refreshToken(refreshToken).build();
    }


    @Transactional(noRollbackFor = RefreshTokenException.class)
    public String handleRefreshToken(String loginId) {

        redisTemplate.delete(loginId);

        String newRefreshToken = createRefreshToken(loginId, 0L);

        redisTemplate.opsForValue()
            .set(loginId, newRefreshToken, Duration.ofMinutes(REFRESH_TOKEN_EXPIRE_TIME));

        return newRefreshToken;
    }


    private String createAccessToken(String loginId, List<String> roles, Long userId) {
        return tokenProvider.generateToken(loginId, roles, "A", userId);
    }

    private String createRefreshToken(String loginId, Long userId) {
        return tokenProvider.generateToken(loginId, null, "R", userId);
    }

    public void deleteRefreshToken(String accessToken) {
        String token = tokenProvider.resolveToken(accessToken);
        String email = tokenProvider.getUserId(token);

        log.info("Refresh Token 삭제 완료 : 사용자 email - {}", email);
    }

    public void registBlackList(String accessToken) {
        // "Bearer " 제거 (헤더에서 받은 경우)
        String token = tokenProvider.resolveToken(accessToken); // Bearer 제거
        if (!tokenProvider.validateToken(token)) {
            throw new TokenException(ErrorCode.INVALID_TOKEN);
        }

        // 토큰 만료 시간 추출
        Claims claims = tokenProvider.parseClames(token);
        Date expiration = claims.getExpiration();
        long now = System.currentTimeMillis();
        long remainTime = expiration.getTime() - now;

        if (remainTime <= 0) {
            log.warn("[TokenService] 이미 만료된 토큰입니다. 블랙리스트 등록 생략.");
            throw new TokenException(ErrorCode.EXPIRED_TOKEN);
        }

        // Redis Key 설정: blacklist:{토큰}
        String key = "blacklist:" + token;

        // Value는 단순 표시용 (예: "logout")
        redisTemplate.opsForValue().set(key, "logout", Duration.ofMillis(remainTime));

        System.out.println("블랙리스트 등록 완료: " + key + " (TTL: " + expiration + "ms)");
    }

    public String getMemberIdFromAccessToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");

        if (bearer == null || !bearer.startsWith("Bearer ")) {
            throw new TokenException(ErrorCode.MISSING_AUTHORIZATION_HEADER);
        }

        String token = tokenProvider.resolveToken(bearer);

        return tokenProvider.getUserId(token); // subject = memberId
    }
}
