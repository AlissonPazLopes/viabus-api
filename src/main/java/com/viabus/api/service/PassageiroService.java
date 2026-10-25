package com.viabus.api.service;

import com.viabus.api.domain.entity.Passageiro;
import com.viabus.api.dto.BeneficioIdJovemRequest;
import com.viabus.api.dto.ElegibilidadeIdJovemResponse;
import com.viabus.api.dto.PassageiroRequest;
import com.viabus.api.dto.PassageiroResponse;
import com.viabus.api.exception.BusinessException;
import com.viabus.api.exception.ResourceNotFoundException;
import com.viabus.api.repository.PassageiroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PassageiroService {

    private static final int IDADE_MINIMA_ID_JOVEM = 15;
    private static final int IDADE_MAXIMA_ID_JOVEM = 29;

    private final PassageiroRepository passageiroRepository;

    @Transactional
    public PassageiroResponse cadastrar(PassageiroRequest dto) {
        String cpf = dto.cpf().trim();
        String email = dto.email().trim().toLowerCase();
        String numeroIdJovem = normalizarNumeroIdJovem(dto.numeroIdJovem());

        if (passageiroRepository.existsByCpf(cpf)) {
            throw new BusinessException("Já existe um passageiro cadastrado com o CPF informado.");
        }

        if (passageiroRepository.existsByEmail(email)) {
            throw new BusinessException("Já existe um passageiro cadastrado com o e-mail informado.");
        }

        if (numeroIdJovem != null && passageiroRepository.existsByNumeroIdJovem(numeroIdJovem)) {
            throw new BusinessException("Já existe um passageiro cadastrado com o número de ID Jovem informado.");
        }

        Passageiro passageiro = Passageiro.builder()
                .nome(dto.nome().trim())
                .cpf(cpf)
                .email(email)
                .telefone(dto.telefone() != null ? dto.telefone().trim() : null)
                .dataNascimento(dto.dataNascimento())
                .numeroIdJovem(numeroIdJovem)
                .validadeIdJovem(numeroIdJovem != null ? dto.validadeIdJovem() : null)
                .ativo(true)
                .dataCadastro(LocalDateTime.now())
                .build();

        return PassageiroResponse.fromEntity(passageiroRepository.save(passageiro));
    }

    @Transactional(readOnly = true)
    public List<PassageiroResponse> listarTodos() {
        return passageiroRepository.findAll()
                .stream()
                .map(PassageiroResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public PassageiroResponse buscarPorId(Long id) {
        return PassageiroResponse.fromEntity(buscarEntidadePorId(id));
    }

    @Transactional(readOnly = true)
    public PassageiroResponse buscarPorCpf(String cpf) {
        Passageiro passageiro = passageiroRepository.findByCpf(cpf.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Passageiro não encontrado com CPF: " + cpf));
        return PassageiroResponse.fromEntity(passageiro);
    }

    @Transactional
    public PassageiroResponse vincularOuAtualizarIdJovem(Long passageiroId, BeneficioIdJovemRequest dto) {
        Passageiro passageiro = buscarEntidadePorId(passageiroId);
        String numeroIdJovem = dto.numeroIdJovem().trim();

        if (passageiroRepository.existsByNumeroIdJovemAndIdNot(numeroIdJovem, passageiroId)) {
            throw new BusinessException("O número de ID Jovem informado já pertence a outro passageiro.");
        }

        passageiro.setNumeroIdJovem(numeroIdJovem);
        passageiro.setValidadeIdJovem(dto.validadeIdJovem());

        return PassageiroResponse.fromEntity(passageiroRepository.save(passageiro));
    }

    @Transactional(readOnly = true)
    public ElegibilidadeIdJovemResponse verificarElegibilidadeIdJovem(Long passageiroId) {
        Passageiro passageiro = buscarEntidadePorId(passageiroId);
        LocalDate hoje = LocalDate.now();
        int idadeAtual = Period.between(passageiro.getDataNascimento(), hoje).getYears();

        if (!Boolean.TRUE.equals(passageiro.getAtivo())) {
            return construirRespostaElegibilidade(
                    passageiro,
                    idadeAtual,
                    false,
                    "Passageiro inativo no sistema."
            );
        }

        if (passageiro.getNumeroIdJovem() == null || passageiro.getNumeroIdJovem().isBlank()) {
            return construirRespostaElegibilidade(
                    passageiro,
                    idadeAtual,
                    false,
                    "Passageiro não possui número de cartão ID Jovem vinculado."
            );
        }

        if (passageiro.getValidadeIdJovem() == null || passageiro.getValidadeIdJovem().isBefore(hoje)) {
            return construirRespostaElegibilidade(
                    passageiro,
                    idadeAtual,
                    false,
                    "Cartão ID Jovem vencido ou sem data de validade vigente."
            );
        }

        if (idadeAtual < IDADE_MINIMA_ID_JOVEM || idadeAtual > IDADE_MAXIMA_ID_JOVEM) {
            return construirRespostaElegibilidade(
                    passageiro,
                    idadeAtual,
                    false,
                    String.format(
                            "Idade atual (%d anos) fora da faixa permitida pelo benefício ID Jovem (%d a %d anos).",
                            idadeAtual, IDADE_MINIMA_ID_JOVEM, IDADE_MAXIMA_ID_JOVEM
                    )
            );
        }

        return construirRespostaElegibilidade(
                passageiro,
                idadeAtual,
                true,
                "Passageiro elegível ao benefício social ID Jovem."
        );
    }

    @Transactional(readOnly = true)
    public Passageiro buscarEntidadePorId(Long id) {
        return passageiroRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Passageiro não encontrado com ID: " + id));
    }

    private String normalizarNumeroIdJovem(String numeroIdJovem) {
        if (numeroIdJovem == null || numeroIdJovem.isBlank()) {
            return null;
        }
        return numeroIdJovem.trim();
    }

    private ElegibilidadeIdJovemResponse construirRespostaElegibilidade(
            Passageiro passageiro,
            int idade,
            boolean elegivel,
            String motivo
    ) {
        return new ElegibilidadeIdJovemResponse(
                passageiro.getId(),
                passageiro.getNome(),
                passageiro.getCpf(),
                idade,
                passageiro.getNumeroIdJovem(),
                passageiro.getValidadeIdJovem(),
                elegivel,
                motivo
        );
    }
}
