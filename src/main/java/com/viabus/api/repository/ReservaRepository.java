package com.viabus.api.repository;

import com.viabus.api.domain.entity.Reserva;
import com.viabus.api.domain.enums.StatusReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    Optional<Reserva> findByCodigoReservaIgnoreCase(String codigoReserva);

    boolean existsByCodigoReservaIgnoreCase(String codigoReserva);

    List<Reserva> findByPassageiroIdOrderByDataReservaDesc(Long passageiroId);

    List<Reserva> findByViagemIdOrderByDataReservaDesc(Long viagemId);

    boolean existsByViagemIdAndPassageiroIdAndStatusIn(
            Long viagemId,
            Long passageiroId,
            Collection<StatusReserva> statusAtivos
    );

    List<Reserva> findByStatusAndDataExpiracaoBefore(StatusReserva status, LocalDateTime limite);
}
