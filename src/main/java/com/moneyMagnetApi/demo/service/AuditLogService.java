package com.moneyMagnetApi.demo.service;

import com.moneyMagnetApi.demo.domain.auditLog.AuditLog;
import com.moneyMagnetApi.demo.domain.usuario.Usuario;
import com.moneyMagnetApi.demo.repository.AuditLogRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private static final int ACTION_LIMIT = 100;
    private static final int RESOURCE_LIMIT = 100;
    private static final int STATUS_LIMIT = 30;
    private static final int IP_LIMIT = 45;

    private final AuditLogRepository auditLogRepository;
    private final EntityManager entityManager;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(AuditLogEntry entry) {
        Usuario usuario = entry.userId() == null
                ? null
                : entityManager.getReference(Usuario.class, entry.userId());

        AuditLog auditLog = AuditLog.builder()
                .usuario(usuario)
                .action(limit(entry.action(), ACTION_LIMIT))
                .resource(limit(entry.resource(), RESOURCE_LIMIT))
                .resourceId(entry.resourceId())
                .status(limit(entry.status(), STATUS_LIMIT))
                .ip(limit(entry.ip(), IP_LIMIT))
                .userAgent(entry.userAgent())
                .metadata(entry.metadata())
                .build();

        auditLogRepository.save(auditLog);
    }

    private String limit(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return "UNKNOWN";
        }

        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    public record AuditLogEntry(
            UUID userId,
            String action,
            String resource,
            UUID resourceId,
            String status,
            String ip,
            String userAgent,
            String metadata
    ) {
    }
}
