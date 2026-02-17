package com.moneyMagnetApi.demo.controller;

import com.moneyMagnetApi.demo.dto.request.UpdateEmailAndUsernameDTO;
import com.moneyMagnetApi.demo.dto.request.UpdateEmailDTO;
import com.moneyMagnetApi.demo.dto.request.UpdatePasswordDTO;
import com.moneyMagnetApi.demo.dto.request.UpdateUsernameDTO;
import com.moneyMagnetApi.demo.dto.response.UsuarioResponseDTO;
import com.moneyMagnetApi.demo.security.UsuarioDetailsImpl;
import com.moneyMagnetApi.demo.service.UsuarioService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(
        name = "Usuario",
        description = "Rotas para usuários manipularem seus dados"
)
@RequestMapping("/api/v1/profile")
public class UsuarioController {
    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioResponseDTO> getById(
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioDetails
    ) {
        UsuarioResponseDTO responseDTO = usuarioService.getById(usuarioDetails.getId());

        return ResponseEntity.ok().body(responseDTO);
    }

    @PatchMapping("/username/and/email")
    public ResponseEntity<UsuarioResponseDTO> updateUsernameAndEmail(
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioDetails,
            @RequestBody @Valid UpdateEmailAndUsernameDTO dto
    ) {
        UsuarioResponseDTO responseDTO = usuarioService.updateEmailAndUsername(usuarioDetails.getId(), dto);

        return ResponseEntity.ok().body(responseDTO);
    }

    @PatchMapping("/email")
    public ResponseEntity<UsuarioResponseDTO> updateEmail(
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioDetails,
            @RequestBody @Valid UpdateEmailDTO dto
    ) {
        UsuarioResponseDTO responseDTO = usuarioService.updateEmail(usuarioDetails.getId(), dto);

        return ResponseEntity.ok().body(responseDTO);
    }

    @PatchMapping("/username")
    public ResponseEntity<UsuarioResponseDTO> updateUsername(
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioDetails,
            @RequestBody @Valid UpdateUsernameDTO dto
    ) {
        UsuarioResponseDTO responseDTO = usuarioService.updateUsername(usuarioDetails.getId(), dto);

        return ResponseEntity.ok().body(responseDTO);
    }

    @PatchMapping("/password")
    public ResponseEntity<Void> updatePassword(
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioDetails,
            @RequestBody @Valid UpdatePasswordDTO dto
    ) {
        usuarioService.updatePassword(usuarioDetails.getId(), dto);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioDetails
    ) {
        usuarioService.deleteById(usuarioDetails.getId());

        return ResponseEntity.noContent().build();
    }
}
