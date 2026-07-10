package com.moneyMagnetApi.demo.security;

import com.moneyMagnetApi.demo.service.AuditLogService;
import com.moneyMagnetApi.demo.service.AuditLogService.AuditLogEntry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class AuditLogFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditLogFilter.class);
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );

    private final AuditLogService auditLogService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long startedAt = System.nanoTime();

        try {
            filterChain.doFilter(request, response);
        } finally {
            saveAuditLog(request, response, startedAt);
        }
    }

    private void saveAuditLog(
            HttpServletRequest request,
            HttpServletResponse response,
            long startedAt
    ) {
        try {
            long durationMs = TimeUnit.NANOSECONDS.toMillis(
                    System.nanoTime() - startedAt
            );
            String route = resolveRoute(request);

            auditLogService.save(new AuditLogEntry(
                    resolveUserId(),
                    request.getMethod(),
                    route,
                    resolveResourceId(request),
                    resolveStatus(response.getStatus()),
                    resolveIp(request),
                    request.getHeader("User-Agent"),
                    metadata(request, response, durationMs)
            ));
        } catch (Exception exception) {
            LOGGER.warn("Nao foi possivel gravar audit log: {}", exception.getMessage());
        }
    }

    private UUID resolveUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UsuarioDetailsImpl details)) {
            return null;
        }

        return details.getId();
    }

    private String resolveRoute(HttpServletRequest request) {
        Object pattern = request.getAttribute(
                HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE
        );

        return pattern == null ? request.getRequestURI() : pattern.toString();
    }

    private UUID resolveResourceId(HttpServletRequest request) {
        String[] segments = request.getRequestURI().split("/");
        for (int index = segments.length - 1; index >= 0; index--) {
            String segment = segments[index];
            if (UUID_PATTERN.matcher(segment).matches()) {
                return UUID.fromString(segment);
            }
        }

        return null;
    }

    private String resolveStatus(int httpStatus) {
        if (httpStatus >= 500) {
            return "SERVER_ERROR";
        }
        if (httpStatus >= 400) {
            return "CLIENT_ERROR";
        }
        if (httpStatus >= 300) {
            return "REDIRECT";
        }
        return "SUCCESS";
    }

    private String resolveIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp;
        }

        return request.getRemoteAddr();
    }

    private String metadata(
            HttpServletRequest request,
            HttpServletResponse response,
            long durationMs
    ) {
        return """
                {"method":"%s","uri":"%s","query":"%s","status":%d,"durationMs":%d}
                """.formatted(
                json(request.getMethod()),
                json(request.getRequestURI()),
                json(request.getQueryString()),
                response.getStatus(),
                durationMs
        ).trim();
    }

    private String json(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }
}
