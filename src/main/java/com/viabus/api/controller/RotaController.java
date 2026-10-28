package com.viabus.api.controller;

import com.viabus.api.dto.RotaRequestDTO;
import com.viabus.api.dto.RotaResponseDTO;
import com.viabus.api.service.RotaService;
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
@RequestMapping("/api/rotas")
@RequiredArgsConstructor
@Tag(name = "Rotas Intermunicipais", description = "Gestão de linhas e trechos intermunicipais com suporte a cache Redis")
public class RotaController {

    private final RotaService rotaService;

    @PostMapping
    @Operation(
            summary = "Cadastrar nova rota intermunicipal",
            description = "Cria uma rota validando que cidade de origem e destino sejam distintas e invalida o cache 'rotas'. "
                    + "Retorna HTTP 201 Created, HTTP 400 Bad Request ou HTTP 404 Not Found.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Rota criada com sucesso"),
                    @ApiResponse(responseCode = "400", description = "Origem igual ao destino, rota duplicada ou dados inválidos"),
                    @ApiResponse(responseCode = "404", description = "Cidade de origem ou destino não encontrada")
            }
    )
    public ResponseEntity<RotaResponseDTO> criar(@Valid @RequestBody RotaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rotaService.criar(dto));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Atualizar rota existente",
            description = "Atualiza dados de distância, tempo estimado e status de uma rota e invalida o cache 'rotas'. "
                    + "Retorna HTTP 200 OK, HTTP 400 Bad Request ou HTTP 404 Not Found.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Rota atualizada com sucesso"),
                    @ApiResponse(responseCode = "400", description = "Dados inválidos ou conflito de origem/destino"),
                    @ApiResponse(responseCode = "404", description = "Rota ou cidade não encontrada")
            }
    )
    public ResponseEntity<RotaResponseDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody RotaRequestDTO dto
    ) {
        return ResponseEntity.ok(rotaService.atualizar(id, dto));
    }

    @GetMapping
    @Operation(
            summary = "Listar rotas ou buscar por origem e destino",
            description = "Consulta rotas intermunicipais com suporte a filtro por origemId e destinoId e cache Redis ('rotas'). "
                    + "Retorna HTTP 200 OK.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Lista de rotas retornada com sucesso")
            }
    )
    public ResponseEntity<List<RotaResponseDTO>> listar(
            @RequestParam(required = false) Long origemId,
            @RequestParam(required = false) Long destinoId
    ) {
        if (origemId != null || destinoId != null) {
            return ResponseEntity.ok(rotaService.buscarPorOrigemEDestino(origemId, destinoId));
        }
        return ResponseEntity.ok(rotaService.listarTodas());
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Buscar rota por ID",
            description = "Retorna os detalhes de uma rota intermunicipal pelo ID (HTTP 200 OK ou HTTP 404 Not Found).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Rota encontrada"),
                    @ApiResponse(responseCode = "404", description = "Rota não encontrada")
            }
    )
    public ResponseEntity<RotaResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(rotaService.buscarPorId(id));
    }
}
