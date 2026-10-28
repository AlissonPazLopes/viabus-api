package com.viabus.api.controller;

import com.viabus.api.dto.OnibusRequestDTO;
import com.viabus.api.dto.OnibusResponseDTO;
import com.viabus.api.service.OnibusService;
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
@RequestMapping("/api/onibus")
@RequiredArgsConstructor
@Tag(name = "Frota de Ônibus", description = "Gestão de veículos da frota rodoviária, tipos de leito e capacidade de poltronas")
public class OnibusController {

    private final OnibusService onibusService;

    @PostMapping
    @Operation(
            summary = "Cadastrar novo ônibus na frota",
            description = "Registra um veículo validando unicidade da placa e quantidade de assentos. "
                    + "Retorna HTTP 201 Created ou HTTP 400 Bad Request.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Ônibus cadastrado com sucesso"),
                    @ApiResponse(responseCode = "400", description = "Placa duplicada ou dados inválidos")
            }
    )
    public ResponseEntity<OnibusResponseDTO> criar(@Valid @RequestBody OnibusRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(onibusService.criar(dto));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Atualizar dados de um ônibus",
            description = "Atualiza placa, modelo, tipo de leito, total de assentos e status ativo do veículo. "
                    + "Retorna HTTP 200 OK, HTTP 400 Bad Request ou HTTP 404 Not Found.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Ônibus atualizado com sucesso"),
                    @ApiResponse(responseCode = "400", description = "Dados inválidos ou placa já pertencente a outro veículo"),
                    @ApiResponse(responseCode = "404", description = "Ônibus não encontrado")
            }
    )
    public ResponseEntity<OnibusResponseDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody OnibusRequestDTO dto
    ) {
        return ResponseEntity.ok(onibusService.atualizar(id, dto));
    }

    @GetMapping
    @Operation(
            summary = "Listar frota de ônibus",
            description = "Lista todos os ônibus cadastrados ou filtra apenas os veículos ativos na frota (HTTP 200 OK).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Lista de ônibus retornada com sucesso")
            }
    )
    public ResponseEntity<List<OnibusResponseDTO>> listar(
            @RequestParam(defaultValue = "false") boolean apenasAtivos
    ) {
        if (apenasAtivos) {
            return ResponseEntity.ok(onibusService.listarAtivos());
        }
        return ResponseEntity.ok(onibusService.listarTodos());
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Buscar ônibus por ID",
            description = "Retorna os detalhes de um veículo pelo seu ID (HTTP 200 OK ou HTTP 404 Not Found).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Ônibus encontrado"),
                    @ApiResponse(responseCode = "404", description = "Ônibus não encontrado")
            }
    )
    public ResponseEntity<OnibusResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(onibusService.buscarPorId(id));
    }
}
