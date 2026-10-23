package com.viabus.api.controller;

import com.viabus.api.dto.OnibusRequestDTO;
import com.viabus.api.dto.OnibusResponseDTO;
import com.viabus.api.service.OnibusService;
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
public class OnibusController {

    private final OnibusService onibusService;

    @PostMapping
    public ResponseEntity<OnibusResponseDTO> criar(@Valid @RequestBody OnibusRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(onibusService.criar(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OnibusResponseDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody OnibusRequestDTO dto
    ) {
        return ResponseEntity.ok(onibusService.atualizar(id, dto));
    }

    @GetMapping
    public ResponseEntity<List<OnibusResponseDTO>> listar(
            @RequestParam(defaultValue = "false") boolean apenasAtivos
    ) {
        if (apenasAtivos) {
            return ResponseEntity.ok(onibusService.listarAtivos());
        }
        return ResponseEntity.ok(onibusService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OnibusResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(onibusService.buscarPorId(id));
    }
}
