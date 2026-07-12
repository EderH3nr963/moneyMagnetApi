package com.moneyMagnetApi.demo.repository;

import com.moneyMagnetApi.demo.domain.usuario.UpdateEmail;
import com.moneyMagnetApi.demo.domain.usuario.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UpdateEmailRepository extends JpaRepository<UpdateEmail, UUID> {
    Optional<UpdateEmail> findByTokenHash(String tokenHash);

    void deleteByUsuario(Usuario usuario);
}
