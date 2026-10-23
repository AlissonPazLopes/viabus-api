package com.viabus.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CidadeRequestDTO(
        @NotBlank(message = "O nome da cidade é obrigatório")
        @Size(max = 100, message = "O nome da cidade deve ter no máximo 100 caracteres")
        String nome,

        @NotBlank(message = "A UF é obrigatória")
        @Size(min = 2, max = 2, message = "A UF deve conter exatamente 2 caracteres")
        String uf
) {
}
