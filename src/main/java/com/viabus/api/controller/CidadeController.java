package com.viabus.api.controller;

import com.viabus.api.dto.CidadeRequestDTO;
import com.viabus.api.dto.CidadeResponseDTO;
import com.viabus.api.service.CidadeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cidades")
@RequiredArgsConstructor
@Tag(name = "Cidades", description = "Gestão de cidades atendidas pelas rotas intermunicipais (com cache Redis)")
public class CidadeController {

    private final CidadeService cidadeService;

    @PostMapping
    @Operation(
            summary = "Cadastrar nova cidade",
            description = "Cadastra uma cidade validando duplicidade de nome na mesma UF e invalida o cache 'cidades'. "
                    + "Retorna HTTP 201 Created ou HTTP 400 Bad Request em caso de duplicidade/validação.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Cidade cadastrada com sucesso"),
                    @ApiResponse(responseCode = "400", description = "Dados inválidos ou cidade já cadastrada na UF")
            }
    )
    public ResponseEntity<CidadeResponseDTO> criar(@Valid @RequestBody CidadeRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cidadeService.criar(dto));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Atualizar cidade existente",
            description = "Atualiza nome e UF de uma cidade pelo ID e invalida o cache 'cidades'. "
                    + "Retorna HTTP 200 OK, HTTP 400 Bad Request ou HTTP 404 Not Found.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Cidade atualizada com sucesso"),
                    @ApiResponse(responseCode = "400", description = "Dados inválidos ou conflito de nome/UF"),
                    @ApiResponse(responseCode = "404", description = "Cidade não encontrada")
            }
    )
    public ResponseEntity<CidadeResponseDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody CidadeRequestDTO dto
    ) {
        return ResponseEntity.ok(cidadeService.atualizar(id, dto));
    }

    @GetMapping
    @Operation(
            summary = "Listar cidades ou filtrar por UF",
            description = "Lista todas as cidades cadastradas ou filtra pela sigla da UF utilizando cache Redis ('cidades'). "
                    + "Retorna HTTP 200 OK.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Lista de cidades retornada com sucesso")
            }
    )
    public ResponseEntity<List<CidadeResponseDTO>> listar(@RequestParam(required = false) String uf) {
        if (uf != null && !uf.isBlank()) {
            return ResponseEntity.ok(cidadeService.listarPorUf(uf));
        }
        return ResponseEntity.ok(cidadeService.listarTodas());
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Buscar cidade por ID",
            description = "Retorna os dados de uma cidade pelo seu identificador único (HTTP 200 OK ou HTTP 404 Not Found).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Cidade encontrada"),
                    @ApiResponse(responseCode = "404", description = "Cidade não encontrada")
            }
    )
    public ResponseEntity<CidadeResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(cidadeService.buscarPorId(id));
    }
}
