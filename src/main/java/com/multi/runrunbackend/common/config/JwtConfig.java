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
            sesstion -> sesstion.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        ).authorizeHttpRequests(auth -> auth
            .requestMatchers(
                "/auth/**",
                "/login",
                "/signup",
                "/swagger-ui/**",
                "/v3/api-docs/**",
                "/public/**",
                "/error",
                "/img/**",
                "/chat/**",
                "/ws/**",
                "/myPage/**",
                "/course_auto/**",
                "/api/routes/**",
                "/files/**",
                "/course",
                "/courseCreate",
                "/courseDetail/**",
                "/courseUpdate/**",
                "/course_manual/**",
                "/crews/new",
                "/crews/**",
                "/test/gps",
                "/match/**",
                "/recruit/**",
                "/tts-test"
            ).permitAll()
            .requestMatchers(
                PathRequest.toStaticResources().atCommonLocations()
            ).permitAll()
            .anyRequest().authenticated()

        ).addFilterBefore(new JwtFilter(tokenProvider, redisTemplate),
            UsernamePasswordAuthenticationFilter.class).exceptionHandling(
            exception -> exception.authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .accessDeniedHandler(jwtAccessDeniedHandler));

    return http.build();

  }

}
