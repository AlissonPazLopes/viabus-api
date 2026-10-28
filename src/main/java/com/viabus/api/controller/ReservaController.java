package com.viabus.api.controller;

import com.viabus.api.dto.BilheteEmbarqueResponse;
import com.viabus.api.dto.ReservaRequest;
import com.viabus.api.dto.ReservaResponse;
import com.viabus.api.scheduler.ExpiracaoReservaScheduler;
import com.viabus.api.service.ReservaService;
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

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
@Tag(name = "Reservas e Bilhetes", description = "Motor de reservas com Optimistic Locking (@Version), cotas sociais ID Jovem e emissão de bilhete SHA-256")
public class ReservaController {

    private final ReservaService reservaService;
    private final ExpiracaoReservaScheduler expiracaoReservaScheduler;

    @PostMapping
    @Operation(
            summary = "Realizar reserva de passagem",
            description = "Reserva uma poltrona aplicando controle de concorrência (Optimistic Locking) e tarifação comercial ou social (ID Jovem 100% gratuito / 50% desconto). "
                    + "Retorna HTTP 201 Created, HTTP 400 Bad Request, HTTP 404 Not Found ou HTTP 409 Conflict em caso de disputa de assento.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Reserva criada com sucesso"),
                    @ApiResponse(responseCode = "400", description = "Regra de negócio violada (inelegível ao ID Jovem, cota esgotada ou passageiro já possui reserva ativa)"),
                    @ApiResponse(responseCode = "404", description = "Viagem, passageiro ou assento não encontrado"),
                    @ApiResponse(responseCode = "409", description = "Conflito de concorrência: poltrona indisponível ou reservada simultaneamente por outro passageiro")
            }
    )
    public ResponseEntity<ReservaResponse> criarReserva(@Valid @RequestBody ReservaRequest dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservaService.criarReserva(dto));
    }

    @PatchMapping("/{id}/confirmar")
    @Operation(
            summary = "Confirmar pagamento da reserva",
            description = "Confirma o pagamento de uma reserva pendente dentro do prazo de expiração (15 min) e marca a poltrona como OCUPADO. "
                    + "Retorna HTTP 200 OK, HTTP 400 Bad Request, HTTP 404 Not Found ou HTTP 409 Conflict.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Pagamento confirmado e poltrona ocupada com sucesso"),
                    @ApiResponse(responseCode = "400", description = "Reserva não está pendente ou prazo de pagamento expirado"),
                    @ApiResponse(responseCode = "404", description = "Reserva não encontrada"),
                    @ApiResponse(responseCode = "409", description = "Conflito de concorrência ao confirmar reserva")
            }
    )
    public ResponseEntity<ReservaResponse> confirmarPagamento(@PathVariable Long id) {
        return ResponseEntity.ok(reservaService.confirmarPagamento(id));
    }

    @PatchMapping("/{id}/cancelar")
    @Operation(
            summary = "Cancelar reserva e estornar assento/cota social",
            description = "Cancela uma reserva, libera imediatamente a poltrona para LIVRE e devolve a vaga de cota do ID Jovem na viagem caso aplicável. "
                    + "Retorna HTTP 200 OK, HTTP 400 Bad Request, HTTP 404 Not Found ou HTTP 409 Conflict.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Reserva cancelada e assento/cota estornados com sucesso"),
                    @ApiResponse(responseCode = "400", description = "Reserva já se encontra cancelada ou expirada"),
                    @ApiResponse(responseCode = "404", description = "Reserva não encontrada"),
                    @ApiResponse(responseCode = "409", description = "Conflito de concorrência ao cancelar reserva")
            }
    )
    public ResponseEntity<ReservaResponse> cancelarReserva(@PathVariable Long id) {
        return ResponseEntity.ok(reservaService.cancelarReserva(id));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Buscar reserva por ID",
            description = "Retorna os detalhes completos de uma reserva pelo seu ID (HTTP 200 OK ou HTTP 404 Not Found).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Reserva encontrada"),
                    @ApiResponse(responseCode = "404", description = "Reserva não encontrada")
            }
    )
    public ResponseEntity<ReservaResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(reservaService.buscarPorId(id));
    }

    @GetMapping("/{id}/bilhete")
    @Operation(
            summary = "Emitir bilhete de embarque com hash SHA-256",
            description = "Gera o comprovante oficial de embarque com CPF mascarado e assinatura digital SHA-256 para reservas CONFIRMADAS. "
                    + "Retorna HTTP 200 OK, HTTP 400 Bad Request (se não confirmada) ou HTTP 404 Not Found.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Bilhete de embarque emitido com sucesso"),
                    @ApiResponse(responseCode = "400", description = "Reserva ainda não está com status CONFIRMADA"),
                    @ApiResponse(responseCode = "404", description = "Reserva não encontrada")
            }
    )
    public ResponseEntity<BilheteEmbarqueResponse> emitirBilheteEmbarque(@PathVariable Long id) {
        return ResponseEntity.ok(reservaService.emitirBilheteEmbarque(id));
    }

    @PostMapping("/processar-expiradas")
    @Operation(
            summary = "Processar manualmente reservas pendentes expiradas",
            description = "Aciona sob demanda a rotina de expiração de reservas cujo prazo de pagamento (15 minutos) venceu, "
                    + "liberando poltronas e estornando cotas do ID Jovem (HTTP 200 OK).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Rotina executada com sucesso")
            }
    )
    public ResponseEntity<Map<String, Object>> processarReservasExpiradasManualmente() {
        int processadas = expiracaoReservaScheduler.processarReservasExpiradas();
        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("reservasExpiradasProcessadas", processadas);
        resposta.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(resposta);
    }

    @GetMapping("/codigo/{codigo}")
    @Operation(
            summary = "Buscar reserva por código localizador",
            description = "Consulta uma reserva pelo seu código alfanumérico único (ex: VB-XXXXXXXX). Retorna HTTP 200 OK ou HTTP 404 Not Found.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Reserva encontrada"),
                    @ApiResponse(responseCode = "404", description = "Reserva não encontrada para o código informado")
            }
    )
    public ResponseEntity<ReservaResponse> buscarPorCodigo(@PathVariable String codigo) {
        return ResponseEntity.ok(reservaService.buscarPorCodigo(codigo));
    }

    @GetMapping("/passageiro/{passageiroId}")
    @Operation(
            summary = "Listar histórico de reservas por passageiro",
            description = "Lista todas as reservas realizadas por um passageiro ordenadas da mais recente para a mais antiga (HTTP 200 OK ou HTTP 404 Not Found).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Histórico de reservas retornado com sucesso"),
                    @ApiResponse(responseCode = "404", description = "Passageiro não encontrado")
            }
    )
    public ResponseEntity<List<ReservaResponse>> listarPorPassageiro(@PathVariable Long passageiroId) {
        return ResponseEntity.ok(reservaService.listarPorPassageiro(passageiroId));
    }
}
