package com.viabus.api.domain.entity;

import com.viabus.api.domain.enums.StatusViagem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "viagens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Viagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "rota_id", nullable = false)
    private Rota rota;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "onibus_id", nullable = false)
    private Onibus onibus;

    @Column(name = "data_hora_partida", nullable = false)
    private LocalDateTime dataHoraPartida;

    @Column(name = "data_hora_chegada_prevista", nullable = false)
    private LocalDateTime dataHoraChegadaPrevista;

    @Column(name = "preco_passagem", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoPassagem;

    @Builder.Default
    @Column(name = "vagas_id_jovem_gratuitas", nullable = false)
    private Integer vagasIdJovemGratuitas = 2;

    @Builder.Default
    @Column(name = "vagas_id_jovem_desconto", nullable = false)
    private Integer vagasIdJovemDesconto = 2;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusViagem status = StatusViagem.AGENDADA;

    @Version
    @Builder.Default
    @Column(nullable = false)
    private Long versao = 0L;
}
