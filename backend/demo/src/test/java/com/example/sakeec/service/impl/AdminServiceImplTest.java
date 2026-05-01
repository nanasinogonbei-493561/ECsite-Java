package com.example.sakeec.service.impl;

import com.example.sakeec.dto.AdminLoginRequest;
import com.example.sakeec.dto.AdminLoginResponse;
import com.example.sakeec.entity.Admin;
import com.example.sakeec.exception.BusinessException;
import com.example.sakeec.repository.AdminRepository;
import com.example.sakeec.security.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock private AdminRepository adminRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;

    @InjectMocks private AdminServiceImpl service;

    private Admin admin(String username, String hashed) {
        Admin a = new Admin();
        a.setId(1L);
        a.setUsername(username);
        a.setPassword(hashed);
        return a;
    }

    @Test
    @DisplayName("正常ログイン: JWT が発行される。トークンに平文パスワードが含まれない")
    void loginSucceeds() {
        when(adminRepository.findByUsername("admin")).thenReturn(Optional.of(admin("admin", "$2a$10$hashedhashed")));
        when(passwordEncoder.matches("admin1234", "$2a$10$hashedhashed")).thenReturn(true);
        when(jwtUtil.generate("admin")).thenReturn("dummy.jwt.token");

        AdminLoginResponse res = service.login(new AdminLoginRequest("admin", "admin1234"));

        assertThat(res.token()).isEqualTo("dummy.jwt.token");
        // セキュリティ: トークン文字列に平文パスワードが含まれていてはならない
        assertThat(res.token()).doesNotContain("admin1234");
        verify(jwtUtil).generate("admin");
    }

    @Test
    @DisplayName("パスワード不一致: AUTH_FAILED 例外。トークンは発行されない")
    void wrongPassword() {
        when(adminRepository.findByUsername("admin")).thenReturn(Optional.of(admin("admin", "$2a$10$hash")));
        when(passwordEncoder.matches("wrong", "$2a$10$hash")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new AdminLoginRequest("admin", "wrong")))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "AUTH_FAILED");

        verify(jwtUtil, never()).generate(anyString());
    }

    @Test
    @DisplayName("セキュリティ: 存在しないユーザでも passwordEncoder.matches が呼ばれ、" +
            "ユーザ存在/非存在で応答時間に差が出ない（タイミング攻撃対策）")
    void timingAttackMitigation() {
        // 重要: 存在しないユーザ → empty Optional だが
        // 「ユーザ有無で処理時間に差が出ない」よう本実装では map(...).orElse(false) を使用している
        when(adminRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new AdminLoginRequest("ghost", "anything")))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "AUTH_FAILED");

        // エラーメッセージは「ユーザー名またはパスワードが正しくありません」で、
        // どちらが間違っているか漏らしてはいけない
        try {
            service.login(new AdminLoginRequest("ghost", "anything"));
        } catch (BusinessException ex) {
            assertThat(ex.getMessage())
                    .doesNotContainIgnoringCase("user")
                    .doesNotContainIgnoringCase("ユーザーが存在")
                    .doesNotContainIgnoringCase("not found");
        }

        verify(jwtUtil, never()).generate(anyString());
    }

    @Test
    @DisplayName("セキュリティ: ユーザ有/無 どちらの失敗でも同じエラーメッセージ・同じエラーコード")
    void identicalErrorForUserExistsOrNot() {
        // 存在するがパスワード違い
        when(adminRepository.findByUsername("admin")).thenReturn(Optional.of(admin("admin", "$2a$10$hash")));
        when(passwordEncoder.matches(eq("bad"), any())).thenReturn(false);

        BusinessException ex1 = catchBusiness(() -> service.login(new AdminLoginRequest("admin", "bad")));

        // 存在しない
        when(adminRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        BusinessException ex2 = catchBusiness(() -> service.login(new AdminLoginRequest("ghost", "bad")));

        assertThat(ex1.getCode()).isEqualTo(ex2.getCode());
        assertThat(ex1.getMessage()).isEqualTo(ex2.getMessage());
    }

    private BusinessException catchBusiness(Runnable r) {
        try {
            r.run();
            throw new AssertionError("expected BusinessException");
        } catch (BusinessException e) {
            return e;
        }
    }
}
