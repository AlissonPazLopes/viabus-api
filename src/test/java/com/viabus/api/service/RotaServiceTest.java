package com.viabus.api.service;

import com.viabus.api.domain.entity.Cidade;
import com.viabus.api.domain.entity.Rota;
import com.viabus.api.dto.RotaRequestDTO;
import com.viabus.api.dto.RotaResponseDTO;
import com.viabus.api.exception.BusinessException;
import com.viabus.api.repository.RotaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RotaServiceTest {

    @Mock
    private RotaRepository rotaRepository;

    @Mock
    private CidadeService cidadeService;

    @InjectMocks
    private RotaService rotaService;

    @Test
    @DisplayName("Deve criar rota intermunicipal com cidades de origem e destino distintas")
    void deveCriarRotaComSucesso() {
        Cidade origem = Cidade.builder().id(1L).nome("São Paulo").uf("SP").build();
        Cidade destino = Cidade.builder().id(2L).nome("Rio de Janeiro").uf("RJ").build();
        RotaRequestDTO request = new RotaRequestDTO(1L, 2L, new BigDecimal("435.50"), 360, true);

        when(cidadeService.buscarEntidadePorId(1L)).thenReturn(origem);
        when(cidadeService.buscarEntidadePorId(2L)).thenReturn(destino);
        when(rotaRepository.existsByCidadeOrigemIdAndCidadeDestinoId(1L, 2L)).thenReturn(false);
        when(rotaRepository.save(any(Rota.class))).thenAnswer(inv -> {
            Rota r = inv.getArgument(0);
            r.setId(10L);
            return r;
        });

        RotaResponseDTO response = rotaService.criar(request);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.cidadeOrigem().nome()).isEqualTo("São Paulo");
        assertThat(response.cidadeDestino().nome()).isEqualTo("Rio de Janeiro");
        assertThat(response.distanciaKm()).isEqualByComparingTo("435.50");
        assertThat(response.tempoEstimadoMinutos()).isEqualTo(360);
    }

    @Test
    @DisplayName("Deve impedir cadastro de rota com a mesma cidade de origem e destino")
    void deveImpedirMesmaCidadeOrigemEDestino() {
        RotaRequestDTO request = new RotaRequestDTO(1L, 1L, new BigDecimal("100.00"), 90, true);

        assertThatThrownBy(() -> rotaService.criar(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("A cidade de origem e a cidade de destino não podem ser iguais");

        verify(rotaRepository, never()).save(any(Rota.class));
    }

    @Test
    @DisplayName("Deve impedir cadastro de rota duplicada para a mesma origem e destino")
    void deveImpedirRotaDuplicada() {
        Cidade origem = Cidade.builder().id(1L).nome("São Paulo").uf("SP").build();
        Cidade destino = Cidade.builder().id(2L).nome("Curitiba").uf("PR").build();
        RotaRequestDTO request = new RotaRequestDTO(1L, 2L, new BigDecimal("408.00"), 330, true);

        when(cidadeService.buscarEntidadePorId(1L)).thenReturn(origem);
        when(cidadeService.buscarEntidadePorId(2L)).thenReturn(destino);
        when(rotaRepository.existsByCidadeOrigemIdAndCidadeDestinoId(1L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> rotaService.criar(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Já existe uma rota cadastrada");

        verify(rotaRepository, never()).save(any(Rota.class));
    }
}
