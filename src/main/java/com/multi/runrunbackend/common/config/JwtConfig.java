package com.multi.runrunbackend.common.config;

import com.multi.runrunbackend.common.jwt.exception.JwtAccessDeniedHandler;
import com.multi.runrunbackend.common.jwt.exception.JwtAuthenticationEntryPoint;
import com.multi.runrunbackend.common.jwt.filter.JwtFilter;
import com.multi.runrunbackend.common.jwt.provider.TokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity(prePostEnabled = true)
public class JwtConfig {

    private final TokenProvider tokenProvider;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final RedisTemplate<String, String> redisTemplate;


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .sessionManagement(
                session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/auth/**",
                    "/login",
                    "/signup",
                    "/css/**",
                    "/js/**",
                    "/img/**",
                    "/favicon.ico",
                    "/actuator/health",
                    "/actuator/health/**",
                    "/actuator/prometheus",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/error",
                    "/public/**",
                    "/attendance-event",
                    // ✅ HTML 페이지 경로는 permitAll (프론트엔드에서 토큰 체크)
                    "/",
                    "/home",
                    "/match/**",
                    "/recruit/**",
                    "/crews/**",
                    "/chat/**",
                    "/notification",
                    "/myPage/**",
                    "/feed/**",
                    "/challenge/**",
                    "/course**",
                    "/courseCreate",
                    "/courseDetail/**",
                    "/courseUpdate/**",
                    "/membership",
                    "/payment/**",
                    "/points/**",
                    "/admin/**",
                    "/test/**",
                    "/setting/**",
                    "/terms/**",
                    "/profile/**",
                    "/friends/**",
                    "/coupon/**",
                    "/running/**"
                ).permitAll()
                .requestMatchers(
                    PathRequest.toStaticResources().atCommonLocations()
                ).permitAll()
                // ✅ API 요청만 인증 필요 (Authorization 헤더로 처리)
                .requestMatchers("/api/**").authenticated()
                // ✅ 나머지 경로도 인증 필요
                .anyRequest().authenticated()
            )
            .addFilterBefore(
                new JwtFilter(tokenProvider, redisTemplate),
                UsernamePasswordAuthenticationFilter.class
            )
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .accessDeniedHandler(jwtAccessDeniedHandler)
            );

        return http.build();
    }

}
