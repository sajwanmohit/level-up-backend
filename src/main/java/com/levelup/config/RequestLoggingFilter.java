package com.levelup.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final String REQUEST_ID_MDC_KEY = "requestId";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString();
        long startTime = System.nanoTime();
        String previousRequestId = MDC.get(REQUEST_ID_MDC_KEY);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMillis = (System.nanoTime() - startTime) / 1_000_000;
            int status = response.getStatus();
            if (status >= 500) {
                logger.error("http_request method={} path={} status={} duration_ms={} request_id={}",
                        request.getMethod(), request.getRequestURI(), status, durationMillis, requestId);
            } else if (status >= 400) {
                logger.warn("http_request method={} path={} status={} duration_ms={} request_id={}",
                        request.getMethod(), request.getRequestURI(), status, durationMillis, requestId);
            } else {
                logger.info("http_request method={} path={} status={} duration_ms={} request_id={}",
                        request.getMethod(), request.getRequestURI(), status, durationMillis, requestId);
            }
            if (previousRequestId == null) MDC.remove(REQUEST_ID_MDC_KEY);
            else MDC.put(REQUEST_ID_MDC_KEY, previousRequestId);
        }
    }
}
