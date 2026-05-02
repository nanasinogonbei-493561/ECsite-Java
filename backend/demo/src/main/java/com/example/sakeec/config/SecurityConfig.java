package com.example.sakeec.config;

import com.example.sakeec.security.JwtAuthenticationFilter;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        return http
                // REST API + JWT のため CSRF 不要 (state を持たない)
                .csrf(AbstractHttpConfigurer::disable)
                // セッションを作らない
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 不要なフォームログイン / Basic を明示的に無効化
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // 公開エンドポイント
                        .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/orders").permitAll()
                        .requestMatchers("/api/admin/login", "/api/admin/logout").permitAll()
                        .requestMatchers("/health", "/actuator/health").permitAll()
                        // 管理 API は要認証
                        .requestMatchers("/api/admin/**").authenticated()
                        // それ以外 (静的リソース等) は許可
                        .anyRequest().permitAll()
                )
                // 未認証アクセスは 401 (※デフォルトは 403)。
                // ボディに JSON を返したいため簡易ハンドラを下に置くカスタム EntryPoint も併用可能だが、
                // ここではステータス統一を最優先 (Bodyは GlobalExceptionHandler 経由ではない経路のため空)。
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint((req, res, ex) -> {
                            res.setStatus(HttpStatus.UNAUTHORIZED.value());
                            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            String traceId = MDC.get("traceId");
                            String body = "{\"code\":\"UNAUTHORIZED\",\"message\":\"authentication required\","
                                    + "\"traceId\":\"" + (traceId == null ? "" : traceId) + "\"}";
                            res.getWriter().write(body);
                        })
                )
                // JWT 認証フィルタを UsernamePasswordAuthenticationFilter の前に挟む
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
