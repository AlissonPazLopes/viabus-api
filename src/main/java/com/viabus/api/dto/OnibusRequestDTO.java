package com.viabus.api.dto;

import com.viabus.api.domain.enums.TipoLeito;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record OnibusRequestDTO(
        @NotBlank(message = "A placa do ônibus é obrigatória")
        @Size(max = 10, message = "A placa deve ter no máximo 10 caracteres")
        String placa,

        @NotBlank(message = "O modelo do ônibus é obrigatório")
        @Size(max = 100, message = "O modelo deve ter no máximo 100 caracteres")
        String modelo,

        @NotNull(message = "O tipo de leito é obrigatório")
        TipoLeito tipoLeito,

        @NotNull(message = "O total de assentos é obrigatório")
        @Positive(message = "O total de assentos deve ser maior que zero")
        Integer totalAssentos,

        Boolean ativo
) {
}
