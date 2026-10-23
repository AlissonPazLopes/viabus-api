package com.viabus.api.service;

import com.viabus.api.domain.entity.Cidade;
import com.viabus.api.domain.entity.Rota;
import com.viabus.api.dto.RotaRequestDTO;
import com.viabus.api.dto.RotaResponseDTO;
import com.viabus.api.exception.BusinessException;
import com.viabus.api.exception.ResourceNotFoundException;
import com.viabus.api.repository.RotaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RotaService {

    private final RotaRepository rotaRepository;
    private final CidadeService cidadeService;

    @Transactional
    @CacheEvict(value = "rotas", allEntries = true)
    public RotaResponseDTO criar(RotaRequestDTO dto) {
        validarOrigemEDestinoDiferentes(dto.cidadeOrigemId(), dto.cidadeDestinoId());

        Cidade origem = cidadeService.buscarEntidadePorId(dto.cidadeOrigemId());
        Cidade destino = cidadeService.buscarEntidadePorId(dto.cidadeDestinoId());

        if (rotaRepository.existsByCidadeOrigemIdAndCidadeDestinoId(origem.getId(), destino.getId())) {
            throw new BusinessException("Já existe uma rota cadastrada para a origem e destino informados.");
        }

        Rota rota = Rota.builder()
                .cidadeOrigem(origem)
                .cidadeDestino(destino)
                .distanciaKm(dto.distanciaKm())
                .tempoEstimadoMinutos(dto.tempoEstimadoMinutos())
                .ativa(dto.ativa() != null ? dto.ativa() : true)
                .build();

        return RotaResponseDTO.fromEntity(rotaRepository.save(rota));
    }

    @Transactional
    @CacheEvict(value = "rotas", allEntries = true)
    public RotaResponseDTO atualizar(Long id, RotaRequestDTO dto) {
        validarOrigemEDestinoDiferentes(dto.cidadeOrigemId(), dto.cidadeDestinoId());

        Rota rota = buscarEntidadePorId(id);
        Cidade origem = cidadeService.buscarEntidadePorId(dto.cidadeOrigemId());
        Cidade destino = cidadeService.buscarEntidadePorId(dto.cidadeDestinoId());

        rotaRepository.findByCidadeOrigemIdAndCidadeDestinoId(origem.getId(), destino.getId())
                .stream()
                .filter(existente -> !existente.getId().equals(id))
                .findFirst()
                .ifPresent(existente -> {
                    throw new BusinessException("Já existe uma rota cadastrada para a origem e destino informados.");
                });

        rota.setCidadeOrigem(origem);
        rota.setCidadeDestino(destino);
        rota.setDistanciaKm(dto.distanciaKm());
        rota.setTempoEstimadoMinutos(dto.tempoEstimadoMinutos());
        if (dto.ativa() != null) {
            rota.setAtiva(dto.ativa());
        }

        return RotaResponseDTO.fromEntity(rotaRepository.save(rota));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "rotas")
    public List<RotaResponseDTO> listarTodas() {
        return rotaRepository.findAll()
                .stream()
                .map(RotaResponseDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "rotas", key = "'origem:' + #origemId + ':destino:' + #destinoId")
    public List<RotaResponseDTO> buscarPorOrigemEDestino(Long origemId, Long destinoId) {
        List<Rota> rotas;
        if (origemId != null && destinoId != null) {
            rotas = rotaRepository.findByCidadeOrigemIdAndCidadeDestinoId(origemId, destinoId);
        } else if (origemId != null) {
            rotas = rotaRepository.findByCidadeOrigemId(origemId);
        } else if (destinoId != null) {
            rotas = rotaRepository.findByCidadeDestinoId(destinoId);
        } else {
            rotas = rotaRepository.findAll();
        }
        return rotas.stream()
                .map(RotaResponseDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "rotas", key = "#id")
    public RotaResponseDTO buscarPorId(Long id) {
        return RotaResponseDTO.fromEntity(buscarEntidadePorId(id));
    }

    @Transactional(readOnly = true)
    public Rota buscarEntidadePorId(Long id) {
        return rotaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rota não encontrada com ID: " + id));
    }

    private void validarOrigemEDestinoDiferentes(Long origemId, Long destinoId) {
        if (origemId != null && origemId.equals(destinoId)) {
            throw new BusinessException("A cidade de origem e a cidade de destino não podem ser iguais.");
        }
    }
}
