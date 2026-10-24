package com.viabus.api.controller;

import com.viabus.api.dto.MapaAssentoResponse;
import com.viabus.api.dto.StatusViagemRequestDTO;
import com.viabus.api.dto.ViagemRequestDTO;
import com.viabus.api.dto.ViagemResponseDTO;
import com.viabus.api.service.ViagemService;
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
public class ViagemController {

    private final ViagemService viagemService;

    @PostMapping
    public ResponseEntity<ViagemResponseDTO> agendar(@Valid @RequestBody ViagemRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(viagemService.agendar(dto));
    }

    @GetMapping
    public ResponseEntity<List<ViagemResponseDTO>> listar(
            @RequestParam(required = false) Long rotaId,
            @RequestParam(required = false) Long origemId,
            @RequestParam(required = false) Long destinoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataPartida
    ) {
        return ResponseEntity.ok(viagemService.listarViagens(rotaId, origemId, destinoId, dataPartida));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ViagemResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(viagemService.buscarPorId(id));
    }

    @GetMapping("/{id}/assentos")
    public ResponseEntity<MapaAssentoResponse> consultarMapaAssentos(@PathVariable Long id) {
        return ResponseEntity.ok(viagemService.consultarMapaAssentos(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ViagemResponseDTO> atualizarStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusViagemRequestDTO dto
    ) {
        return ResponseEntity.ok(viagemService.atualizarStatus(id, dto));
    }
}
