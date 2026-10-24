package com.viabus.api.dto;

import com.viabus.api.domain.entity.Viagem;
import com.viabus.api.domain.enums.StatusAssento;
import com.viabus.api.domain.enums.StatusViagem;
import com.viabus.api.domain.enums.TipoLeito;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record MapaAssentoResponse(
        Long viagemId,
        CidadeResponseDTO cidadeOrigem,
        CidadeResponseDTO cidadeDestino,
        LocalDateTime dataHoraPartida,
        LocalDateTime dataHoraChegadaPrevista,
        String placaOnibus,
        String modeloOnibus,
        TipoLeito tipoLeito,
        BigDecimal precoPassagem,
        StatusViagem statusViagem,
        Integer totalAssentos,
        Integer assentosLivres,
        Integer vagasIdJovemGratuitas,
        Integer vagasIdJovemDesconto,
        List<AssentoResponseDTO> assentos
) implements Serializable {

    public static MapaAssentoResponse of(Viagem viagem, List<AssentoResponseDTO> assentos) {
        int livres = (int) assentos.stream()
                .filter(a -> a.status() == StatusAssento.LIVRE)
                .count();

        return new MapaAssentoResponse(
                viagem.getId(),
                CidadeResponseDTO.fromEntity(viagem.getRota().getCidadeOrigem()),
                CidadeResponseDTO.fromEntity(viagem.getRota().getCidadeDestino()),
                viagem.getDataHoraPartida(),
                viagem.getDataHoraChegadaPrevista(),
                viagem.getOnibus().getPlaca(),
                viagem.getOnibus().getModelo(),
                viagem.getOnibus().getTipoLeito(),
                viagem.getPrecoPassagem(),
                viagem.getStatus(),
                viagem.getOnibus().getTotalAssentos(),
                livres,
                viagem.getVagasIdJovemGratuitas(),
                viagem.getVagasIdJovemDesconto(),
                assentos
        );
    }
}
