package com.viabus.api.service;

import com.viabus.api.domain.entity.AssentoViagem;
import com.viabus.api.domain.entity.Passageiro;
import com.viabus.api.domain.entity.Reserva;
import com.viabus.api.domain.entity.Viagem;
import com.viabus.api.domain.enums.StatusAssento;
import com.viabus.api.domain.enums.StatusReserva;
import com.viabus.api.domain.enums.StatusViagem;
import com.viabus.api.domain.enums.TipoTarifa;
import com.viabus.api.dto.ElegibilidadeIdJovemResponse;
import com.viabus.api.dto.ReservaRequest;
import com.viabus.api.dto.ReservaResponse;
import com.viabus.api.exception.BusinessException;
import com.viabus.api.exception.ConflitoConcorrenciaException;
import com.viabus.api.exception.ResourceNotFoundException;
import com.viabus.api.repository.AssentoViagemRepository;
import com.viabus.api.repository.ReservaRepository;
import com.viabus.api.repository.ViagemRepository;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReservaService {

    private static final int MINUTOS_EXPIRACAO_PAGAMENTO = 15;
    private static final String CARACTERES_CODIGO = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int TAMANHO_SUFIXO_CODIGO = 8;
    private static final Set<StatusReserva> STATUS_RESERVA_ATIVOS =
            EnumSet.of(StatusReserva.PENDENTE_PAGAMENTO, StatusReserva.CONFIRMADA);

    private final SecureRandom secureRandom = new SecureRandom();

    private final ReservaRepository reservaRepository;
    private final ViagemRepository viagemRepository;
    private final AssentoViagemRepository assentoViagemRepository;
    private final ViagemService viagemService;
    private final PassageiroService passageiroService;

    @Transactional
    public ReservaResponse criarReserva(ReservaRequest dto) {
        try {
            Viagem viagem = viagemService.buscarEntidadePorId(dto.viagemId());
            if (viagem.getStatus() != StatusViagem.AGENDADA) {
                throw new BusinessException("Apenas viagens com status AGENDADA permitem novas reservas.");
            }

            Passageiro passageiro = passageiroService.buscarEntidadePorId(dto.passageiroId());
            if (!Boolean.TRUE.equals(passageiro.getAtivo())) {
                throw new BusinessException("Passageiro inativo não pode realizar reservas.");
            }

            if (reservaRepository.existsByViagemIdAndPassageiroIdAndStatusIn(
                    viagem.getId(),
                    passageiro.getId(),
                    STATUS_RESERVA_ATIVOS
            )) {
                throw new BusinessException("O passageiro já possui uma reserva ativa nesta viagem.");
            }

            AssentoViagem assento = assentoViagemRepository
                    .findByViagemIdAndNumeroAssento(viagem.getId(), dto.numeroAssento())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Assento " + dto.numeroAssento() + " não encontrado para a viagem informada."
                    ));

            if (assento.getStatus() != StatusAssento.LIVRE) {
                throw new ConflitoConcorrenciaException(
                        "O assento " + dto.numeroAssento() + " já está " + assento.getStatus() + " e não pode ser reservado."
                );
            }

            TipoTarifa tipoTarifa = dto.tipoTarifa();
            if (tipoTarifa == TipoTarifa.ID_JOVEM_GRATUITO || tipoTarifa == TipoTarifa.ID_JOVEM_DESCONTO) {
                validarEConsumirCotaIdJovem(viagem, passageiro.getId(), tipoTarifa);
            }

            BigDecimal valorOriginal = viagem.getPrecoPassagem().setScale(2, RoundingMode.HALF_UP);
            BigDecimal valorFinal = calcularValorFinal(valorOriginal, tipoTarifa);

            LocalDateTime agora = LocalDateTime.now();
            LocalDateTime dataExpiracao = agora.plusMinutes(MINUTOS_EXPIRACAO_PAGAMENTO);

            StatusReserva statusInicial;
            LocalDateTime dataConfirmacao = null;

            if (tipoTarifa == TipoTarifa.ID_JOVEM_GRATUITO) {
                statusInicial = StatusReserva.CONFIRMADA;
                dataConfirmacao = agora;
                assento.setStatus(StatusAssento.OCUPADO);
            } else {
                statusInicial = StatusReserva.PENDENTE_PAGAMENTO;
                assento.setStatus(StatusAssento.RESERVADO);
            }

            assentoViagemRepository.saveAndFlush(assento);
            viagemRepository.saveAndFlush(viagem);

            Reserva reserva = Reserva.builder()
                    .codigoReserva(gerarCodigoReservaUnico())
                    .viagem(viagem)
                    .passageiro(passageiro)
                    .assentoViagem(assento)
                    .tipoTarifa(tipoTarifa)
                    .valorOriginal(valorOriginal)
                    .valorFinal(valorFinal)
                    .status(statusInicial)
                    .dataReserva(agora)
                    .dataExpiracao(dataExpiracao)
                    .dataConfirmacao(dataConfirmacao)
                    .versao(0L)
                    .build();

            Reserva reservaSalva = reservaRepository.saveAndFlush(reserva);
            return ReservaResponse.fromEntity(reservaSalva);
        } catch (ObjectOptimisticLockingFailureException | OptimisticLockException ex) {
            throw new ConflitoConcorrenciaException(
                    "Conflito de concorrência ao reservar assento ou cota da viagem. Tente novamente.",
                    ex
            );
        }
    }

    @Transactional
    public ReservaResponse confirmarPagamento(Long reservaId) {
        try {
            Reserva reserva = buscarEntidadePorId(reservaId);

            if (reserva.getStatus() != StatusReserva.PENDENTE_PAGAMENTO) {
                throw new BusinessException(
                        "Apenas reservas com status PENDENTE_PAGAMENTO podem ser confirmadas. Status atual: " + reserva.getStatus()
                );
            }

            LocalDateTime agora = LocalDateTime.now();
            if (reserva.getDataExpiracao() != null && reserva.getDataExpiracao().isBefore(agora)) {
                throw new BusinessException("O prazo de pagamento desta reserva expirou.");
            }

            AssentoViagem assento = reserva.getAssentoViagem();
            assento.setStatus(StatusAssento.OCUPADO);
            assentoViagemRepository.saveAndFlush(assento);

            reserva.setStatus(StatusReserva.CONFIRMADA);
            reserva.setDataConfirmacao(agora);

            return ReservaResponse.fromEntity(reservaRepository.saveAndFlush(reserva));
        } catch (ObjectOptimisticLockingFailureException | OptimisticLockException ex) {
            throw new ConflitoConcorrenciaException(
                    "Conflito de concorrência ao confirmar o pagamento da reserva.",
                    ex
            );
        }
    }

    @Transactional
    public ReservaResponse cancelarReserva(Long reservaId) {
        try {
            Reserva reserva = buscarEntidadePorId(reservaId);

            if (reserva.getStatus() == StatusReserva.CANCELADA || reserva.getStatus() == StatusReserva.EXPIRADA) {
                throw new BusinessException("A reserva já se encontra com status " + reserva.getStatus() + ".");
            }

            AssentoViagem assento = reserva.getAssentoViagem();
            assento.setStatus(StatusAssento.LIVRE);
            assentoViagemRepository.saveAndFlush(assento);

            Viagem viagem = reserva.getViagem();
            devolverCotaIdJovemSeAplicavel(viagem, reserva.getTipoTarifa());
            viagemRepository.saveAndFlush(viagem);

            reserva.setStatus(StatusReserva.CANCELADA);
            return ReservaResponse.fromEntity(reservaRepository.saveAndFlush(reserva));
        } catch (ObjectOptimisticLockingFailureException | OptimisticLockException ex) {
            throw new ConflitoConcorrenciaException(
                    "Conflito de concorrência ao cancelar a reserva.",
                    ex
            );
        }
    }

    @Transactional(readOnly = true)
    public ReservaResponse buscarPorId(Long id) {
        return ReservaResponse.fromEntity(buscarEntidadePorId(id));
    }

    @Transactional(readOnly = true)
    public ReservaResponse buscarPorCodigo(String codigoReserva) {
        Reserva reserva = reservaRepository.findByCodigoReservaIgnoreCase(codigoReserva.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Reserva não encontrada com código: " + codigoReserva));
        return ReservaResponse.fromEntity(reserva);
    }

    @Transactional(readOnly = true)
    public List<ReservaResponse> listarPorPassageiro(Long passageiroId) {
        passageiroService.buscarEntidadePorId(passageiroId);
        return reservaRepository.findByPassageiroIdOrderByDataReservaDesc(passageiroId)
                .stream()
                .map(ReservaResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public Reserva buscarEntidadePorId(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva não encontrada com ID: " + id));
    }

    private void validarEConsumirCotaIdJovem(Viagem viagem, Long passageiroId, TipoTarifa tipoTarifa) {
        ElegibilidadeIdJovemResponse elegibilidade = passageiroService.verificarElegibilidadeIdJovem(passageiroId);
        if (!elegibilidade.elegivel()) {
            throw new BusinessException("Passageiro não elegível ao ID Jovem: " + elegibilidade.motivo());
        }

        if (tipoTarifa == TipoTarifa.ID_JOVEM_GRATUITO) {
            if (viagem.getVagasIdJovemGratuitas() == null || viagem.getVagasIdJovemGratuitas() <= 0) {
                throw new BusinessException("Não há mais vagas gratuitas (100%) do ID Jovem disponíveis nesta viagem.");
            }
            viagem.setVagasIdJovemGratuitas(viagem.getVagasIdJovemGratuitas() - 1);
        } else if (tipoTarifa == TipoTarifa.ID_JOVEM_DESCONTO) {
            if (viagem.getVagasIdJovemDesconto() == null || viagem.getVagasIdJovemDesconto() <= 0) {
                throw new BusinessException("Não há mais vagas com desconto (50%) do ID Jovem disponíveis nesta viagem.");
            }
            viagem.setVagasIdJovemDesconto(viagem.getVagasIdJovemDesconto() - 1);
        }
    }

    private void devolverCotaIdJovemSeAplicavel(Viagem viagem, TipoTarifa tipoTarifa) {
        if (tipoTarifa == TipoTarifa.ID_JOVEM_GRATUITO) {
            viagem.setVagasIdJovemGratuitas(viagem.getVagasIdJovemGratuitas() + 1);
        } else if (tipoTarifa == TipoTarifa.ID_JOVEM_DESCONTO) {
            viagem.setVagasIdJovemDesconto(viagem.getVagasIdJovemDesconto() + 1);
        }
    }

    private BigDecimal calcularValorFinal(BigDecimal valorOriginal, TipoTarifa tipoTarifa) {
        return switch (tipoTarifa) {
            case ID_JOVEM_GRATUITO -> BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            case ID_JOVEM_DESCONTO -> valorOriginal.multiply(new BigDecimal("0.50")).setScale(2, RoundingMode.HALF_UP);
            case COMERCIAL -> valorOriginal.setScale(2, RoundingMode.HALF_UP);
        };
    }

    private String gerarCodigoReservaUnico() {
        String codigo;
        do {
            StringBuilder sb = new StringBuilder("VB-");
            for (int i = 0; i < TAMANHO_SUFIXO_CODIGO; i++) {
                int index = secureRandom.nextInt(CARACTERES_CODIGO.length());
                sb.append(CARACTERES_CODIGO.charAt(index));
            }
            codigo = sb.toString();
        } while (reservaRepository.existsByCodigoReservaIgnoreCase(codigo));
        return codigo;
    }
}
