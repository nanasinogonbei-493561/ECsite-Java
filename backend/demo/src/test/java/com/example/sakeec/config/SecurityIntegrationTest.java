package com.example.sakeec.config;

import com.example.sakeec.dto.AdminLoginResponse;
import com.example.sakeec.security.JwtUtil;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * SecurityConfig + JwtAuthenticationFilter の認可動作を統合的に検証。
 * <ul>
 *   <li>permitAll エンドポイントは未認証で 200</li>
 *   <li>認証必須エンドポイントは未認証で <b>401</b> (403 ではない)</li>
 *   <li>有効な Bearer トークンを付与すれば 200</li>
 *   <li>不正/期限切れ/署名違反のトークンは 401</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper om;
    @Autowired private JwtUtil jwtUtil;

    @MockitoBean private ProductService productService;
    @MockitoBean private OrderService orderService;
    @MockitoBean private AdminService adminService;

    // ---------- 公開エンドポイント (permitAll) ----------

    @Test
    @DisplayName("permitAll: GET /api/products は未認証で 200")
    void publicProductsList() throws Exception {
        when(productService.findAll(any(), anyInt())).thenReturn(List.of());
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("permitAll: GET /api/products/{id} は未認証でアクセス可能 (401 にならない)")
    void publicProductDetail() throws Exception {
        when(productService.findById(any())).thenReturn(null);
        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().is(org.hamcrest.Matchers.not(401)))
                .andExpect(status().is(org.hamcrest.Matchers.not(403)));
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
    @DisplayName("CSRF: POST /api/admin/login が CSRFトークンなしで通る (stateless想定でcsrf disabled)")
    void csrfDisabledForApi() throws Exception {
        when(adminService.login(any())).thenReturn(new AdminLoginResponse("dummy"));
        String body = om.writeValueAsString(Map.of("username", "admin", "password", "admin1234"));

        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is(org.hamcrest.Matchers.not(403)));
    }

    // ---------- 認証必須エンドポイント (未認証) ----------

    @Test
    @DisplayName("認可: 未認証で GET /api/admin/products → 401 + UNAUTHORIZED")
    void adminProductsRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/admin/products"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("認可: 未認証で GET /api/admin/orders → 401")
    void adminOrdersRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("認可: 未認証で PUT /api/admin/orders/1/status → 401")
    void adminOrderStatusRequiresAuth() throws Exception {
        String body = om.writeValueAsString(Map.of("status", "SHIPPED"));
        mockMvc.perform(put("/api/admin/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("認可: 未認証で POST /api/admin/products → 401")
    void adminProductCreateRequiresAuth() throws Exception {
        String body = om.writeValueAsString(Map.of(
                "name", "新商品", "price", 1500, "imageUrl", "/img/x.jpg"));
        mockMvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("認可: 未認証で DELETE /api/admin/products/1 → 401")
    void adminProductDeleteRequiresAuth() throws Exception {
        mockMvc.perform(delete("/api/admin/products/1"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- 認証成功フロー (有効JWT) ----------

    @Test
    @DisplayName("認可: 有効な Bearer JWT を付与すると GET /api/admin/products は 200")
    void adminProductsWithValidJwt() throws Exception {
        String token = jwtUtil.generate("admin");
        when(productService.findAll(any(), anyInt())).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/products")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("認可: 有効な Bearer JWT を付与すると GET /api/admin/orders は 200")
    void adminOrdersWithValidJwt() throws Exception {
        String token = jwtUtil.generate("admin");
        when(orderService.findAll(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/orders")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    // ---------- 認証失敗系 (不正トークン) ----------

    @Test
    @DisplayName("セキュリティ: 改ざん/形式不正トークン → 401")
    void malformedJwtRejected() throws Exception {
        mockMvc.perform(get("/api/admin/orders")
                        .header("Authorization", "Bearer this.is.not.a.real.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("セキュリティ: 別の秘密鍵で署名されたトークンは署名検証で 401")
    void wrongSignatureRejected() throws Exception {
        // 全く別の secret で組み立てた util
        JwtProperties evil = new JwtProperties();
        evil.setSecret("attacker-secret-key-attacker-secret-key-attacker-secret-key-XXX");
        evil.setExpirationMs(60_000);
        String forgedToken = new JwtUtil(evil).generate("admin");

        mockMvc.perform(get("/api/admin/orders")
                        .header("Authorization", "Bearer " + forgedToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("セキュリティ: Authorization ヘッダなし → 401 (= 認証必須エンドポイントに無記名アクセス禁止)")
    void noAuthHeader() throws Exception {
        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("セキュリティ: Bearer prefix なしのトークン → 401 (拾わない)")
    void missingBearerPrefix() throws Exception {
        String token = jwtUtil.generate("admin");
        mockMvc.perform(get("/api/admin/orders")
                        .header("Authorization", token)) // Bearer なし
                .andExpect(status().isUnauthorized());
    }
}
