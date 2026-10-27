package com.viabus.api.scheduler;

import com.viabus.api.domain.entity.AssentoViagem;
import com.viabus.api.domain.entity.Reserva;
import com.viabus.api.domain.entity.Viagem;
import com.viabus.api.domain.enums.StatusAssento;
import com.viabus.api.domain.enums.StatusReserva;
import com.viabus.api.domain.enums.TipoTarifa;
import com.viabus.api.repository.AssentoViagemRepository;
import com.viabus.api.repository.ReservaRepository;
import com.viabus.api.repository.ViagemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExpiracaoReservaScheduler {

    private final ReservaRepository reservaRepository;
    private final AssentoViagemRepository assentoViagemRepository;
    private final ViagemRepository viagemRepository;

    @Scheduled(fixedDelayString = "${viabus.scheduler.expiracao-ms:60000}")
    @Transactional
    public void executarRotinaAgendada() {
        int processadas = processarReservasExpiradas();
        if (processadas > 0) {
            log.info("Job de expiração concluído: {} reserva(s) expirada(s) e poltrona(s) liberada(s).", processadas);
        }
    }

    @Transactional
    public int processarReservasExpiradas() {
        LocalDateTime agora = LocalDateTime.now();
        List<Reserva> vencidas = reservaRepository.findByStatusAndDataExpiracaoBefore(
                StatusReserva.PENDENTE_PAGAMENTO,
                agora
        );

        for (Reserva reserva : vencidas) {
            reserva.setStatus(StatusReserva.EXPIRADA);

            AssentoViagem assento = reserva.getAssentoViagem();
            assento.setStatus(StatusAssento.LIVRE);
            assentoViagemRepository.saveAndFlush(assento);

            Viagem viagem = reserva.getViagem();
            estornarCotaSocialSeAplicavel(viagem, reserva.getTipoTarifa());
            viagemRepository.saveAndFlush(viagem);

            reservaRepository.saveAndFlush(reserva);
        }

        return vencidas.size();
    }

    private void estornarCotaSocialSeAplicavel(Viagem viagem, TipoTarifa tipoTarifa) {
        if (tipoTarifa == TipoTarifa.ID_JOVEM_GRATUITO) {
            viagem.setVagasIdJovemGratuitas(viagem.getVagasIdJovemGratuitas() + 1);
        } else if (tipoTarifa == TipoTarifa.ID_JOVEM_DESCONTO) {
            viagem.setVagasIdJovemDesconto(viagem.getVagasIdJovemDesconto() + 1);
        }
    }
}
