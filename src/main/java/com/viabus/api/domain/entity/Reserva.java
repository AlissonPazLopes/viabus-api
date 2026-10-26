package com.viabus.api.domain.entity;

import com.viabus.api.domain.enums.StatusReserva;
import com.viabus.api.domain.enums.TipoTarifa;
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
@Table(name = "reservas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_reserva", nullable = false, unique = true, length = 20)
    private String codigoReserva;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "viagem_id", nullable = false)
    private Viagem viagem;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "passageiro_id", nullable = false)
    private Passageiro passageiro;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "assento_viagem_id", nullable = false)
    private AssentoViagem assentoViagem;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_tarifa", nullable = false, length = 30)
    private TipoTarifa tipoTarifa;

    @Column(name = "valor_original", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorOriginal;

    @Column(name = "valor_final", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorFinal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusReserva status;

    @Column(name = "data_reserva", nullable = false)
    private LocalDateTime dataReserva;

    @Column(name = "data_expiracao", nullable = false)
    private LocalDateTime dataExpiracao;

    @Column(name = "data_confirmacao")
    private LocalDateTime dataConfirmacao;

    @Version
    @Builder.Default
    @Column(nullable = false)
    private Long versao = 0L;
}
