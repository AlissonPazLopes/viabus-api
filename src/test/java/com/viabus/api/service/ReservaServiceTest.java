package com.viabus.api.service;

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
import com.viabus.api.dto.ElegibilidadeIdJovemResponse;
import com.viabus.api.dto.ReservaRequest;
import com.viabus.api.dto.ReservaResponse;
import com.viabus.api.exception.BusinessException;
import com.viabus.api.exception.ConflitoConcorrenciaException;
import com.viabus.api.repository.AssentoViagemRepository;
import com.viabus.api.repository.ReservaRepository;
import com.viabus.api.repository.ViagemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private ViagemRepository viagemRepository;

    @Mock
    private AssentoViagemRepository assentoViagemRepository;

    @Mock
    private ViagemService viagemService;

    @Mock
    private PassageiroService passageiroService;

    @InjectMocks
    private ReservaService reservaService;

    private Viagem viagemPadrao;
    private Passageiro passageiroPadrao;
    private AssentoViagem assentoPadrao;

    @BeforeEach
    void setUp() {
        Cidade origem = Cidade.builder().id(1L).nome("São Paulo").uf("SP").build();
        Cidade destino = Cidade.builder().id(2L).nome("Rio de Janeiro").uf("RJ").build();
        Rota rota = Rota.builder()
                .id(10L)
                .cidadeOrigem(origem)
                .cidadeDestino(destino)
                .distanciaKm(new BigDecimal("435.00"))
                .tempoEstimadoMinutos(360)
                .ativa(true)
                .build();

        Onibus onibus = Onibus.builder()
                .id(5L)
                .placa("VBU5E67")
                .modelo("Marcopolo G8")
                .tipoLeito(TipoLeito.EXECUTIVO)
                .totalAssentos(40)
                .ativo(true)
                .build();

        viagemPadrao = Viagem.builder()
                .id(100L)
                .rota(rota)
                .onibus(onibus)
                .dataHoraPartida(LocalDateTime.now().plusDays(2))
                .dataHoraChegadaPrevista(LocalDateTime.now().plusDays(2).plusHours(6))
                .precoPassagem(new BigDecimal("120.00"))
                .vagasIdJovemGratuitas(2)
                .vagasIdJovemDesconto(2)
                .status(StatusViagem.AGENDADA)
                .versao(0L)
                .build();

        passageiroPadrao = Passageiro.builder()
                .id(50L)
                .nome("Lucas Pereira")
                .cpf("12345678901")
                .email("lucas@email.com")
                .dataNascimento(LocalDate.now().minusYears(20))
                .numeroIdJovem("IDJ998877")
                .validadeIdJovem(LocalDate.now().plusMonths(6))
                .ativo(true)
                .build();

        assentoPadrao = AssentoViagem.builder()
                .id(500L)
                .viagem(viagemPadrao)
                .numeroAssento(12)
                .status(StatusAssento.LIVRE)
                .versao(0L)
                .build();
    }

    @Test
    @DisplayName("Deve criar reserva COMERCIAL como PENDENTE_PAGAMENTO com 100% do valor e assento RESERVADO")
    void deveCriarReservaComercialComSucesso() {
        ReservaRequest request = new ReservaRequest(100L, 50L, 12, TipoTarifa.COMERCIAL);

        when(viagemService.buscarEntidadePorId(100L)).thenReturn(viagemPadrao);
        when(passageiroService.buscarEntidadePorId(50L)).thenReturn(passageiroPadrao);
        when(reservaRepository.existsByViagemIdAndPassageiroIdAndStatusIn(any(), any(), any())).thenReturn(false);
        when(assentoViagemRepository.findByViagemIdAndNumeroAssento(100L, 12)).thenReturn(Optional.of(assentoPadrao));
        when(reservaRepository.existsByCodigoReservaIgnoreCase(anyString())).thenReturn(false);
        when(reservaRepository.saveAndFlush(any(Reserva.class))).thenAnswer(inv -> {
            Reserva r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });

        ReservaResponse response = reservaService.criarReserva(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.codigoReserva()).startsWith("VB-");
        assertThat(response.status()).isEqualTo(StatusReserva.PENDENTE_PAGAMENTO);
        assertThat(response.statusAssento()).isEqualTo(StatusAssento.RESERVADO);
        assertThat(response.valorOriginal()).isEqualByComparingTo("120.00");
        assertThat(response.valorFinal()).isEqualByComparingTo("120.00");
        assertThat(response.dataConfirmacao()).isNull();
    }

    @Test
    @DisplayName("Deve criar reserva ID_JOVEM_GRATUITO já CONFIRMADA com valor 0.00, assento OCUPADO e decrementar cota gratuita")
    void deveCriarReservaIdJovemGratuitoConfirmada() {
        ReservaRequest request = new ReservaRequest(100L, 50L, 12, TipoTarifa.ID_JOVEM_GRATUITO);

        when(viagemService.buscarEntidadePorId(100L)).thenReturn(viagemPadrao);
        when(passageiroService.buscarEntidadePorId(50L)).thenReturn(passageiroPadrao);
        when(reservaRepository.existsByViagemIdAndPassageiroIdAndStatusIn(any(), any(), any())).thenReturn(false);
        when(assentoViagemRepository.findByViagemIdAndNumeroAssento(100L, 12)).thenReturn(Optional.of(assentoPadrao));
        when(passageiroService.verificarElegibilidadeIdJovem(50L)).thenReturn(
                new ElegibilidadeIdJovemResponse(50L, "Lucas Pereira", "12345678901", 20, "IDJ998877", LocalDate.now().plusMonths(6), true, "Elegível")
        );
        when(reservaRepository.existsByCodigoReservaIgnoreCase(anyString())).thenReturn(false);
        when(reservaRepository.saveAndFlush(any(Reserva.class))).thenAnswer(inv -> {
            Reserva r = inv.getArgument(0);
            r.setId(2L);
            return r;
        });

        ReservaResponse response = reservaService.criarReserva(request);

        assertThat(response.status()).isEqualTo(StatusReserva.CONFIRMADA);
        assertThat(response.statusAssento()).isEqualTo(StatusAssento.OCUPADO);
        assertThat(response.valorFinal()).isEqualByComparingTo("0.00");
        assertThat(response.dataConfirmacao()).isNotNull();
        assertThat(viagemPadrao.getVagasIdJovemGratuitas()).isEqualTo(1);
    }

    @Test
    @DisplayName("Deve criar reserva ID_JOVEM_DESCONTO com 50% de desconto e status PENDENTE_PAGAMENTO")
    void deveCriarReservaIdJovemDescontoComCinquentaPorCento() {
        ReservaRequest request = new ReservaRequest(100L, 50L, 12, TipoTarifa.ID_JOVEM_DESCONTO);

        when(viagemService.buscarEntidadePorId(100L)).thenReturn(viagemPadrao);
        when(passageiroService.buscarEntidadePorId(50L)).thenReturn(passageiroPadrao);
        when(reservaRepository.existsByViagemIdAndPassageiroIdAndStatusIn(any(), any(), any())).thenReturn(false);
        when(assentoViagemRepository.findByViagemIdAndNumeroAssento(100L, 12)).thenReturn(Optional.of(assentoPadrao));
        when(passageiroService.verificarElegibilidadeIdJovem(50L)).thenReturn(
                new ElegibilidadeIdJovemResponse(50L, "Lucas Pereira", "12345678901", 20, "IDJ998877", LocalDate.now().plusMonths(6), true, "Elegível")
        );
        when(reservaRepository.existsByCodigoReservaIgnoreCase(anyString())).thenReturn(false);
        when(reservaRepository.saveAndFlush(any(Reserva.class))).thenAnswer(inv -> {
            Reserva r = inv.getArgument(0);
            r.setId(3L);
            return r;
        });

        ReservaResponse response = reservaService.criarReserva(request);

        assertThat(response.status()).isEqualTo(StatusReserva.PENDENTE_PAGAMENTO);
        assertThat(response.statusAssento()).isEqualTo(StatusAssento.RESERVADO);
        assertThat(response.valorOriginal()).isEqualByComparingTo("120.00");
        assertThat(response.valorFinal()).isEqualByComparingTo("60.00");
        assertThat(viagemPadrao.getVagasIdJovemDesconto()).isEqualTo(1);
    }

    @Test
    @DisplayName("Deve devolver vaga da cota ID Jovem e liberar assento ao cancelar reserva social")
    void deveEstornarCotaIdJovemAoCancelarReserva() {
        viagemPadrao.setVagasIdJovemGratuitas(1);
        assentoPadrao.setStatus(StatusAssento.OCUPADO);

        Reserva reserva = Reserva.builder()
                .id(10L)
                .codigoReserva("VB-TESTE123")
                .viagem(viagemPadrao)
                .passageiro(passageiroPadrao)
                .assentoViagem(assentoPadrao)
                .tipoTarifa(TipoTarifa.ID_JOVEM_GRATUITO)
                .valorOriginal(new BigDecimal("120.00"))
                .valorFinal(new BigDecimal("0.00"))
                .status(StatusReserva.CONFIRMADA)
                .dataReserva(LocalDateTime.now().minusMinutes(5))
                .dataExpiracao(LocalDateTime.now().plusMinutes(10))
                .dataConfirmacao(LocalDateTime.now().minusMinutes(5))
                .build();

        when(reservaRepository.findById(10L)).thenReturn(Optional.of(reserva));
        when(reservaRepository.saveAndFlush(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

        ReservaResponse response = reservaService.cancelarReserva(10L);

        assertThat(response.status()).isEqualTo(StatusReserva.CANCELADA);
        assertThat(assentoPadrao.getStatus()).isEqualTo(StatusAssento.LIVRE);
        assertThat(viagemPadrao.getVagasIdJovemGratuitas()).isEqualTo(2);
        verify(assentoViagemRepository).saveAndFlush(assentoPadrao);
        verify(viagemRepository).saveAndFlush(viagemPadrao);
    }

    @Test
    @DisplayName("Deve capturar ObjectOptimisticLockingFailureException e converter para ConflitoConcorrenciaException")
    void deveConverterFalhaDeOptimisticLockingEmConflitoConcorrenciaException() {
        ReservaRequest request = new ReservaRequest(100L, 50L, 12, TipoTarifa.COMERCIAL);

        when(viagemService.buscarEntidadePorId(100L)).thenReturn(viagemPadrao);
        when(passageiroService.buscarEntidadePorId(50L)).thenReturn(passageiroPadrao);
        when(reservaRepository.existsByViagemIdAndPassageiroIdAndStatusIn(any(), any(), any())).thenReturn(false);
        when(assentoViagemRepository.findByViagemIdAndNumeroAssento(100L, 12)).thenReturn(Optional.of(assentoPadrao));
        when(assentoViagemRepository.saveAndFlush(any(AssentoViagem.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(AssentoViagem.class, 500L));

        assertThatThrownBy(() -> reservaService.criarReserva(request))
                .isInstanceOf(ConflitoConcorrenciaException.class)
                .hasMessageContaining("Conflito de concorrência");
    }

    @Test
    @DisplayName("Deve impedir reserva quando o passageiro já possui reserva ativa na mesma viagem")
    void deveImpedirDuasReservasAtivasDoMesmoPassageiroNaMesmaViagem() {
        ReservaRequest request = new ReservaRequest(100L, 50L, 12, TipoTarifa.COMERCIAL);

        when(viagemService.buscarEntidadePorId(100L)).thenReturn(viagemPadrao);
        when(passageiroService.buscarEntidadePorId(50L)).thenReturn(passageiroPadrao);
        when(reservaRepository.existsByViagemIdAndPassageiroIdAndStatusIn(any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> reservaService.criarReserva(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("já possui uma reserva ativa");
    }
}
