package com.moneyMagnetApi.demo.repository;

import com.moneyMagnetApi.demo.domain.institution.Institution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InstitutionRepository extends JpaRepository<Institution, UUID> {
    Optional<Institution> findByPluggyConnectorId(String pluggyConnectorId);
}
