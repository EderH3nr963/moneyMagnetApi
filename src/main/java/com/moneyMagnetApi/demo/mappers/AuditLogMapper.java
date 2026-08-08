package com.moneyMagnetApi.demo.mappers;

import com.moneyMagnetApi.demo.dto.auditLog.AuditLogEntry;
import com.moneyMagnetApi.demo.security.UsuarioDetailsImpl;
import com.moneyMagnetApi.demo.utils.ResolveUserId;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerMapping;

import java.util.UUID;

public class AuditLogMapper {
    public static AuditLogEntry toEntry(HttpServletRequest request, HttpServletResponse response, long durationMs) {
        return new AuditLogEntry(
                ResolveUserId.resolve(),
                request.getMethod(),
                resolveRoute(request),
                null,
                resolveStatus(response.getStatus()),
                request.getRemoteAddr(),
                request.getHeader("User-Agent"),
                metadata(request, response, durationMs)
        );
    }
    
    public static String resolveRoute(HttpServletRequest request) {
        Object pattern = request.getAttribute(
                HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE
        );
        
        return pattern == null ? request.getRequestURI() : pattern.toString();
    }
    
    public static String resolveStatus(int httpStatus) {
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
    
    public static String resolveIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
    
    public static String metadata(
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
    
    public static String json(String value) {
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
