package com.viabus.api.dto;

import com.viabus.api.domain.entity.Onibus;
import com.viabus.api.domain.enums.TipoLeito;

import java.io.Serializable;

public record OnibusResponseDTO(
        Long id,
        String placa,
        String modelo,
        TipoLeito tipoLeito,
        Integer totalAssentos,
        Boolean ativo
) implements Serializable {

    public static OnibusResponseDTO fromEntity(Onibus onibus) {
        return new OnibusResponseDTO(
                onibus.getId(),
                onibus.getPlaca(),
                onibus.getModelo(),
                onibus.getTipoLeito(),
                onibus.getTotalAssentos(),
                onibus.getAtivo()
        );
    }
}
