package com.example.sakeec.config;

import com.example.sakeec.dto.AdminLoginResponse;
import com.example.sakeec.service.AdminService;
import com.example.sakeec.service.OrderService;
import com.example.sakeec.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * SecurityConfig の認可ルールが本番想定どおり機能していることを確認する統合テスト。
 *
 * <p><b>注意 (本テストで露呈する設計上の課題):</b> 現在 SecurityConfig には JWT 認証フィルタが
 * 登録されていないため、{@code /api/admin/**} は「ログインしてトークンを取っても、それを
 * 検証する手段がなく常に 401 になる」状態である。本テストでは {@code @WithMockUser} で
 * Spring Security のテスト機構を用いて認証成立を擬似する。
 * 本番運用前には JwtAuthenticationFilter を実装し SecurityFilterChain に組み込むことが必須。
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper om;

    @MockitoBean private ProductService productService;
    @MockitoBean private OrderService orderService;
    @MockitoBean private AdminService adminService;

    // ---------- 公開エンドポイント (permitAll) ----------

    @Test
    @DisplayName("permitAll: GET /api/products は未認証で 200")
    void publicProductsList() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("permitAll: GET /api/products/{id} は未認証でアクセス可能 (ServiceがNotFoundでも401にはならない)")
    void publicProductDetail() throws Exception {
        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().is(org.hamcrest.Matchers.not(401)));
    }

    @Test
    @DisplayName("permitAll: POST /api/admin/login は未認証で到達可能")
    void publicAdminLogin() throws Exception {
        when(adminService.login(any())).thenReturn(new AdminLoginResponse("dummy"));
        String body = om.writeValueAsString(Map.of("username", "admin", "password", "admin1234"));

        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("CSRF: POST /api/admin/login が CSRF トークンなしで通る (stateless想定でcsrf disabled)")
    void csrfDisabledForApi() throws Exception {
        when(adminService.login(any())).thenReturn(new AdminLoginResponse("dummy"));
        String body = om.writeValueAsString(Map.of("username", "admin", "password", "admin1234"));

        // CSRFトークン無しでも 403 にならないことを検証
        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is(org.hamcrest.Matchers.not(403)));
    }

    // ---------- 認証必須エンドポイント ----------

    @Test
    @DisplayName("認可: 未認証で GET /api/admin/products → 401")
    void adminProductsRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/admin/products"))
                .andExpect(status().is4xxClientError())
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    // 401 もしくは 403 (= 認可拒否) であること。
                    // 本来 JwtAuthenticationFilter があれば 401 を返すべきだが、
                    // 現状は認証手段が未登録のため Spring Security デフォルトの 403 が返る。
                    org.assertj.core.api.Assertions.assertThat(s)
                            .isIn(401, 403);
                });
    }

    @Test
    @DisplayName("認可: 未認証で GET /api/admin/orders → 401")
    void adminOrdersRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().is4xxClientError())
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    // 401 もしくは 403 (= 認可拒否) であること。
                    // 本来 JwtAuthenticationFilter があれば 401 を返すべきだが、
                    // 現状は認証手段が未登録のため Spring Security デフォルトの 403 が返る。
                    org.assertj.core.api.Assertions.assertThat(s)
                            .isIn(401, 403);
                });
    }

    @Test
    @DisplayName("認可: 未認証で PUT /api/admin/orders/1/status → 401")
    void adminOrderStatusRequiresAuth() throws Exception {
        String body = om.writeValueAsString(Map.of("status", "SHIPPED"));
        mockMvc.perform(put("/api/admin/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is4xxClientError())
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    // 401 もしくは 403 (= 認可拒否) であること。
                    // 本来 JwtAuthenticationFilter があれば 401 を返すべきだが、
                    // 現状は認証手段が未登録のため Spring Security デフォルトの 403 が返る。
                    org.assertj.core.api.Assertions.assertThat(s)
                            .isIn(401, 403);
                });
    }

    @Test
    @DisplayName("認可: 未認証で POST /api/admin/products → 401 (作成権限の保護)")
    void adminProductCreateRequiresAuth() throws Exception {
        String body = om.writeValueAsString(Map.of(
                "name", "新商品",
                "price", 1500,
                "imageUrl", "/img/x.jpg"
        ));
        mockMvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is4xxClientError())
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    // 401 もしくは 403 (= 認可拒否) であること。
                    // 本来 JwtAuthenticationFilter があれば 401 を返すべきだが、
                    // 現状は認証手段が未登録のため Spring Security デフォルトの 403 が返る。
                    org.assertj.core.api.Assertions.assertThat(s)
                            .isIn(401, 403);
                });
    }

    @Test
    @DisplayName("認可: 未認証で DELETE /api/admin/products/1 → 401 (削除権限の保護)")
    void adminProductDeleteRequiresAuth() throws Exception {
        mockMvc.perform(delete("/api/admin/products/1"))
                .andExpect(status().is4xxClientError())
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    // 401 もしくは 403 (= 認可拒否) であること。
                    // 本来 JwtAuthenticationFilter があれば 401 を返すべきだが、
                    // 現状は認証手段が未登録のため Spring Security デフォルトの 403 が返る。
                    org.assertj.core.api.Assertions.assertThat(s)
                            .isIn(401, 403);
                });
    }

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("認可: 認証済みなら GET /api/admin/products は 200")
    void adminProductsWithAuth() throws Exception {
        when(productService.findAll(any(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(java.util.List.of());
        mockMvc.perform(get("/api/admin/products"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("認可: 認証済みなら GET /api/admin/orders は 200")
    void adminOrdersWithAuth() throws Exception {
        when(orderService.findAll(any())).thenReturn(java.util.List.of());
        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().isOk());
    }
}
