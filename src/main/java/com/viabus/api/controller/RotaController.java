package com.viabus.api.controller;

import com.viabus.api.dto.RotaRequestDTO;
import com.viabus.api.dto.RotaResponseDTO;
import com.viabus.api.service.RotaService;
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
public class RotaController {

    private final RotaService rotaService;

    @PostMapping
    public ResponseEntity<RotaResponseDTO> criar(@Valid @RequestBody RotaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rotaService.criar(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RotaResponseDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody RotaRequestDTO dto
    ) {
        return ResponseEntity.ok(rotaService.atualizar(id, dto));
    }

    @GetMapping
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
    public ResponseEntity<RotaResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(rotaService.buscarPorId(id));
    }
}
