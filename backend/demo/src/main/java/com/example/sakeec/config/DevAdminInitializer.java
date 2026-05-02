package com.example.sakeec.config;

import com.example.sakeec.entity.Admin;
import com.example.sakeec.repository.AdminRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 開発・テスト環境でのみ動作する初期管理者シード。
 * <p>BCrypt ハッシュは起動時に PasswordEncoder で実時間生成するため、
 * data.sql に固定ハッシュを書き込む方式と違って「ハッシュアルゴリズム / ラウンド数の
 * 不一致でログイン不可」を構造的に防げる。
 * <p>本番 (profile=prod) では起動しない。本番管理者は別途プロビジョニングする。
 */
@Component
@Profile({"default", "dev", "test"})
public class DevAdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevAdminInitializer.class);

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;

    public DevAdminInitializer(AdminRepository adminRepository,
                               PasswordEncoder passwordEncoder,
                               @Value("${app.dev.admin.username:admin}") String username,
                               @Value("${app.dev.admin.password:admin1234}") String password) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        adminRepository.findByUsername(username).ifPresentOrElse(
                a -> log.info("DEV_ADMIN_ALREADY_EXISTS username={}", username),
                () -> {
                    Admin a = new Admin();
                    a.setUsername(username);
                    a.setPassword(passwordEncoder.encode(password));
                    adminRepository.save(a);
                    log.info("DEV_ADMIN_CREATED username={} (DEV ONLY - never for production)", username);
                }
        );
    }
}
