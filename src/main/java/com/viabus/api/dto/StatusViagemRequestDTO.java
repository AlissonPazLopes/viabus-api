package com.viabus.api.dto;

import com.viabus.api.domain.enums.StatusViagem;
import jakarta.validation.constraints.NotNull;

public record StatusViagemRequestDTO(
        @NotNull(message = "O novo status da viagem é obrigatório")
        StatusViagem status
) {
}
