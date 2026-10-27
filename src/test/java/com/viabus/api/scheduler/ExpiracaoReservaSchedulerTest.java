package com.viabus.api.scheduler;

import com.viabus.api.domain.entity.AssentoViagem;
import com.viabus.api.domain.entity.Cidade;
import com.viabus.api.domain.entity.Onibus;
import com.viabus.api.domain.entity.Passageiro;
import com.viabus.api.domain.entity.Reserva;
import com.viabus.api.domain.entity.Rota;
import com.viabus.api.domain.entity.Viagem;
import com.viabus.api.domain.enums.StatusAssento;
import com.viabus.api.domain.enums.StatusReserva;
import com.viabus.api.domain.enums.StatusViagem;
import com.viabus.api.domain.enums.TipoLeito;
import com.viabus.api.domain.enums.TipoTarifa;
import com.viabus.api.repository.AssentoViagemRepository;
import com.viabus.api.repository.ReservaRepository;
import com.viabus.api.repository.ViagemRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpiracaoReservaSchedulerTest {

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private AssentoViagemRepository assentoViagemRepository;

    @Mock
    private ViagemRepository viagemRepository;

    @InjectMocks
    private ExpiracaoReservaScheduler expiracaoReservaScheduler;

    @Test
    @DisplayName("Deve expirar reservas vencidas, liberar assento para LIVRE e devolver cota de desconto do ID Jovem")
    void deveExpirarReservaLiberarAssentoEDevolverCotaIdJovem() {
        Cidade origem = Cidade.builder().id(1L).nome("Salvador").uf("BA").build();
        Cidade destino = Cidade.builder().id(2L).nome("Aracaju").uf("SE").build();
        Rota rota = Rota.builder()
                .id(10L)
                .cidadeOrigem(origem)
                .cidadeDestino(destino)
                .distanciaKm(new BigDecimal("320.00"))
                .tempoEstimadoMinutos(280)
                .ativa(true)
                .build();

        Onibus onibus = Onibus.builder()
                .id(5L)
                .placa("VBU6S77")
                .modelo("Marcopolo G8")
                .tipoLeito(TipoLeito.EXECUTIVO)
                .totalAssentos(40)
                .ativo(true)
                .build();

        Viagem viagem = Viagem.builder()
                .id(100L)
                .rota(rota)
                .onibus(onibus)
                .dataHoraPartida(LocalDateTime.now().plusDays(1))
                .dataHoraChegadaPrevista(LocalDateTime.now().plusDays(1).plusHours(5))
                .precoPassagem(new BigDecimal("100.00"))
                .vagasIdJovemGratuitas(2)
                .vagasIdJovemDesconto(1) // 1 vaga estava em uso pela reserva pendente
                .status(StatusViagem.AGENDADA)
                .versao(0L)
                .build();

        AssentoViagem assento = AssentoViagem.builder()
                .id(300L)
                .viagem(viagem)
                .numeroAssento(7)
                .status(StatusAssento.RESERVADO)
                .versao(0L)
                .build();

        Passageiro passageiro = Passageiro.builder()
                .id(20L)
                .nome("Mariana Lima")
                .cpf("12345678901")
                .email("mariana@email.com")
                .dataNascimento(LocalDate.now().minusYears(21))
                .ativo(true)
                .build();

        Reserva reservaVencida = Reserva.builder()
                .id(900L)
                .codigoReserva("VB-EXP12345")
                .viagem(viagem)
                .passageiro(passageiro)
                .assentoViagem(assento)
                .tipoTarifa(TipoTarifa.ID_JOVEM_DESCONTO)
                .valorOriginal(new BigDecimal("100.00"))
                .valorFinal(new BigDecimal("50.00"))
                .status(StatusReserva.PENDENTE_PAGAMENTO)
                .dataReserva(LocalDateTime.now().minusMinutes(25))
                .dataExpiracao(LocalDateTime.now().minusMinutes(10))
                .build();

        when(reservaRepository.findByStatusAndDataExpiracaoBefore(eq(StatusReserva.PENDENTE_PAGAMENTO), any(LocalDateTime.class)))
                .thenReturn(List.of(reservaVencida));

        int totalProcessadas = expiracaoReservaScheduler.processarReservasExpiradas();

        assertThat(totalProcessadas).isEqualTo(1);
        assertThat(reservaVencida.getStatus()).isEqualTo(StatusReserva.EXPIRADA);
        assertThat(assento.getStatus()).isEqualTo(StatusAssento.LIVRE);
        assertThat(viagem.getVagasIdJovemDesconto()).isEqualTo(2);

        verify(assentoViagemRepository).saveAndFlush(assento);
        verify(viagemRepository).saveAndFlush(viagem);
        verify(reservaRepository).saveAndFlush(reservaVencida);
    }
}
