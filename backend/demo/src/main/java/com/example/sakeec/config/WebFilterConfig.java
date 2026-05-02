// src/main/java/com/example/sakeec/config/WebFilterConfig.java
package com.example.sakeec.config;

import com.example.sakeec.logging.TraceIdFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebFilterConfig {

    @Bean
    public TraceIdFilter traceIdFilter() {            // ← 明示的にインスタンス化
        return new TraceIdFilter();
    }
    
    @Bean
    public FilterRegistrationBean<TraceIdFilter> traceIdFilterRegistration(TraceIdFilter filter) {
        FilterRegistrationBean<TraceIdFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(filter);
        reg.addUrlPatterns("/*"); // 全エンドポイントに適用
        reg.setOrder(1);
        return reg;
    }
}
