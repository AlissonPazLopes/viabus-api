package com.viabus.api.repository;

import com.viabus.api.domain.entity.Cidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CidadeRepository extends JpaRepository<Cidade, Long> {

    boolean existsByNomeIgnoreCaseAndUfIgnoreCase(String nome, String uf);

    Optional<Cidade> findByNomeIgnoreCaseAndUfIgnoreCase(String nome, String uf);

    List<Cidade> findByUfIgnoreCaseOrderByNomeAsc(String uf);
}
