package com.moneyMagnetApi.demo.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateEmailDTO(

        @NotBlank(message = "O email é obrigatório")
        @Email(message = "O email deve possuir um formato válido")
        String email

) {}
