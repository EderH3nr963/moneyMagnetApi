package com.moneyMagnetApi.demo.security;

import com.moneyMagnetApi.demo.dto.auditLog.AuditLogEntry;
import com.moneyMagnetApi.demo.mappers.AuditLogMapper;
import com.moneyMagnetApi.demo.service.AuditLogService;
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
            
            AuditLogEntry auditLogEntry = AuditLogMapper.toEntry(request, response, durationMs);

            auditLogService.save(auditLogEntry);
        } catch (Exception exception) {
            LOGGER.warn("Nao foi possivel gravar audit log: {}", exception.getMessage());
        }
    }
    
   

    
}
