package com.multi.runrunbackend.domain.auth.service;


import static java.time.LocalDateTime.now;

import com.multi.runrunbackend.common.exception.custom.DuplicateUsernameException;
import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.common.jwt.dto.TokenDto;
import com.multi.runrunbackend.common.jwt.service.TokenService;
import com.multi.runrunbackend.domain.auth.dto.AuthSignInResDto;
import com.multi.runrunbackend.domain.user.dto.req.UserSignInDto;
import com.multi.runrunbackend.domain.user.dto.req.UserSignUpDto;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate;

    private final CustomUserDetailService customUserDetailService;
    private final TokenService tokenService;

    @Transactional
    public void signUp(UserSignUpDto userSignUpDto) {
        if (userRepository.findByLoginId(userSignUpDto.getLoginId()).isPresent()) {
            throw new DuplicateUsernameException(ErrorCode.DUPLICATE_USER);
        }
        String role = userSignUpDto.getLoginId().toLowerCase().contains("admin") ? "ROLE_ADMIN"
            : "ROLE_USER";
        userSignUpDto.setUserPassword(
            passwordEncoder.encode(userSignUpDto.getUserPassword()));
        User user = User.toEntity(userSignUpDto);
        user.setRole(role);
        User savedMember = userRepository.save(user);
        if (savedMember == null) {
            throw new RuntimeException("회원가입에 실패했습니다.");
        }
        String key = "ROLE:" + userSignUpDto.getLoginId();
        redisTemplate.opsForValue()
            .set(key, savedMember.getRole(), Duration.ofHours(1));

        String savedRole = redisTemplate.opsForValue().get(key);
        if (savedRole == null) {
            throw new RuntimeException("Redis에 저장 실패");
        }
    }

    @Transactional
    public AuthSignInResDto login(UserSignInDto userSignInDto) {
        UserDetails userDetails = customUserDetailService.loadUserByUsername(
            userSignInDto.getLoginId());
        if (!passwordEncoder.matches(userSignInDto.getLoginPw(),
            userDetails.getPassword())) {
            throw new BadCredentialsException("비밀번호가 일치하지 않습니다");
        }
        List<String> roles = userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .toList(); // 롤이 여러개 일때 VIP,PREMIUM 등등 여러 역할이 있을 때 어차피 확장해야한다...
        Map<String, Object> loginData = new HashMap<>(); // 나중에 더 데이터가 들어갔을 때 확장성에 용이하다
        loginData.put("loginId", userSignInDto.getLoginId());
        loginData.put("roles", roles);

        TokenDto token = tokenService.createToken(loginData);

        User user = userRepository.findByLoginId(userSignInDto.getLoginId())
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
        user.updateLastLogin(now());

        return AuthSignInResDto.from(token, user);
    }
}

