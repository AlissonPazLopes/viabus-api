package com.viabus.api.dto;

import com.viabus.api.domain.entity.Rota;

import java.io.Serializable;
import java.math.BigDecimal;

public record RotaResponseDTO(
        Long id,
        CidadeResponseDTO cidadeOrigem,
        CidadeResponseDTO cidadeDestino,
        BigDecimal distanciaKm,
        Integer tempoEstimadoMinutos,
        Boolean ativa
) implements Serializable {

    public static RotaResponseDTO fromEntity(Rota rota) {
        return new RotaResponseDTO(
                rota.getId(),
                CidadeResponseDTO.fromEntity(rota.getCidadeOrigem()),
                CidadeResponseDTO.fromEntity(rota.getCidadeDestino()),
                rota.getDistanciaKm(),
                rota.getTempoEstimadoMinutos(),
                rota.getAtiva()
        );
    }
}
