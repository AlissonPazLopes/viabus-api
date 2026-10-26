package com.viabus.api.dto;

import com.viabus.api.domain.enums.TipoTarifa;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReservaRequest(
        @NotNull(message = "O ID da viagem é obrigatório")
        Long viagemId,

        @NotNull(message = "O ID do passageiro é obrigatório")
        Long passageiroId,

        @NotNull(message = "O número do assento é obrigatório")
        @Positive(message = "O número do assento deve ser maior que zero")
        Integer numeroAssento,

        @NotNull(message = "O tipo de tarifa é obrigatório")
        TipoTarifa tipoTarifa
) {
}
