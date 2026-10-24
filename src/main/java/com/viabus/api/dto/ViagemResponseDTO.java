package com.viabus.api.dto;

import com.viabus.api.domain.entity.Viagem;
import com.viabus.api.domain.enums.StatusViagem;
import com.viabus.api.domain.enums.TipoLeito;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ViagemResponseDTO(
        Long id,
        Long rotaId,
        CidadeResponseDTO cidadeOrigem,
        CidadeResponseDTO cidadeDestino,
        Long onibusId,
        String placaOnibus,
        String modeloOnibus,
        TipoLeito tipoLeito,
        Integer totalAssentos,
        LocalDateTime dataHoraPartida,
        LocalDateTime dataHoraChegadaPrevista,
        BigDecimal precoPassagem,
        Integer vagasIdJovemGratuitas,
        Integer vagasIdJovemDesconto,
        StatusViagem status,
        Long versao
) implements Serializable {

    public static ViagemResponseDTO fromEntity(Viagem viagem) {
        return new ViagemResponseDTO(
                viagem.getId(),
                viagem.getRota().getId(),
                CidadeResponseDTO.fromEntity(viagem.getRota().getCidadeOrigem()),
                CidadeResponseDTO.fromEntity(viagem.getRota().getCidadeDestino()),
                viagem.getOnibus().getId(),
                viagem.getOnibus().getPlaca(),
                viagem.getOnibus().getModelo(),
                viagem.getOnibus().getTipoLeito(),
                viagem.getOnibus().getTotalAssentos(),
                viagem.getDataHoraPartida(),
                viagem.getDataHoraChegadaPrevista(),
                viagem.getPrecoPassagem(),
                viagem.getVagasIdJovemGratuitas(),
                viagem.getVagasIdJovemDesconto(),
                viagem.getStatus(),
                viagem.getVersao()
        );
    }
}
