package com.viabus.api.controller;

import com.viabus.api.dto.MapaAssentoResponse;
import com.viabus.api.dto.StatusViagemRequestDTO;
import com.viabus.api.dto.ViagemRequestDTO;
import com.viabus.api.dto.ViagemResponseDTO;
import com.viabus.api.service.ViagemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/viagens")
@RequiredArgsConstructor
@Tag(name = "Viagens e Mapa de Assentos", description = "Agendamento de viagens, geração automática de poltronas e controle de cotas do ID Jovem")
public class ViagemController {

    private final ViagemService viagemService;

    @PostMapping
    @Operation(
            summary = "Agendar nova viagem",
            description = "Agenda uma viagem calculando a chegada prevista, inicializando 2 vagas gratuitas e 2 vagas com 50% de desconto do ID Jovem, "
                    + "gerando automaticamente todas as poltronas do ônibus e validando conflito de horário do veículo. "
                    + "Retorna HTTP 201 Created, HTTP 400 Bad Request ou HTTP 404 Not Found.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Viagem agendada e mapa de assentos gerado com sucesso"),
                    @ApiResponse(responseCode = "400", description = "Conflito de horário do ônibus, rota/ônibus inativo ou dados inválidos"),
                    @ApiResponse(responseCode = "404", description = "Rota ou ônibus não encontrado")
            }
    )
    public ResponseEntity<ViagemResponseDTO> agendar(@Valid @RequestBody ViagemRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(viagemService.agendar(dto));
    }

    @GetMapping
    @Operation(
            summary = "Listar e filtrar viagens",
            description = "Lista viagens disponíveis permitindo filtrar por rotaId, origemId, destinoId e dataPartida (HTTP 200 OK).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Lista de viagens retornada com sucesso")
            }
    )
    public ResponseEntity<List<ViagemResponseDTO>> listar(
            @RequestParam(required = false) Long rotaId,
            @RequestParam(required = false) Long origemId,
            @RequestParam(required = false) Long destinoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataPartida
    ) {
        return ResponseEntity.ok(viagemService.listarViagens(rotaId, origemId, destinoId, dataPartida));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Buscar detalhes da viagem por ID",
            description = "Retorna horários, veículo escalado, preço e saldo de cotas do ID Jovem da viagem (HTTP 200 OK ou HTTP 404 Not Found).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Viagem encontrada"),
                    @ApiResponse(responseCode = "404", description = "Viagem não encontrada")
            }
    )
    public ResponseEntity<ViagemResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(viagemService.buscarPorId(id));
    }

    @GetMapping("/{id}/assentos")
    @Operation(
            summary = "Consultar mapa de assentos e saldo de cotas ID Jovem da viagem",
            description = "Retorna o mapa completo de poltronas (LIVRE, RESERVADO, OCUPADO) e o saldo atualizado de vagas sociais do ID Jovem "
                    + "(HTTP 200 OK ou HTTP 404 Not Found).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Mapa de assentos retornado com sucesso"),
                    @ApiResponse(responseCode = "404", description = "Viagem não encontrada")
            }
    )
    public ResponseEntity<MapaAssentoResponse> consultarMapaAssentos(@PathVariable Long id) {
        return ResponseEntity.ok(viagemService.consultarMapaAssentos(id));
    }

    @PatchMapping("/{id}/status")
    @Operation(
            summary = "Atualizar status da viagem",
            description = "Atualiza o status operacional da viagem (AGENDADA, EM_ANDAMENTO, FINALIZADA, CANCELADA). "
                    + "Retorna HTTP 200 OK, HTTP 400 Bad Request ou HTTP 404 Not Found.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Status da viagem atualizado com sucesso"),
                    @ApiResponse(responseCode = "400", description = "Transição de status inválida"),
                    @ApiResponse(responseCode = "404", description = "Viagem não encontrada")
            }
    )
    public ResponseEntity<ViagemResponseDTO> atualizarStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusViagemRequestDTO dto
    ) {
        return ResponseEntity.ok(viagemService.atualizarStatus(id, dto));
    }
}
