package com.viabus.api.controller;

import com.viabus.api.dto.ReservaRequest;
import com.viabus.api.dto.ReservaResponse;
import com.viabus.api.service.ReservaService;
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
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaService reservaService;

    @PostMapping
    public ResponseEntity<ReservaResponse> criarReserva(@Valid @RequestBody ReservaRequest dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservaService.criarReserva(dto));
    }

    @PatchMapping("/{id}/confirmar")
    public ResponseEntity<ReservaResponse> confirmarPagamento(@PathVariable Long id) {
        return ResponseEntity.ok(reservaService.confirmarPagamento(id));
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<ReservaResponse> cancelarReserva(@PathVariable Long id) {
        return ResponseEntity.ok(reservaService.cancelarReserva(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservaResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(reservaService.buscarPorId(id));
    }

    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<ReservaResponse> buscarPorCodigo(@PathVariable String codigo) {
        return ResponseEntity.ok(reservaService.buscarPorCodigo(codigo));
    }

    @GetMapping("/passageiro/{passageiroId}")
    public ResponseEntity<List<ReservaResponse>> listarPorPassageiro(@PathVariable Long passageiroId) {
        return ResponseEntity.ok(reservaService.listarPorPassageiro(passageiroId));
    }
}
