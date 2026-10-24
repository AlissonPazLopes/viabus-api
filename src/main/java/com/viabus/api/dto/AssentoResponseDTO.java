package com.viabus.api.dto;

import com.viabus.api.domain.entity.AssentoViagem;
import com.viabus.api.domain.enums.StatusAssento;

import java.io.Serializable;

public record AssentoResponseDTO(
        Long id,
        Integer numeroAssento,
        StatusAssento status
) implements Serializable {

    public static AssentoResponseDTO fromEntity(AssentoViagem assento) {
        return new AssentoResponseDTO(
                assento.getId(),
                assento.getNumeroAssento(),
                assento.getStatus()
        );
    }
}
