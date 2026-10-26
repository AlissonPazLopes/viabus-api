package com.viabus.api.dto;

import com.viabus.api.domain.entity.Reserva;
import com.viabus.api.domain.enums.StatusAssento;
import com.viabus.api.domain.enums.StatusReserva;
import com.viabus.api.domain.enums.TipoTarifa;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReservaResponse(
        Long id,
        String codigoReserva,
        Long viagemId,
        CidadeResponseDTO cidadeOrigem,
        CidadeResponseDTO cidadeDestino,
        LocalDateTime dataHoraPartida,
        Long passageiroId,
        String nomePassageiro,
        String cpfPassageiro,
        Long assentoViagemId,
        Integer numeroAssento,
        StatusAssento statusAssento,
        TipoTarifa tipoTarifa,
        BigDecimal valorOriginal,
        BigDecimal valorFinal,
        StatusReserva status,
        LocalDateTime dataReserva,
        LocalDateTime dataExpiracao,
        LocalDateTime dataConfirmacao,
        Long versao
) implements Serializable {

    public static ReservaResponse fromEntity(Reserva reserva) {
        return new ReservaResponse(
                reserva.getId(),
                reserva.getCodigoReserva(),
                reserva.getViagem().getId(),
                CidadeResponseDTO.fromEntity(reserva.getViagem().getRota().getCidadeOrigem()),
                CidadeResponseDTO.fromEntity(reserva.getViagem().getRota().getCidadeDestino()),
                reserva.getViagem().getDataHoraPartida(),
                reserva.getPassageiro().getId(),
                reserva.getPassageiro().getNome(),
                reserva.getPassageiro().getCpf(),
                reserva.getAssentoViagem().getId(),
                reserva.getAssentoViagem().getNumeroAssento(),
                reserva.getAssentoViagem().getStatus(),
                reserva.getTipoTarifa(),
                reserva.getValorOriginal(),
                reserva.getValorFinal(),
                reserva.getStatus(),
                reserva.getDataReserva(),
                reserva.getDataExpiracao(),
                reserva.getDataConfirmacao(),
                reserva.getVersao()
        );
    }
}
