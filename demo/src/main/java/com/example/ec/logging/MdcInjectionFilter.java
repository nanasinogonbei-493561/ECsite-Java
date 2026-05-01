// src/main/java/com/example/ec/logging/MdcInjectionFilter.java
package com.example.ec.logging;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@Order(Integer.MIN_VALUE)
public class MdcInjectionFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        String requestId = UUID.randomUUID().toString();
        HttpServletRequest req = (HttpServletRequest) request;

        try {
            MDC.put("requestId", requestId);
            MDC.put("method", req.getMethod());
            MDC.put("path", req.getRequestURI());
            MDC.put("query", req.getQueryString());
            MDC.put("clientIp", getClientIp(req));
            MDC.put("userAgent", req.getHeader("User-Agent"));

            // 認証済ユーザIDが取れる場合の例（Spring Security等）
            String userId = (req.getUserPrincipal() != null) ? req.getUserPrincipal().getName() : "anonymous";
            MDC.put("userId", userId);

            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    private String getClientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return req.getRemoteAddr();
    }
}
