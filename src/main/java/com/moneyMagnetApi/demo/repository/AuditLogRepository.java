package com.moneyMagnetApi.demo.repository;

import com.moneyMagnetApi.demo.domain.auditLog.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
}
