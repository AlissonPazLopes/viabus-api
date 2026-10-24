package com.viabus.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ViagemRequestDTO(
        @NotNull(message = "A rota é obrigatória")
        Long rotaId,

        @NotNull(message = "O ônibus é obrigatório")
        Long onibusId,

        @NotNull(message = "A data e hora de partida são obrigatórias")
        LocalDateTime dataHoraPartida,

        @NotNull(message = "O preço da passagem é obrigatório")
        @Positive(message = "O preço da passagem deve ser maior que zero")
        BigDecimal precoPassagem
) {
}
