package com.viabus.api.repository;

import com.viabus.api.domain.entity.Onibus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OnibusRepository extends JpaRepository<Onibus, Long> {

    boolean existsByPlacaIgnoreCase(String placa);

    Optional<Onibus> findByPlacaIgnoreCase(String placa);

    List<Onibus> findByAtivoTrue();
}
