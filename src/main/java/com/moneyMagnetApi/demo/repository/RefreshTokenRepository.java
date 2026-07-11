package com.moneyMagnetApi.demo.repository;

import com.moneyMagnetApi.demo.domain.usuario.RefreshToken;
import com.moneyMagnetApi.demo.domain.usuario.Usuario;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    void deleteAllByUsuario(Usuario usuario);
}
