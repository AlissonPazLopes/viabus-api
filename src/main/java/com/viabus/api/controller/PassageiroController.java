package com.viabus.api.controller;

import com.viabus.api.dto.BeneficioIdJovemRequest;
import com.viabus.api.dto.ElegibilidadeIdJovemResponse;
import com.viabus.api.dto.PassageiroRequest;
import com.viabus.api.dto.PassageiroResponse;
import com.viabus.api.service.PassageiroService;
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
@RequestMapping("/api/passageiros")
@RequiredArgsConstructor
public class PassageiroController {

    private final PassageiroService passageiroService;

    @PostMapping
    public ResponseEntity<PassageiroResponse> cadastrar(@Valid @RequestBody PassageiroRequest dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(passageiroService.cadastrar(dto));
    }

    @GetMapping
    public ResponseEntity<List<PassageiroResponse>> listar() {
        return ResponseEntity.ok(passageiroService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PassageiroResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(passageiroService.buscarPorId(id));
    }

    @GetMapping("/cpf/{cpf}")
    public ResponseEntity<PassageiroResponse> buscarPorCpf(@PathVariable String cpf) {
        return ResponseEntity.ok(passageiroService.buscarPorCpf(cpf));
    }

    @PatchMapping("/{id}/id-jovem")
    public ResponseEntity<PassageiroResponse> vincularOuAtualizarIdJovem(
            @PathVariable Long id,
            @Valid @RequestBody BeneficioIdJovemRequest dto
    ) {
        return ResponseEntity.ok(passageiroService.vincularOuAtualizarIdJovem(id, dto));
    }

    @GetMapping("/{id}/elegibilidade-id-jovem")
    public ResponseEntity<ElegibilidadeIdJovemResponse> verificarElegibilidadeIdJovem(@PathVariable Long id) {
        return ResponseEntity.ok(passageiroService.verificarElegibilidadeIdJovem(id));
    }
}
