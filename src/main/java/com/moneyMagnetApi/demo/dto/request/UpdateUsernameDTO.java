package com.moneyMagnetApi.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUsernameDTO(

        @NotBlank(message = "O nome de usuário é obrigatório")
        @Size(min = 3, max = 17, message = "O nome de usuário deve ter entre 3 e 17 caracteres")
        String username

) {}
