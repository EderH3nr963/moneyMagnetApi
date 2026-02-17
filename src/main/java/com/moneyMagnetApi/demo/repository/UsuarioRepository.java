package com.moneyMagnetApi.demo.repository;

import com.moneyMagnetApi.demo.domain.usuario.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
    Optional<Usuario> findByEmail(String email);
    Optional<Usuario> findByUsername(String username);

    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
}
