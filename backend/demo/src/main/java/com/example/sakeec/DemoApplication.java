package com.example.sakeec;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * UserDetailsServiceAutoConfiguration を除外:
 * 本アプリは JWT 認証 (JwtAuthenticationFilter) を使用するため、
 * Spring Boot がデフォルトで生成する InMemoryUserDetailsManager と
 * 「Using generated security password」のログは不要。
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
