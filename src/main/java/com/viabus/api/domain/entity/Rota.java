package com.viabus.api.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "rotas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "cidade_origem_id", nullable = false)
    private Cidade cidadeOrigem;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "cidade_destino_id", nullable = false)
    private Cidade cidadeDestino;

    @Column(name = "distancia_km", nullable = false, precision = 10, scale = 2)
    private BigDecimal distanciaKm;

    @Column(name = "tempo_estimado_minutos", nullable = false)
    private Integer tempoEstimadoMinutos;

    @Builder.Default
    @Column(nullable = false)
    private Boolean ativa = true;
}
