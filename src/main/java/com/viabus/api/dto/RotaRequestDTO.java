package com.viabus.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record RotaRequestDTO(
        @NotNull(message = "A cidade de origem é obrigatória")
        Long cidadeOrigemId,

        @NotNull(message = "A cidade de destino é obrigatória")
        Long cidadeDestinoId,

        @NotNull(message = "A distância em km é obrigatória")
        @Positive(message = "A distância em km deve ser maior que zero")
        BigDecimal distanciaKm,

        @NotNull(message = "O tempo estimado em minutos é obrigatório")
        @Positive(message = "O tempo estimado em minutos deve ser maior que zero")
        Integer tempoEstimadoMinutos,

        Boolean ativa
) {
}
