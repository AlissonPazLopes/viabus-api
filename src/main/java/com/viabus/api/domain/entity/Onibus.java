package com.viabus.api.domain.entity;

import com.viabus.api.domain.enums.TipoLeito;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "onibus")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Onibus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String placa;

    @Column(nullable = false, length = 100)
    private String modelo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_leito", nullable = false, length = 30)
    private TipoLeito tipoLeito;

    @Column(name = "total_assentos", nullable = false)
    private Integer totalAssentos;

    @Builder.Default
    @Column(nullable = false)
    private Boolean ativo = true;
}
