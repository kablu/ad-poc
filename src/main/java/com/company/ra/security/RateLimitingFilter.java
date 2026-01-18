package com.company.ra.security;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Rate Limiting Filter
 * Implements simple token bucket rate limiting per IP address
 */
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);

    // Maximum requests per time window
    private static final int MAX_REQUESTS_PER_WINDOW = 100;
    // Time window in milliseconds (1 minute)
    private static final long TIME_WINDOW_MS = 60000;

    // Storage for rate limit buckets
    private final Map<String, RateLimitBucket> rateLimitBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {

        String clientIp = getClientIpAddress(request);
        String requestUri = request.getRequestURI();

        // Skip rate limiting for health checks
        if (requestUri.contains("/actuator/health")) {
            filterChain.doFilter(request, response);
            return;
        }

        RateLimitBucket bucket = rateLimitBuckets.computeIfAbsent(clientIp, k -> new RateLimitBucket());

        if (bucket.tryConsume()) {
            // Request allowed
            filterChain.doFilter(request, response);
        } else {
            // Rate limit exceeded
            logger.warn("Rate limit exceeded for IP: {}, URI: {}", clientIp, requestUri);

            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_MISDIRECTED_REQUEST);

            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("success", false);
            errorDetails.put("error", "Too Many Requests");
            errorDetails.put("message", "Rate limit exceeded. Please try again later.");
            errorDetails.put("retryAfter", TIME_WINDOW_MS / 1000);

            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(response.getOutputStream(), errorDetails);
        }
    }

    /**
     * Get client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * Inner class representing a rate limit bucket
     */
    private static class RateLimitBucket {
        private final AtomicInteger tokenCount;
        private long lastRefillTimestamp;

        public RateLimitBucket() {
            this.tokenCount = new AtomicInteger(MAX_REQUESTS_PER_WINDOW);
            this.lastRefillTimestamp = System.currentTimeMillis();
        }

        /**
         * Try to consume a token from the bucket
         *
         * @return true if request allowed, false if rate limit exceeded
         */
        public synchronized boolean tryConsume() {
            refillIfNeeded();

            if (tokenCount.get() > 0) {
                tokenCount.decrementAndGet();
                return true;
            }

            return false;
        }

        /**
         * Refill bucket if time window has elapsed
         */
        private void refillIfNeeded() {
            long now = System.currentTimeMillis();
            if (now - lastRefillTimestamp >= TIME_WINDOW_MS) {
                tokenCount.set(MAX_REQUESTS_PER_WINDOW);
                lastRefillTimestamp = now;
            }
        }
    }
}
