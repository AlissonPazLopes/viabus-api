package com.viabus.api.repository;

import com.viabus.api.domain.entity.AssentoViagem;
import com.viabus.api.domain.enums.StatusAssento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssentoViagemRepository extends JpaRepository<AssentoViagem, Long> {

    List<AssentoViagem> findByViagemIdOrderByNumeroAssentoAsc(Long viagemId);

    Optional<AssentoViagem> findByViagemIdAndNumeroAssento(Long viagemId, Integer numeroAssento);

    long countByViagemIdAndStatus(Long viagemId, StatusAssento status);
}
