package com.viabus.api.repository;

import com.viabus.api.domain.entity.Rota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RotaRepository extends JpaRepository<Rota, Long> {

    boolean existsByCidadeOrigemIdAndCidadeDestinoId(Long cidadeOrigemId, Long cidadeDestinoId);

    List<Rota> findByCidadeOrigemIdAndCidadeDestinoId(Long cidadeOrigemId, Long cidadeDestinoId);

    List<Rota> findByCidadeOrigemId(Long cidadeOrigemId);

    List<Rota> findByCidadeDestinoId(Long cidadeDestinoId);

    List<Rota> findByAtivaTrue();
}
