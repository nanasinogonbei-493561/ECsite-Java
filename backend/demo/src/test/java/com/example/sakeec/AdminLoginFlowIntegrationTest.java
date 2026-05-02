package com.example.sakeec;

import com.example.sakeec.service.OrderService;
import com.example.sakeec.service.ProductService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 「DevAdminInitializer が dev admin を投入 → POST /api/admin/login で JWT 取得 →
 * Bearer 付与で管理 API にアクセス」が一気通貫で通ることを確認する統合テスト。
 *
 * <p>これが緑なら本番の認証フローは確立している (ユーザ DB / トークン署名 / フィルタ /
 * EntryPoint / SecurityConfig がすべて噛み合っている)。
 */
@SpringBootTest
@AutoConfigureMockMvc
class AdminLoginFlowIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper om;

    @MockitoBean private ProductService productService;
    @MockitoBean private OrderService orderService;

    @Test
    @DisplayName("E2E: ログイン → トークン取得 → トークン付きで /api/admin/products にアクセスできる")
    void loginThenAccessAdminApi() throws Exception {
        // 1) ログイン (DevAdminInitializer が admin/admin1234 を seed している)
        String loginBody = om.writeValueAsString(Map.of("username", "admin", "password", "admin1234"));
        MvcResult loginResult = mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        JsonNode json = om.readTree(loginResult.getResponse().getContentAsString());
        String token = json.get("token").asText();
        assertThat(token).isNotBlank();

        // 2) トークン付きで管理 API にアクセス
        when(productService.findAll(any(), anyInt())).thenReturn(List.of());
        mockMvc.perform(get("/api/admin/products")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("E2E: 間違ったパスワードでログイン失敗 → AUTH_FAILED, 詳細を漏らさない固定メッセージ")
    void loginFailsWithWrongPassword() throws Exception {
        String loginBody = om.writeValueAsString(Map.of("username", "admin", "password", "wrong-password"));

        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("AUTH_FAILED"))
                .andExpect(jsonPath("$.message").value("ユーザー名またはパスワードが正しくありません"));
    }

    @Test
    @DisplayName("E2E: 存在しないユーザでログイン失敗 → AUTH_FAILED で同じレスポンス (列挙攻撃対策)")
    void loginFailsWithUnknownUser() throws Exception {
        String loginBody = om.writeValueAsString(Map.of("username", "ghost", "password", "anything"));

        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("AUTH_FAILED"))
                .andExpect(jsonPath("$.message").value("ユーザー名またはパスワードが正しくありません"));
    }
}
