package com.viabus.api.service;

import com.viabus.api.domain.entity.Cidade;
import com.viabus.api.dto.CidadeRequestDTO;
import com.viabus.api.dto.CidadeResponseDTO;
import com.viabus.api.exception.BusinessException;
import com.viabus.api.exception.ResourceNotFoundException;
import com.viabus.api.repository.CidadeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CidadeService {

    private final CidadeRepository cidadeRepository;

    @Transactional
    @CacheEvict(value = "cidades", allEntries = true)
    public CidadeResponseDTO criar(CidadeRequestDTO dto) {
        String nomeNormalizado = dto.nome().trim();
        String ufNormalizada = dto.uf().trim().toUpperCase();

        if (cidadeRepository.existsByNomeIgnoreCaseAndUfIgnoreCase(nomeNormalizado, ufNormalizada)) {
            throw new BusinessException("Já existe uma cidade cadastrada com o nome '" + nomeNormalizado + "' na UF '" + ufNormalizada + "'.");
        }

        Cidade cidade = Cidade.builder()
                .nome(nomeNormalizado)
                .uf(ufNormalizada)
                .build();

        return CidadeResponseDTO.fromEntity(cidadeRepository.save(cidade));
    }

    @Transactional
    @CacheEvict(value = "cidades", allEntries = true)
    public CidadeResponseDTO atualizar(Long id, CidadeRequestDTO dto) {
        Cidade cidade = buscarEntidadePorId(id);
        String nomeNormalizado = dto.nome().trim();
        String ufNormalizada = dto.uf().trim().toUpperCase();

        cidadeRepository.findByNomeIgnoreCaseAndUfIgnoreCase(nomeNormalizado, ufNormalizada)
                .ifPresent(existente -> {
                    if (!existente.getId().equals(id)) {
                        throw new BusinessException("Já existe uma cidade cadastrada com o nome '" + nomeNormalizado + "' na UF '" + ufNormalizada + "'.");
                    }
                });

        cidade.setNome(nomeNormalizado);
        cidade.setUf(ufNormalizada);

        return CidadeResponseDTO.fromEntity(cidadeRepository.save(cidade));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "cidades")
    public List<CidadeResponseDTO> listarTodas() {
        return cidadeRepository.findAll()
                .stream()
                .map(CidadeResponseDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "cidades", key = "#uf.toUpperCase()")
    public List<CidadeResponseDTO> listarPorUf(String uf) {
        return cidadeRepository.findByUfIgnoreCaseOrderByNomeAsc(uf.trim())
                .stream()
                .map(CidadeResponseDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "cidades", key = "#id")
    public CidadeResponseDTO buscarPorId(Long id) {
        return CidadeResponseDTO.fromEntity(buscarEntidadePorId(id));
    }

    @Transactional(readOnly = true)
    public Cidade buscarEntidadePorId(Long id) {
        return cidadeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cidade não encontrada com ID: " + id));
    }
}
