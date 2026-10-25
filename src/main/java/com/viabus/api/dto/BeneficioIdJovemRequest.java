package com.viabus.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record BeneficioIdJovemRequest(

        @NotBlank(message = "O número do cartão ID Jovem é obrigatório.")
        @Size(max = 30, message = "O número do ID Jovem deve ter no máximo 30 caracteres.")
        String numeroIdJovem,

        @NotNull(message = "A data de validade do cartão ID Jovem é obrigatória.")
        LocalDate validadeIdJovem
) {
}
