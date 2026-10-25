package com.viabus.api.dto;

import com.viabus.api.domain.entity.Passageiro;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PassageiroResponse(
        Long id,
        String nome,
        String cpf,
        String email,
        String telefone,
        LocalDate dataNascimento,
        String numeroIdJovem,
        LocalDate validadeIdJovem,
        Boolean ativo,
        LocalDateTime dataCadastro
) {
    public static PassageiroResponse fromEntity(Passageiro passageiro) {
        return new PassageiroResponse(
                passageiro.getId(),
                passageiro.getNome(),
                passageiro.getCpf(),
                passageiro.getEmail(),
                passageiro.getTelefone(),
                passageiro.getDataNascimento(),
                passageiro.getNumeroIdJovem(),
                passageiro.getValidadeIdJovem(),
                passageiro.getAtivo(),
                passageiro.getDataCadastro()
        );
    }
}
