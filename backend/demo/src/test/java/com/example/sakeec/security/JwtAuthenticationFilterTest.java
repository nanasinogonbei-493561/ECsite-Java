package com.example.sakeec.security;

import com.example.sakeec.config.JwtProperties;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class JwtAuthenticationFilterTest {

    private JwtUtil jwtUtil;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setup() {
        JwtProperties p = new JwtProperties();
        p.setSecret("test-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm-test");
        p.setExpirationMs(60_000);
        jwtUtil = new JwtUtil(p);
        filter = new JwtAuthenticationFilter(jwtUtil);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("有効な Bearer JWT: SecurityContext に認証がセットされる")
    void validBearer() throws Exception {
        String token = jwtUtil.generate("admin");
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/admin/products");
        req.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("admin");
        assertThat(auth.getAuthorities()).extracting(Object::toString).contains("ROLE_ADMIN");
        verify(chain).doFilter(req, res);
    }

    @Test
    @DisplayName("Authorization ヘッダなし: SecurityContext は空のまま")
    void noAuthHeader() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/admin/products");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(req, res);
    }

    @Test
    @DisplayName("セキュリティ: Bearer prefix なし → SecurityContext は空 (生トークンを誤認証しない)")
    void noBearerPrefix() throws Exception {
        String token = jwtUtil.generate("admin");
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/admin/products");
        req.addHeader("Authorization", token); // Bearer なし
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, mock(FilterChain.class));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("セキュリティ: 不正トークン → SecurityContext は空、フィルタチェーンは継続")
    void invalidToken() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/admin/products");
        req.addHeader("Authorization", "Bearer not-a-real-jwt");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(req, res);
    }

    @Test
    @DisplayName("セキュリティ: 空 Bearer トークン (Bearer のみ) → SecurityContext は空")
    void emptyBearer() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/admin/products");
        req.addHeader("Authorization", "Bearer ");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, mock(FilterChain.class));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
