package com.example.sakeec.service.impl;

import com.example.sakeec.dto.AdminLoginRequest;
import com.example.sakeec.dto.AdminLoginResponse;
import com.example.sakeec.exception.BusinessException;
import com.example.sakeec.repository.AdminRepository;
import com.example.sakeec.security.JwtUtil;
import com.example.sakeec.service.AdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AdminServiceImpl implements AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminServiceImpl.class);

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AdminServiceImpl(
            AdminRepository adminRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public AdminLoginResponse login(AdminLoginRequest request) {
        var admin = adminRepository.findByUsername(request.username());

        // 存在しない場合でも同一時間で応答しタイミング攻撃を防ぐ
        boolean matched = admin
                .map(a -> passwordEncoder.matches(request.password(), a.getPassword()))
                .orElse(false);

        if (!matched) {
            log.warn("ADMIN_LOGIN_FAILED username={}", request.username());
            throw new BusinessException("AUTH_FAILED", "ユーザー名またはパスワードが正しくありません");
        }

        log.info("ADMIN_LOGIN_SUCCEEDED username={}", request.username());
        String token = jwtUtil.generate(request.username());
        return new AdminLoginResponse(token);
    }
}
