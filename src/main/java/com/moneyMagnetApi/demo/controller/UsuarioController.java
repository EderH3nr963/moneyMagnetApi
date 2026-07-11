package com.moneyMagnetApi.demo.controller;

import com.moneyMagnetApi.demo.dto.usuario.request.UpdateEmailAndUsernameDTO;
import com.moneyMagnetApi.demo.dto.usuario.request.UpdatePasswordDTO;
import com.moneyMagnetApi.demo.dto.usuario.request.UpdateThemeDTO;
import com.moneyMagnetApi.demo.dto.usuario.response.UsuarioResponseDTO;
import com.moneyMagnetApi.demo.security.UsuarioDetailsImpl;
import com.moneyMagnetApi.demo.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
    @Operation(
            summary = "Retorna o perfil autenticado",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Perfil retornado"),
                    @ApiResponse(responseCode = "401", description = "Token ausente ou invalido")
            }
    )
    public ResponseEntity<UsuarioResponseDTO> getById(
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioDetails
    ) {
        UsuarioResponseDTO responseDTO = usuarioService.getById(usuarioDetails.getId());

        return ResponseEntity.ok().body(responseDTO);
    }

    @PatchMapping("/username/and/email")
    @Operation(
            summary = "Atualiza nome de usuario e e-mail",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Perfil atualizado"),
                    @ApiResponse(responseCode = "400", description = "Dados invalidos"),
                    @ApiResponse(responseCode = "401", description = "Token ausente ou invalido")
            }
    )
    public ResponseEntity<UsuarioResponseDTO> updateUsernameAndEmail(
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioDetails,
            @RequestBody @Valid UpdateEmailAndUsernameDTO dto
    ) {
        UsuarioResponseDTO responseDTO = usuarioService.updateEmailAndUsername(usuarioDetails.getId(), dto);

        return ResponseEntity.ok().body(responseDTO);
    }

    @PatchMapping("/password")
    @Operation(
            summary = "Atualiza a senha do usuario autenticado",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Senha atualizada"),
                    @ApiResponse(responseCode = "400", description = "Dados invalidos"),
                    @ApiResponse(responseCode = "401", description = "Token ausente ou invalido")
            }
    )
    public ResponseEntity<Void> updatePassword(
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioDetails,
            @RequestBody @Valid UpdatePasswordDTO dto
    ) {
        usuarioService.updatePassword(usuarioDetails.getId(), dto);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/theme")
    @Operation(
            summary = "Atualiza preferencia de tema",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Tema atualizado"),
                    @ApiResponse(responseCode = "400", description = "Tema invalido"),
                    @ApiResponse(responseCode = "401", description = "Token ausente ou invalido")
            }
    )
    public ResponseEntity<UsuarioResponseDTO> updateTheme(
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioDetails,
            @RequestBody @Valid UpdateThemeDTO dto
    ) {
        UsuarioResponseDTO responseDTO = usuarioService.updateTheme(usuarioDetails.getId(), dto);

        return ResponseEntity.ok().body(responseDTO);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Remove a conta do usuario autenticado",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Conta removida"),
                    @ApiResponse(responseCode = "401", description = "Token ausente ou invalido")
            }
    )
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UsuarioDetailsImpl usuarioDetails
    ) {
        usuarioService.deleteById(usuarioDetails.getId());

        return ResponseEntity.noContent().build();
    }
}
