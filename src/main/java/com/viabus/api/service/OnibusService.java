package com.viabus.api.service;

import com.viabus.api.domain.entity.Onibus;
import com.viabus.api.dto.OnibusRequestDTO;
import com.viabus.api.dto.OnibusResponseDTO;
import com.viabus.api.exception.BusinessException;
import com.viabus.api.exception.ResourceNotFoundException;
import com.viabus.api.repository.OnibusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OnibusService {

    private final OnibusRepository onibusRepository;

    @Transactional
    public OnibusResponseDTO criar(OnibusRequestDTO dto) {
        String placaNormalizada = dto.placa().trim().toUpperCase();

        if (onibusRepository.existsByPlacaIgnoreCase(placaNormalizada)) {
            throw new BusinessException("Já existe um ônibus cadastrado com a placa '" + placaNormalizada + "'.");
        }

        Onibus onibus = Onibus.builder()
                .placa(placaNormalizada)
                .modelo(dto.modelo().trim())
                .tipoLeito(dto.tipoLeito())
                .totalAssentos(dto.totalAssentos())
                .ativo(dto.ativo() != null ? dto.ativo() : true)
                .build();

        return OnibusResponseDTO.fromEntity(onibusRepository.save(onibus));
    }

    @Transactional
    public OnibusResponseDTO atualizar(Long id, OnibusRequestDTO dto) {
        Onibus onibus = buscarEntidadePorId(id);
        String placaNormalizada = dto.placa().trim().toUpperCase();

        onibusRepository.findByPlacaIgnoreCase(placaNormalizada)
                .ifPresent(existente -> {
                    if (!existente.getId().equals(id)) {
                        throw new BusinessException("Já existe um ônibus cadastrado com a placa '" + placaNormalizada + "'.");
                    }
                });

        onibus.setPlaca(placaNormalizada);
        onibus.setModelo(dto.modelo().trim());
        onibus.setTipoLeito(dto.tipoLeito());
        onibus.setTotalAssentos(dto.totalAssentos());
        if (dto.ativo() != null) {
            onibus.setAtivo(dto.ativo());
        }

        return OnibusResponseDTO.fromEntity(onibusRepository.save(onibus));
    }

    @Transactional(readOnly = true)
    public List<OnibusResponseDTO> listarTodos() {
        return onibusRepository.findAll()
                .stream()
                .map(OnibusResponseDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OnibusResponseDTO> listarAtivos() {
        return onibusRepository.findByAtivoTrue()
                .stream()
                .map(OnibusResponseDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public OnibusResponseDTO buscarPorId(Long id) {
        return OnibusResponseDTO.fromEntity(buscarEntidadePorId(id));
    }

    @Transactional(readOnly = true)
    public Onibus buscarEntidadePorId(Long id) {
        return onibusRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ônibus não encontrado com ID: " + id));
    }
}
