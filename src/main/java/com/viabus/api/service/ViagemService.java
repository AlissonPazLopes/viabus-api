package com.viabus.api.service;

import com.viabus.api.domain.entity.AssentoViagem;
import com.viabus.api.domain.entity.Onibus;
import com.viabus.api.domain.entity.Rota;
import com.viabus.api.domain.entity.Viagem;
import com.viabus.api.domain.enums.StatusAssento;
import com.viabus.api.domain.enums.StatusViagem;
import com.viabus.api.dto.AssentoResponseDTO;
import com.viabus.api.dto.MapaAssentoResponse;
import com.viabus.api.dto.StatusViagemRequestDTO;
import com.viabus.api.dto.ViagemRequestDTO;
import com.viabus.api.dto.ViagemResponseDTO;
import com.viabus.api.exception.BusinessException;
import com.viabus.api.exception.ResourceNotFoundException;
import com.viabus.api.repository.AssentoViagemRepository;
import com.viabus.api.repository.ViagemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ViagemService {

    private static final int COTA_INICIAL_ID_JOVEM_GRATUITA = 2;
    private static final int COTA_INICIAL_ID_JOVEM_DESCONTO = 2;
    private static final Set<StatusViagem> STATUS_COM_OCUPACAO_DE_FROTA =
            EnumSet.of(StatusViagem.AGENDADA, StatusViagem.EM_ANDAMENTO);

    private final ViagemRepository viagemRepository;
    private final AssentoViagemRepository assentoViagemRepository;
    private final RotaService rotaService;
    private final OnibusService onibusService;

    @Transactional
    public ViagemResponseDTO agendar(ViagemRequestDTO dto) {
        Rota rota = rotaService.buscarEntidadePorId(dto.rotaId());
        if (!Boolean.TRUE.equals(rota.getAtiva())) {
            throw new BusinessException("Não é possível agendar viagem em uma rota inativa.");
        }

        Onibus onibus = onibusService.buscarEntidadePorId(dto.onibusId());
        if (!Boolean.TRUE.equals(onibus.getAtivo())) {
            throw new BusinessException("Não é possível escalar um ônibus inativo para a viagem.");
        }

        LocalDateTime partida = dto.dataHoraPartida();
        LocalDateTime chegadaPrevista = partida.plusMinutes(rota.getTempoEstimadoMinutos());

        boolean conflitoHorario = viagemRepository.existsConflitoHorarioOnibus(
                onibus.getId(),
                partida,
                chegadaPrevista,
                STATUS_COM_OCUPACAO_DE_FROTA
        );
        if (conflitoHorario) {
            throw new BusinessException("O ônibus informado já está escalado em outra viagem com horário conflitante.");
        }

        Viagem viagem = Viagem.builder()
                .rota(rota)
                .onibus(onibus)
                .dataHoraPartida(partida)
                .dataHoraChegadaPrevista(chegadaPrevista)
                .precoPassagem(dto.precoPassagem())
                .vagasIdJovemGratuitas(COTA_INICIAL_ID_JOVEM_GRATUITA)
                .vagasIdJovemDesconto(COTA_INICIAL_ID_JOVEM_DESCONTO)
                .status(StatusViagem.AGENDADA)
                .versao(0L)
                .build();

        Viagem viagemSalva = viagemRepository.save(viagem);
        gerarAssentosDaViagem(viagemSalva, onibus.getTotalAssentos());

        return ViagemResponseDTO.fromEntity(viagemSalva);
    }

    @Transactional(readOnly = true)
    public List<ViagemResponseDTO> listarViagens(
            Long rotaId,
            Long origemId,
            Long destinoId,
            LocalDate dataPartida
    ) {
        LocalDateTime inicio = dataPartida != null ? dataPartida.atStartOfDay() : null;
        LocalDateTime fim = dataPartida != null ? dataPartida.atTime(LocalTime.MAX) : null;

        return viagemRepository.buscarViagensPorFiltros(rotaId, origemId, destinoId, inicio, fim)
                .stream()
                .map(ViagemResponseDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public ViagemResponseDTO buscarPorId(Long id) {
        return ViagemResponseDTO.fromEntity(buscarEntidadePorId(id));
    }

    @Transactional(readOnly = true)
    public MapaAssentoResponse consultarMapaAssentos(Long viagemId) {
        Viagem viagem = buscarEntidadePorId(viagemId);
        List<AssentoResponseDTO> assentos = assentoViagemRepository
                .findByViagemIdOrderByNumeroAssentoAsc(viagemId)
                .stream()
                .map(AssentoResponseDTO::fromEntity)
                .toList();

        return MapaAssentoResponse.of(viagem, assentos);
    }

    @Transactional
    public ViagemResponseDTO atualizarStatus(Long id, StatusViagemRequestDTO dto) {
        Viagem viagem = buscarEntidadePorId(id);

        if (viagem.getStatus() == StatusViagem.CANCELADA || viagem.getStatus() == StatusViagem.FINALIZADA) {
            throw new BusinessException("Não é possível alterar o status de uma viagem já " + viagem.getStatus() + ".");
        }

        viagem.setStatus(dto.status());
        return ViagemResponseDTO.fromEntity(viagemRepository.save(viagem));
    }

    @Transactional(readOnly = true)
    public Viagem buscarEntidadePorId(Long id) {
        return viagemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Viagem não encontrada com ID: " + id));
    }

    private void gerarAssentosDaViagem(Viagem viagem, int totalAssentos) {
        List<AssentoViagem> assentos = new ArrayList<>(totalAssentos);
        for (int numero = 1; numero <= totalAssentos; numero++) {
            assentos.add(AssentoViagem.builder()
                    .viagem(viagem)
                    .numeroAssento(numero)
                    .status(StatusAssento.LIVRE)
                    .versao(0L)
                    .build());
        }
        assentoViagemRepository.saveAll(assentos);
    }
}
