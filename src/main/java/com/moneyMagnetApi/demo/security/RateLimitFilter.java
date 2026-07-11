package com.moneyMagnetApi.demo.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build();


    private Bucket createNewBucket(int limitNumber) {
        Bandwidth limit = Bandwidth.simple(limitNumber, Duration.ofMinutes(1));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        // Cabeçalhos como X-Forwarded-For são controláveis pelo cliente.
        // O proxy reverso deve limitar o acesso direto à aplicação e repassar
        // o IP confiável como endereço remoto da conexão.
        return request.getRemoteAddr();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        String ip = resolveClientIp(request);
        String key;

        int limit;

        if (request.getRequestURI().contains("/auth")) {
            key = ip + ":auth";
            limit = 7;
        } else {
            key = ip;
            limit = 40;
        }

        Bucket bucket = buckets.get(key, k -> createNewBucket(limit));
        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");

            String body = """
            {
              "status": 429,
              "error": "Too Many Requests",
              "message": "Token inválido ou expirado",
              "timestamp": "%s"
            }
            """.formatted(java.time.Instant.now());

            response.getWriter().write(body);

        }
    }
}

