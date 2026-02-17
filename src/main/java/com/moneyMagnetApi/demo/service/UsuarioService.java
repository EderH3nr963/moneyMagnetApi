package com.moneyMagnetApi.demo.service;

import com.moneyMagnetApi.demo.domain.usuario.Usuario;
import com.moneyMagnetApi.demo.dto.request.UpdateEmailAndUsernameDTO;
import com.moneyMagnetApi.demo.dto.request.UpdateEmailDTO;
import com.moneyMagnetApi.demo.dto.request.UpdatePasswordDTO;
import com.moneyMagnetApi.demo.dto.request.UpdateUsernameDTO;
import com.moneyMagnetApi.demo.dto.response.UsuarioResponseDTO;
import com.moneyMagnetApi.demo.exception.BusinessException;
import com.moneyMagnetApi.demo.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDTO getById(UUID id) {

        Usuario usuario = findUsuarioOrThrow(id);

        return UsuarioResponseDTO.fromUsuario(usuario);
    }

    @Transactional
    public UsuarioResponseDTO updateEmailAndUsername(UUID id, UpdateEmailAndUsernameDTO dto) {
        Usuario usuario = findUsuarioOrThrow(id);

        if (!usuario.getEmail().equals(dto.email())
                && usuarioRepository.existsByEmail(dto.email())) {
            throw new BusinessException("O email informado já está em uso", HttpStatus.CONFLICT);
        }
        if (!usuario.getUsername().equals(dto.username())
                && usuarioRepository.existsByUsername(dto.username())) {
            throw new BusinessException("O nome de usuário informado já está em uso", HttpStatus.CONFLICT);
        }

        System.out.println("to aqui");

        usuario.setUsername(dto.username());
        usuario.setEmail(dto.email());

        return UsuarioResponseDTO.fromUsuario(usuario);
    }

    @Transactional
    public UsuarioResponseDTO updateEmail(UUID id, UpdateEmailDTO dto) {

        Usuario usuario = findUsuarioOrThrow(id);

        if (!usuario.getEmail().equals(dto.email())
                && usuarioRepository.existsByEmail(dto.email())) {
            throw new BusinessException("O email informado já está em uso", HttpStatus.CONFLICT);
        }

        usuario.setEmail(dto.email());

        return UsuarioResponseDTO.fromUsuario(usuario);
    }

    @Transactional
    public UsuarioResponseDTO updateUsername(UUID id, UpdateUsernameDTO dto) {

        Usuario usuario = findUsuarioOrThrow(id);

        if (!usuario.getUsername().equals(dto.username())
                && usuarioRepository.existsByUsername(dto.username())) {
            throw new BusinessException("O nome de usuário informado já está em uso", HttpStatus.CONFLICT);
        }

        usuario.setUsername(dto.username());

        return UsuarioResponseDTO.fromUsuario(usuario);
    }

    @Transactional
    public void updatePassword(UUID id, UpdatePasswordDTO dto) {

        Usuario usuario = findUsuarioOrThrow(id);

        if (!passwordEncoder.matches(dto.currentPassword(), usuario.getPassword())) {
            throw new ValidationException("A senha atual informada é inválida");
        }

        if (!dto.password().equals(dto.confirmPassword())) {
            throw new ValidationException("A nova senha e a confirmação não coincidem");
        }

        String novaSenhaHash = passwordEncoder.encode(dto.password());
        usuario.setPassword(novaSenhaHash);
    }

    @Transactional
    public void deleteById(UUID id) {

        Usuario usuario = findUsuarioOrThrow(id);

        usuario.setEmail(usuario.getEmail() + "#deleted_" + usuario.getId());
        usuario.setUsername(usuario.getUsername() + "#deleted_" + usuario.getId());

        usuarioRepository.delete(usuario);
    }

    private Usuario findUsuarioOrThrow(UUID id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Usuário não encontrado")
                );
    }
}
