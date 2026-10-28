package com.viabus.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@Tag(name = "Health Check", description = "Monitoramento de disponibilidade e status operacional da API")
public class HealthController {

    @GetMapping
    @Operation(
            summary = "Verificar status da API",
            description = "Retorna HTTP 200 OK com o status operacional do serviço viabus-api e timestamp atual.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Serviço em operação normal (UP)")
            }
    )
    public ResponseEntity<Map<String, Object>> checkHealth() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "UP");
        response.put("service", "viabus-api");
        response.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(response);
    }
}
