package com.viabus.api.repository;

import com.viabus.api.domain.entity.Passageiro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PassageiroRepository extends JpaRepository<Passageiro, Long> {

    Optional<Passageiro> findByCpf(String cpf);

    Optional<Passageiro> findByEmail(String email);

    Optional<Passageiro> findByNumeroIdJovem(String numeroIdJovem);

    boolean existsByCpf(String cpf);

    boolean existsByEmail(String email);

    boolean existsByNumeroIdJovem(String numeroIdJovem);

    boolean existsByNumeroIdJovemAndIdNot(String numeroIdJovem, Long id);
}
