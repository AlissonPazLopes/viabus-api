package com.viabus.api.service;

import com.viabus.api.domain.entity.Onibus;
import com.viabus.api.domain.enums.TipoLeito;
import com.viabus.api.dto.OnibusRequestDTO;
import com.viabus.api.dto.OnibusResponseDTO;
import com.viabus.api.exception.BusinessException;
import com.viabus.api.repository.OnibusRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnibusServiceTest {

    @Mock
    private OnibusRepository onibusRepository;

    @InjectMocks
    private OnibusService onibusService;

    @Test
    @DisplayName("Deve cadastrar ônibus com sucesso e definir ativo como true por padrão")
    void deveCriarOnibusComSucesso() {
        OnibusRequestDTO request = new OnibusRequestDTO("abc1d23", "Marcopolo Paradiso G8", TipoLeito.EXECUTIVO, 42, null);
        when(onibusRepository.existsByPlacaIgnoreCase("ABC1D23")).thenReturn(false);
        when(onibusRepository.save(any(Onibus.class))).thenAnswer(inv -> {
            Onibus o = inv.getArgument(0);
            o.setId(1L);
            return o;
        });

        OnibusResponseDTO response = onibusService.criar(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.placa()).isEqualTo("ABC1D23");
        assertThat(response.tipoLeito()).isEqualTo(TipoLeito.EXECUTIVO);
        assertThat(response.totalAssentos()).isEqualTo(42);
        assertThat(response.ativo()).isTrue();
    }

    @Test
    @DisplayName("Deve impedir o cadastro de ônibus com placa duplicada")
    void deveImpedirPlacaDuplicada() {
        OnibusRequestDTO request = new OnibusRequestDTO("ABC1D23", "Marcopolo Paradiso G8", TipoLeito.SEMI_LEITO, 46, true);
        when(onibusRepository.existsByPlacaIgnoreCase("ABC1D23")).thenReturn(true);

        assertThatThrownBy(() -> onibusService.criar(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Já existe um ônibus cadastrado com a placa");

        verify(onibusRepository, never()).save(any(Onibus.class));
    }
}
