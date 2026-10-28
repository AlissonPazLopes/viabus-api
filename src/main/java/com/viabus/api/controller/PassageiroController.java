package com.viabus.api.controller;

import com.viabus.api.dto.BeneficioIdJovemRequest;
import com.viabus.api.dto.ElegibilidadeIdJovemResponse;
import com.viabus.api.dto.PassageiroRequest;
import com.viabus.api.dto.PassageiroResponse;
import com.viabus.api.service.PassageiroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/passageiros")
@RequiredArgsConstructor
@Tag(name = "Passageiros e ID Jovem", description = "Cadastro de passageiros e validação de elegibilidade ao benefício social ID Jovem (15 a 29 anos)")
public class PassageiroController {

    private final PassageiroService passageiroService;

    @PostMapping
    @Operation(
            summary = "Cadastrar novo passageiro",
            description = "Cadastra um passageiro validando unicidade de CPF, e-mail e número do cartão ID Jovem (quando informado). "
                    + "Retorna HTTP 201 Created ou HTTP 400 Bad Request.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Passageiro cadastrado com sucesso"),
                    @ApiResponse(responseCode = "400", description = "CPF/e-mail/ID Jovem duplicado ou erro de validação")
            }
    )
    public ResponseEntity<PassageiroResponse> cadastrar(@Valid @RequestBody PassageiroRequest dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(passageiroService.cadastrar(dto));
    }

    @GetMapping
    @Operation(
            summary = "Listar todos os passageiros",
            description = "Retorna a lista de passageiros cadastrados no sistema (HTTP 200 OK).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Lista de passageiros retornada com sucesso")
            }
    )
    public ResponseEntity<List<PassageiroResponse>> listar() {
        return ResponseEntity.ok(passageiroService.listarTodos());
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Buscar passageiro por ID",
            description = "Retorna os dados cadastrais do passageiro pelo ID (HTTP 200 OK ou HTTP 404 Not Found).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Passageiro encontrado"),
                    @ApiResponse(responseCode = "404", description = "Passageiro não encontrado")
            }
    )
    public ResponseEntity<PassageiroResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(passageiroService.buscarPorId(id));
    }

    @GetMapping("/cpf/{cpf}")
    @Operation(
            summary = "Buscar passageiro por CPF",
            description = "Retorna os dados cadastrais do passageiro pelos 11 dígitos do CPF (HTTP 200 OK ou HTTP 404 Not Found).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Passageiro encontrado"),
                    @ApiResponse(responseCode = "404", description = "Passageiro não encontrado para o CPF informado")
            }
    )
    public ResponseEntity<PassageiroResponse> buscarPorCpf(@PathVariable String cpf) {
        return ResponseEntity.ok(passageiroService.buscarPorCpf(cpf));
    }

    @PatchMapping("/{id}/id-jovem")
    @Operation(
            summary = "Vincular ou atualizar cartão ID Jovem do passageiro",
            description = "Associa ou renova o número e a data de validade do benefício ID Jovem de um passageiro. "
                    + "Retorna HTTP 200 OK, HTTP 400 Bad Request ou HTTP 404 Not Found.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Cartão ID Jovem atualizado com sucesso"),
                    @ApiResponse(responseCode = "400", description = "Número de ID Jovem já vinculado a outro passageiro ou validade inválida"),
                    @ApiResponse(responseCode = "404", description = "Passageiro não encontrado")
            }
    )
    public ResponseEntity<PassageiroResponse> vincularOuAtualizarIdJovem(
            @PathVariable Long id,
            @Valid @RequestBody BeneficioIdJovemRequest dto
    ) {
        return ResponseEntity.ok(passageiroService.vincularOuAtualizarIdJovem(id, dto));
    }

    @GetMapping("/{id}/elegibilidade-id-jovem")
    @Operation(
            summary = "Verificar elegibilidade do passageiro ao benefício ID Jovem",
            description = "Verifica se o passageiro está ativo, possui cartão ID Jovem válido e tem idade entre 15 e 29 anos. "
                    + "Retorna HTTP 200 OK com diagnóstico detalhado ou HTTP 404 Not Found.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Diagnóstico de elegibilidade retornado com sucesso"),
                    @ApiResponse(responseCode = "404", description = "Passageiro não encontrado")
            }
    )
    public ResponseEntity<ElegibilidadeIdJovemResponse> verificarElegibilidadeIdJovem(@PathVariable Long id) {
        return ResponseEntity.ok(passageiroService.verificarElegibilidadeIdJovem(id));
    }
}
