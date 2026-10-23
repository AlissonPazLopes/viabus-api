package com.viabus.api.service;

import com.viabus.api.domain.entity.Cidade;
import com.viabus.api.dto.CidadeRequestDTO;
import com.viabus.api.dto.CidadeResponseDTO;
import com.viabus.api.exception.BusinessException;
import com.viabus.api.exception.ResourceNotFoundException;
import com.viabus.api.repository.CidadeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CidadeServiceTest {

    @Mock
    private CidadeRepository cidadeRepository;

    @InjectMocks
    private CidadeService cidadeService;

    @Test
    @DisplayName("Deve cadastrar uma nova cidade com sucesso normalizando a UF")
    void deveCriarCidadeComSucesso() {
        CidadeRequestDTO request = new CidadeRequestDTO("Campinas", "sp");
        when(cidadeRepository.existsByNomeIgnoreCaseAndUfIgnoreCase("Campinas", "SP")).thenReturn(false);
        when(cidadeRepository.save(any(Cidade.class))).thenAnswer(inv -> {
            Cidade c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        CidadeResponseDTO response = cidadeService.criar(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.nome()).isEqualTo("Campinas");
        assertThat(response.uf()).isEqualTo("SP");
        verify(cidadeRepository).save(any(Cidade.class));
    }

    @Test
    @DisplayName("Deve impedir o cadastro de cidade duplicada no mesmo estado (UF)")
    void deveImpedirCidadeDuplicadaNaMesmaUf() {
        CidadeRequestDTO request = new CidadeRequestDTO("Campinas", "SP");
        when(cidadeRepository.existsByNomeIgnoreCaseAndUfIgnoreCase("Campinas", "SP")).thenReturn(true);

        assertThatThrownBy(() -> cidadeService.criar(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Já existe uma cidade cadastrada");

        verify(cidadeRepository, never()).save(any(Cidade.class));
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao buscar cidade inexistente")
    void deveLancarExcecaoQuandoCidadeNaoEncontrada() {
        when(cidadeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cidadeService.buscarPorId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }
}
