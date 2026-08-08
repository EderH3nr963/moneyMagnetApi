package com.moneyMagnetApi.demo.dto.auditLog;

import java.util.UUID;

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