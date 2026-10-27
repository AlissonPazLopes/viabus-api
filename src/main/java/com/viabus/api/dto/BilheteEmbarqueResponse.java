package com.viabus.api.dto;

import com.viabus.api.domain.enums.TipoLeito;
import com.viabus.api.domain.enums.TipoTarifa;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BilheteEmbarqueResponse(
        Long reservaId,
        String codigoReserva,
        String nomePassageiro,
        String cpfMascarado,
        String cidadeOrigem,
        String cidadeDestino,
        LocalDateTime dataHoraPartida,
        LocalDateTime dataHoraChegadaPrevista,
        Integer numeroAssento,
        String placaOnibus,
        String modeloOnibus,
        TipoLeito tipoLeito,
        TipoTarifa tipoTarifa,
        BigDecimal valorPago,
        LocalDateTime dataConfirmacao,
        String hashValidacao
) implements Serializable {
}
