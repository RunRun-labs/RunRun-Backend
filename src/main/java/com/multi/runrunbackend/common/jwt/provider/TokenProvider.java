package com.multi.runrunbackend.common.jwt.provider;

import com.multi.runrunbackend.common.exception.custom.TokenException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Key;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TokenProvider {


    private static final String AUTHORITIES_KEY = "auth";  // 클레임에서 권한정보담을키
    private static final long ACCESS_TOKEN_EXPIRE_TIME = 1000 * 60 * 10 * 10;     //3분
    private static final long REFRESH_TOKEN_EXPIRE_TIME =
        1000L * 60 * 60 * 24 * 30; //1000L * 60 * 60 * 24 * 1;  // 1일

    private final JwtProvider jwtProvider;  // JwtProvider 의존성 추가
    private final Key SKEY;
    private final String ISSUER;
    private final RedisTemplate<String, String> redisTemplate;
    private final UserRepository userRepository;

    //application.yml 에 정의해놓은 jwt.secret 값을 가져와서 JWT 를 만들 때 사용하는 암호화 키값을 생성
    public TokenProvider(JwtProvider jwtProvider, RedisTemplate<String, String> redisTemplate,
        UserRepository userRepository) {
        this.jwtProvider = jwtProvider;
        SKEY = jwtProvider.getSecretKey();
        ISSUER = jwtProvider.getIssuer();
        this.redisTemplate = redisTemplate;
        this.userRepository = userRepository;
        System.out.println("TokenProvider-------------" + SKEY);
        System.out.println("   ISSUER     -------------" + ISSUER);

    }

    public String generateToken(String memberId, List<String> roles, String code, Long memberNo) {
        Claims claims = Jwts
            .claims()
            .setSubject(memberId);
        claims.put("memberNo", memberNo);
        long now = (new Date()).getTime();
        Date tokenExpiresIn = new Date();
        if (code.equals("A")) {
            tokenExpiresIn = new Date(now + ACCESS_TOKEN_EXPIRE_TIME);
            claims.put(AUTHORITIES_KEY, String.join(",", roles)); // List<String> -> 콤마로 구분된 문자열
        } else {
            tokenExpiresIn = new Date(now + REFRESH_TOKEN_EXPIRE_TIME);

        }

        String token = Jwts.builder()
            .setIssuer(ISSUER)
            .setIssuedAt(new Date(now))
            .setClaims(claims)
            .setExpiration(tokenExpiresIn)
            .signWith(SKEY, SignatureAlgorithm.HS512)
            .compact();

        // ✅ 리프레시 토큰이면 Redis에 즉시 저장
        if ("R".equals(code)) {
            redisTemplate.opsForValue().set(
                memberId,
                token,
                Duration.ofMillis(REFRESH_TOKEN_EXPIRE_TIME)
            );
            log.info("[TokenProvider] Redis에 리프레시 토큰 저장 완료: memberId={}, TTL={}분",
                memberId, REFRESH_TOKEN_EXPIRE_TIME / 60000);
        }

        return token;
    }

    public boolean validateToken(String token) {
        try {
            log.info("[TokenProvider] 유효성 검증 중인 토큰: {}", token);
            // 토큰을 비밀 키 와함께 복호화를 진행 해서 유효하지 않으면 false 반환, 유효하면 true 반환
            Jwts.parserBuilder()
                .setSigningKey(SKEY)
                .build()
                .parseClaimsJws(token);

            log.info("[TokenProvider] JWT 토큰이 유효합니다.");
            return true;

        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.error("[TokenProvider] 잘못된 JWT 서명입니다. 토큰: {}", token, e);
            throw new TokenException(ErrorCode.INVALID_TOKEN);
        } catch (ExpiredJwtException e) {
            log.warn("[TokenProvider] 만료된 JWT 토큰입니다. 토큰: {}, 만료 시각: {}", token,
                e.getClaims().getExpiration(), e);
            throw new TokenException(ErrorCode.EXPIRED_TOKEN);

        } catch (UnsupportedJwtException e) {
            log.error("[TokenProvider] 지원되지 않는 JWT 토큰입니다. 토큰: {}", token, e);
            throw new TokenException(ErrorCode.UNAUTHORIZED);

        } catch (IllegalArgumentException e) {
            log.error("[TokenProvider] JWT 토큰이 잘못되었습니다. 토큰: {}", token, e);
            throw new TokenException(ErrorCode.TOKEN_BAD_REQUEST);
        }
    }

    public LocalDateTime getRefreshTokenExpiry() {
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime reTokenExpiry = now.plus(REFRESH_TOKEN_EXPIRE_TIME, ChronoUnit.MILLIS);

        return reTokenExpiry;
    }

    public Authentication getAuthentication(String jwt) {

        Claims claims = parseClames(jwt);
        if (claims.get(AUTHORITIES_KEY) == null) {
            throw new RuntimeException("권한 정보가 없는 토큰입니당");
        }
        Collection<? extends GrantedAuthority> authorities =
            Arrays.stream(claims.get(AUTHORITIES_KEY).toString().split(","))
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        log.info("[TokenProvider] authorities : {}", authorities);

        CustomUser customUser = new CustomUser();
        customUser.setEmail(claims.getSubject());
        customUser.setAuthorities(authorities);
        return new UsernamePasswordAuthenticationToken(customUser, "", authorities);
    }

    public Claims parseClames(String jwt) {
        try {
            return Jwts.parserBuilder().setSigningKey(SKEY).build().parseClaimsJws(jwt).getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }

    }

    public String getUserId(String token) {
        return Jwts.parserBuilder().setSigningKey(SKEY).build().parseClaimsJws(token).getBody()
            .getSubject();
    }

    public Long getMemberNo(String token) {
        Claims claims = Jwts.parserBuilder()
            .setSigningKey(SKEY)
            .build()
            .parseClaimsJws(token)
            .getBody();
        return claims.get("memberNo", Long.class);
    }


    public String resolveToken(String token) {
        // Bearer 접두어가 있는 경우 제거하고 순수한 토큰 반환
        if (token != null && token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        return token;
    }

    public String extractMemberId(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            throw new TokenException(ErrorCode.MISSING_AUTHORIZATION_HEADER);
        }

        String jwt = resolveToken(bearerToken);
        return getUserId(jwt);
    }
}
