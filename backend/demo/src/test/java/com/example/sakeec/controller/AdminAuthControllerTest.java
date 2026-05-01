package com.example.sakeec.controller;

import com.example.sakeec.dto.AdminLoginResponse;
import com.example.sakeec.exception.BusinessException;
import com.example.sakeec.exception.GlobalExceptionHandler;
import com.example.sakeec.service.AdminService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminAuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AdminAuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper om;

    @MockitoBean private AdminService adminService;

    @Test
    @DisplayName("正常: 200 と JWT トークン返却")
    void loginOk() throws Exception {
        when(adminService.login(any())).thenReturn(new AdminLoginResponse("dummy.jwt.token"));

        String body = om.writeValueAsString(Map.of("username", "admin", "password", "admin1234"));

        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("dummy.jwt.token"));
    }

    @Test
    @DisplayName("認証失敗: AUTH_FAILED の BusinessException → 400 で固定メッセージ")
    void loginFailed() throws Exception {
        when(adminService.login(any()))
                .thenThrow(new BusinessException("AUTH_FAILED", "ユーザー名またはパスワードが正しくありません"));

        String body = om.writeValueAsString(Map.of("username", "admin", "password", "wrong"));

        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("AUTH_FAILED"))
                // セキュリティ: 詳細を漏らさない固定メッセージ
                .andExpect(jsonPath("$.message").value("ユーザー名またはパスワードが正しくありません"));
    }

    @Test
    @DisplayName("バリデーション: username 空 → 400 VALIDATION_ERROR")
    void blankUsername() throws Exception {
        String body = om.writeValueAsString(Map.of("username", "", "password", "x"));

        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("バリデーション: password 空 → 400")
    void blankPassword() throws Exception {
        String body = om.writeValueAsString(Map.of("username", "admin", "password", ""));

        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("logout: 常に 200 + メッセージ (ステートレス)")
    void logoutAlwaysOk() throws Exception {
        mockMvc.perform(post("/api/admin/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("logged out"));
    }
}
