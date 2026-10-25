package com.viabus.api.dto;

import java.time.LocalDate;

public record ElegibilidadeIdJovemResponse(
        Long passageiroId,
        String nome,
        String cpf,
        int idade,
        String numeroIdJovem,
        LocalDate validadeIdJovem,
        boolean elegivel,
        String motivo
) {
}
