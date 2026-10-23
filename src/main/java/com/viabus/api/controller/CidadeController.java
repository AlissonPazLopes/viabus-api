package com.viabus.api.controller;

import com.viabus.api.dto.CidadeRequestDTO;
import com.viabus.api.dto.CidadeResponseDTO;
import com.viabus.api.service.CidadeService;
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
public class CidadeController {

    private final CidadeService cidadeService;

    @PostMapping
    public ResponseEntity<CidadeResponseDTO> criar(@Valid @RequestBody CidadeRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cidadeService.criar(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CidadeResponseDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody CidadeRequestDTO dto
    ) {
        return ResponseEntity.ok(cidadeService.atualizar(id, dto));
    }

    @GetMapping
    public ResponseEntity<List<CidadeResponseDTO>> listar(@RequestParam(required = false) String uf) {
        if (uf != null && !uf.isBlank()) {
            return ResponseEntity.ok(cidadeService.listarPorUf(uf));
        }
        return ResponseEntity.ok(cidadeService.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CidadeResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(cidadeService.buscarPorId(id));
    }
}
