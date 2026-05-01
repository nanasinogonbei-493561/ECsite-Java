package com.example.sakeec.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * 全リクエストに traceId と関連メタ情報を MDC で付与し、
 * 完了時にアクセスログを 1 行出力する。
 * 例外は GlobalExceptionHandler が拾う前に finally でログ化されるよう、
 * Spring 標準の例外解決より前段で動く。
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcInjectionFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String TRACE_ID_KEY    = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws IOException, ServletException {

        long start = System.currentTimeMillis();

        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }

        MDC.put(TRACE_ID_KEY, traceId);
        MDC.put("method",     request.getMethod());
        MDC.put("path",       request.getRequestURI());
        if (request.getQueryString() != null) {
            MDC.put("query", request.getQueryString());
        }
        MDC.put("clientIp",  resolveClientIp(request));
        String ua = request.getHeader("User-Agent");
        if (ua != null) MDC.put("userAgent", ua);
        MDC.put("userId",
                request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "anonymous");

        response.setHeader(TRACE_ID_HEADER, traceId);

        try {
            chain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - start;
            int status = response.getStatus();
            // status を MDC にも入れて他のログとの相関を取りやすくする
            MDC.put("status", String.valueOf(status));

            if (status >= 500) {
                log.error("REQUEST_COMPLETED",
                        kv("event", "REQUEST_COMPLETED"),
                        kv("status", status),
                        kv("durationMs", durationMs));
            } else if (status >= 400) {
                log.warn("REQUEST_COMPLETED",
                        kv("event", "REQUEST_COMPLETED"),
                        kv("status", status),
                        kv("durationMs", durationMs));
            } else {
                log.info("REQUEST_COMPLETED",
                        kv("event", "REQUEST_COMPLETED"),
                        kv("status", status),
                        kv("durationMs", durationMs));
            }
            MDC.clear();
        }
    }

    private String resolveClientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }
}
