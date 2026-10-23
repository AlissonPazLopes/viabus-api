package com.viabus.api.dto;

import com.viabus.api.domain.entity.Cidade;

import java.io.Serializable;

public record CidadeResponseDTO(
        Long id,
        String nome,
        String uf
) implements Serializable {

    public static CidadeResponseDTO fromEntity(Cidade cidade) {
        return new CidadeResponseDTO(
                cidade.getId(),
                cidade.getNome(),
                cidade.getUf()
        );
    }
}
