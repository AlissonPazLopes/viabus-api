package com.viabus.api.service;

import com.viabus.api.domain.entity.AssentoViagem;
import com.viabus.api.domain.entity.Cidade;
import com.viabus.api.domain.entity.Onibus;
import com.viabus.api.domain.entity.Rota;
import com.viabus.api.domain.entity.Viagem;
import com.viabus.api.domain.enums.StatusAssento;
import com.viabus.api.domain.enums.StatusViagem;
import com.viabus.api.domain.enums.TipoLeito;
import com.viabus.api.dto.ViagemRequestDTO;
import com.viabus.api.dto.ViagemResponseDTO;
import com.viabus.api.exception.BusinessException;
import com.viabus.api.repository.AssentoViagemRepository;
import com.viabus.api.repository.ViagemRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ViagemServiceTest {

    @Mock
    private ViagemRepository viagemRepository;

    @Mock
    private AssentoViagemRepository assentoViagemRepository;

    @Mock
    private RotaService rotaService;

    @Mock
    private OnibusService onibusService;

    @InjectMocks
    private ViagemService viagemService;

    @Test
    @DisplayName("Deve agendar viagem calculando chegada prevista, cotas ID Jovem e gerando poltronas de 1 até totalAssentos")
    @SuppressWarnings("unchecked")
    void deveAgendarViagemEGerarAssentos() {
        Cidade origem = Cidade.builder().id(1L).nome("São Paulo").uf("SP").build();
        Cidade destino = Cidade.builder().id(2L).nome("Curitiba").uf("PR").build();
        Rota rota = Rota.builder()
                .id(10L)
                .cidadeOrigem(origem)
                .cidadeDestino(destino)
                .distanciaKm(new BigDecimal("408.00"))
                .tempoEstimadoMinutos(360)
                .ativa(true)
                .build();

        Onibus onibus = Onibus.builder()
                .id(5L)
                .placa("VBU2B34")
                .modelo("Marcopolo Paradiso G8")
                .tipoLeito(TipoLeito.EXECUTIVO)
                .totalAssentos(40)
                .ativo(true)
                .build();

        LocalDateTime partida = LocalDateTime.of(2026, 11, 10, 8, 0);
        ViagemRequestDTO request = new ViagemRequestDTO(10L, 5L, partida, new BigDecimal("150.00"));

        when(rotaService.buscarEntidadePorId(10L)).thenReturn(rota);
        when(onibusService.buscarEntidadePorId(5L)).thenReturn(onibus);
        when(viagemRepository.existsConflitoHorarioOnibus(eq(5L), eq(partida), eq(partida.plusMinutes(360)), any()))
                .thenReturn(false);
        when(viagemRepository.save(any(Viagem.class))).thenAnswer(inv -> {
            Viagem v = inv.getArgument(0);
            v.setId(100L);
            return v;
        });

        ViagemResponseDTO response = viagemService.agendar(request);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.status()).isEqualTo(StatusViagem.AGENDADA);
        assertThat(response.dataHoraPartida()).isEqualTo(partida);
        assertThat(response.dataHoraChegadaPrevista()).isEqualTo(LocalDateTime.of(2026, 11, 10, 14, 0));
        assertThat(response.vagasIdJovemGratuitas()).isEqualTo(2);
        assertThat(response.vagasIdJovemDesconto()).isEqualTo(2);

        ArgumentCaptor<List<AssentoViagem>> assentosCaptor = ArgumentCaptor.forClass(List.class);
        verify(assentoViagemRepository).saveAll(assentosCaptor.capture());

        List<AssentoViagem> assentosGerados = assentosCaptor.getValue();
        assertThat(assentosGerados).hasSize(40);
        assertThat(assentosGerados.getFirst().getNumeroAssento()).isEqualTo(1);
        assertThat(assentosGerados.getLast().getNumeroAssento()).isEqualTo(40);
        assertThat(assentosGerados).allMatch(a -> a.getStatus() == StatusAssento.LIVRE);
    }

    @Test
    @DisplayName("Deve impedir agendamento quando o mesmo ônibus possuir conflito de horário")
    void deveImpedirConflitoHorarioMesmoOnibus() {
        Cidade origem = Cidade.builder().id(1L).nome("São Paulo").uf("SP").build();
        Cidade destino = Cidade.builder().id(2L).nome("Curitiba").uf("PR").build();
        Rota rota = Rota.builder()
                .id(10L)
                .cidadeOrigem(origem)
                .cidadeDestino(destino)
                .distanciaKm(new BigDecimal("408.00"))
                .tempoEstimadoMinutos(360)
                .ativa(true)
                .build();

        Onibus onibus = Onibus.builder()
                .id(5L)
                .placa("VBU2B34")
                .modelo("Marcopolo Paradiso G8")
                .tipoLeito(TipoLeito.EXECUTIVO)
                .totalAssentos(40)
                .ativo(true)
                .build();

        LocalDateTime partida = LocalDateTime.of(2026, 11, 10, 10, 0);
        ViagemRequestDTO request = new ViagemRequestDTO(10L, 5L, partida, new BigDecimal("150.00"));

        when(rotaService.buscarEntidadePorId(10L)).thenReturn(rota);
        when(onibusService.buscarEntidadePorId(5L)).thenReturn(onibus);
        when(viagemRepository.existsConflitoHorarioOnibus(eq(5L), eq(partida), eq(partida.plusMinutes(360)), any()))
                .thenReturn(true);

        assertThatThrownBy(() -> viagemService.agendar(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("horário conflitante");

        verify(viagemRepository, never()).save(any(Viagem.class));
        verify(assentoViagemRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Deve impedir agendamento de viagem com ônibus inativo")
    void deveImpedirOnibusInativo() {
        Rota rota = Rota.builder().id(10L).tempoEstimadoMinutos(200).ativa(true).build();
        Onibus onibusInativo = Onibus.builder().id(5L).placa("INA1B23").ativo(false).build();
        ViagemRequestDTO request = new ViagemRequestDTO(
                10L,
                5L,
                LocalDateTime.of(2026, 11, 12, 9, 0),
                new BigDecimal("90.00")
        );

        when(rotaService.buscarEntidadePorId(10L)).thenReturn(rota);
        when(onibusService.buscarEntidadePorId(5L)).thenReturn(onibusInativo);

        assertThatThrownBy(() -> viagemService.agendar(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ônibus inativo");

        verify(viagemRepository, never()).save(any(Viagem.class));
    }
}
