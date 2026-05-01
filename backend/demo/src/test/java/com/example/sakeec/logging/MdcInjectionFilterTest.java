package com.example.sakeec.logging;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MdcInjectionFilterTest {

    private final MdcInjectionFilter filter = new MdcInjectionFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("X-Trace-Id ヘッダがない場合: UUID が生成され、レスポンスにエコーバックされる")
    void generatesTraceIdIfMissing() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/products");
        req.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        String traceId = res.getHeader(MdcInjectionFilter.TRACE_ID_HEADER);
        assertThat(traceId).isNotNull();
        // UUID 形式 (RFC4122)
        assertThat(traceId).matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
        verify(chain).doFilter(req, res);
    }

    @Test
    @DisplayName("X-Trace-Id 付与時はその値が踏襲され、フロント/上流から traceId を引き継げる")
    void propagatesIncomingTraceId() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/products");
        req.addHeader(MdcInjectionFilter.TRACE_ID_HEADER, "abc-123");
        req.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, mock(FilterChain.class));

        assertThat(res.getHeader(MdcInjectionFilter.TRACE_ID_HEADER)).isEqualTo("abc-123");
    }

    @Test
    @DisplayName("チェーン処理中は MDC に traceId/method/path/clientIp/userId が入っている")
    void mdcPopulatedDuringChain() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/orders");
        req.setQueryString("debug=true");
        req.setRemoteAddr("10.0.0.5");
        req.addHeader("User-Agent", "JUnit/5");
        MockHttpServletResponse res = new MockHttpServletResponse();

        FilterChain chain = mock(FilterChain.class);
        doAnswer(inv -> {
            assertThat(MDC.get(MdcInjectionFilter.TRACE_ID_KEY)).isNotBlank();
            assertThat(MDC.get("method")).isEqualTo("POST");
            assertThat(MDC.get("path")).isEqualTo("/api/orders");
            assertThat(MDC.get("query")).isEqualTo("debug=true");
            assertThat(MDC.get("clientIp")).isEqualTo("10.0.0.5");
            assertThat(MDC.get("userAgent")).isEqualTo("JUnit/5");
            assertThat(MDC.get("userId")).isEqualTo("anonymous");
            return null;
        }).when(chain).doFilter(req, res);

        filter.doFilter(req, res, chain);

        // 終了後はクリアされている
        assertThat(MDC.get(MdcInjectionFilter.TRACE_ID_KEY)).isNull();
        assertThat(MDC.get("method")).isNull();
    }

    @Test
    @DisplayName("X-Forwarded-For 先頭の IP が clientIp に採用される (LB背後想定)")
    void clientIpFromXff() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/products");
        req.addHeader("X-Forwarded-For", "203.0.113.5, 10.0.0.1");
        req.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse res = new MockHttpServletResponse();

        FilterChain chain = mock(FilterChain.class);
        doAnswer(inv -> {
            assertThat(MDC.get("clientIp")).isEqualTo("203.0.113.5");
            return null;
        }).when(chain).doFilter(req, res);

        filter.doFilter(req, res, chain);
    }

    @Test
    @DisplayName("運用: 例外発生時でも MDC は finally でクリアされ、リーク (前リクエストの traceId 流用) が起きない")
    void mdcClearedOnException() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/products");
        req.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse res = new MockHttpServletResponse();

        FilterChain chain = mock(FilterChain.class);
        doAnswer(inv -> { throw new RuntimeException("boom"); }).when(chain).doFilter(req, res);

        try {
            filter.doFilter(req, res, chain);
        } catch (RuntimeException ignored) {
        }
        assertThat(MDC.get(MdcInjectionFilter.TRACE_ID_KEY)).isNull();
    }
}
