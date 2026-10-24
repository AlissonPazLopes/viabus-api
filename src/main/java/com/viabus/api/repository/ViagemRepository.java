package com.viabus.api.repository;

import com.viabus.api.domain.entity.Viagem;
import com.viabus.api.domain.enums.StatusViagem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface ViagemRepository extends JpaRepository<Viagem, Long> {

    List<Viagem> findByRotaIdOrderByDataHoraPartidaAsc(Long rotaId);

    List<Viagem> findByRotaIdAndDataHoraPartidaBetweenOrderByDataHoraPartidaAsc(
            Long rotaId,
            LocalDateTime inicio,
            LocalDateTime fim
    );

    @Query("""
            SELECT v FROM Viagem v
            WHERE (:rotaId IS NULL OR v.rota.id = :rotaId)
              AND (:origemId IS NULL OR v.rota.cidadeOrigem.id = :origemId)
              AND (:destinoId IS NULL OR v.rota.cidadeDestino.id = :destinoId)
              AND (:inicio IS NULL OR v.dataHoraPartida >= :inicio)
              AND (:fim IS NULL OR v.dataHoraPartida <= :fim)
            ORDER BY v.dataHoraPartida ASC
            """)
    List<Viagem> buscarViagensPorFiltros(
            @Param("rotaId") Long rotaId,
            @Param("origemId") Long origemId,
            @Param("destinoId") Long destinoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );

    @Query("""
            SELECT COUNT(v) > 0 FROM Viagem v
            WHERE v.onibus.id = :onibusId
              AND v.status IN :statusAtivos
              AND v.dataHoraPartida < :novaChegada
              AND v.dataHoraChegadaPrevista > :novaPartida
            """)
    boolean existsConflitoHorarioOnibus(
            @Param("onibusId") Long onibusId,
            @Param("novaPartida") LocalDateTime novaPartida,
            @Param("novaChegada") LocalDateTime novaChegada,
            @Param("statusAtivos") Collection<StatusViagem> statusAtivos
    );
}
