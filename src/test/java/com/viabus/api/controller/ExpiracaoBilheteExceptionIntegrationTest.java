package com.viabus.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viabus.api.domain.entity.Cidade;
import com.viabus.api.domain.entity.Onibus;
import com.viabus.api.domain.entity.Passageiro;
import com.viabus.api.domain.entity.Reserva;
import com.viabus.api.domain.entity.Rota;
import com.viabus.api.domain.entity.Viagem;
import com.viabus.api.domain.enums.TipoLeito;
import com.viabus.api.domain.enums.TipoTarifa;
import com.viabus.api.dto.ReservaRequest;
import com.viabus.api.dto.ViagemRequestDTO;
import com.viabus.api.repository.AssentoViagemRepository;
import com.viabus.api.repository.CidadeRepository;
import com.viabus.api.repository.OnibusRepository;
import com.viabus.api.repository.PassageiroRepository;
import com.viabus.api.repository.ReservaRepository;
import com.viabus.api.repository.RotaRepository;
import com.viabus.api.repository.ViagemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasLength;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExpiracaoBilheteExceptionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private AssentoViagemRepository assentoViagemRepository;

    @Autowired
    private ViagemRepository viagemRepository;

    @Autowired
    private PassageiroRepository passageiroRepository;

    @Autowired
    private RotaRepository rotaRepository;

    @Autowired
    private OnibusRepository onibusRepository;

    @Autowired
    private CidadeRepository cidadeRepository;

    private Long viagemId;
    private Passageiro passageiroIdJovem;
    private Passageiro passageiroComum;

    @BeforeEach
    void setUp() throws Exception {
        reservaRepository.deleteAll();
        assentoViagemRepository.deleteAll();
        viagemRepository.deleteAll();
        passageiroRepository.deleteAll();
        rotaRepository.deleteAll();
        onibusRepository.deleteAll();
        cidadeRepository.deleteAll();

        Cidade origem = cidadeRepository.save(Cidade.builder().nome("Goiânia").uf("GO").build());
        Cidade destino = cidadeRepository.save(Cidade.builder().nome("Brasília").uf("DF").build());

        Rota rota = rotaRepository.save(Rota.builder()
                .cidadeOrigem(origem)
                .cidadeDestino(destino)
                .distanciaKm(new BigDecimal("209.00"))
                .tempoEstimadoMinutos(180)
                .ativa(true)
                .build());

        Onibus onibus = onibusRepository.save(Onibus.builder()
                .placa("VBU6G10")
                .modelo("Marcopolo Paradiso G8")
                .tipoLeito(TipoLeito.EXECUTIVO)
                .totalAssentos(12)
                .ativo(true)
                .build());

        ViagemRequestDTO viagemRequest = new ViagemRequestDTO(
                rota.getId(),
                onibus.getId(),
                LocalDateTime.now().plusDays(2),
                new BigDecimal("90.00")
        );

        String viagemJson = mockMvc.perform(post("/api/viagens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(viagemRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        viagemId = objectMapper.readTree(viagemJson).get("id").asLong();

        passageiroIdJovem = passageiroRepository.save(Passageiro.builder()
                .nome("Gabriel Rocha")
                .cpf("12345678909")
                .email("gabriel@viabus.com")
                .dataNascimento(LocalDate.now().minusYears(20))
                .numeroIdJovem("IDJ6001")
                .validadeIdJovem(LocalDate.now().plusMonths(6))
                .ativo(true)
                .build());

        passageiroComum = passageiroRepository.save(Passageiro.builder()
                .nome("Fernanda Alves")
                .cpf("98765432100")
                .email("fernanda@viabus.com")
                .dataNascimento(LocalDate.now().minusYears(32))
                .ativo(true)
                .build());
    }

    @Test
    @DisplayName("Deve emitir bilhete de embarque com CPF mascarado e hash SHA-256 apenas para reservas CONFIRMADAS")
    void deveEmitirBilheteApenasQuandoConfirmada() throws Exception {
        ReservaRequest request = new ReservaRequest(viagemId, passageiroComum.getId(), 5, TipoTarifa.COMERCIAL);

        String respJson = mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long reservaId = objectMapper.readTree(respJson).get("id").asLong();

        // Enquanto estiver PENDENTE_PAGAMENTO, não permite emitir bilhete (HTTP 400)
        mockMvc.perform(get("/api/reservas/{id}/bilhete", reservaId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensagem").exists())
                .andExpect(jsonPath("$.path").value("/api/reservas/" + reservaId + "/bilhete"));

        // Confirma o pagamento
        mockMvc.perform(patch("/api/reservas/{id}/confirmar", reservaId))
                .andExpect(status().isOk());

        // Agora emite o bilhete com sucesso e valida CPF mascarado e hash SHA-256 (64 hex chars)
        mockMvc.perform(get("/api/reservas/{id}/bilhete", reservaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservaId").value(reservaId))
                .andExpect(jsonPath("$.nomePassageiro").value("Fernanda Alves"))
                .andExpect(jsonPath("$.cpfMascarado").value("***.654.321-**"))
                .andExpect(jsonPath("$.cidadeOrigem").value("Goiânia/GO"))
                .andExpect(jsonPath("$.cidadeDestino").value("Brasília/DF"))
                .andExpect(jsonPath("$.numeroAssento").value(5))
                .andExpect(jsonPath("$.placaOnibus").value("VBU6G10"))
                .andExpect(jsonPath("$.valorPago").value(90.00))
                .andExpect(jsonPath("$.hashValidacao", hasLength(64)));
    }

    @Test
    @DisplayName("Deve processar reservas expiradas sob demanda, liberar assento para LIVRE e devolver cota ID Jovem")
    void deveProcessarReservasExpiradasELiberarAssentoECota() throws Exception {
        ReservaRequest request = new ReservaRequest(viagemId, passageiroIdJovem.getId(), 3, TipoTarifa.ID_JOVEM_DESCONTO);

        String respJson = mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDENTE_PAGAMENTO"))
                .andReturn().getResponse().getContentAsString();

        Long reservaId = objectMapper.readTree(respJson).get("id").asLong();

        // Verifica que a cota de desconto caiu de 2 para 1
        Viagem viagemAntes = viagemRepository.findById(viagemId).orElseThrow();
        assertThat(viagemAntes.getVagasIdJovemDesconto()).isEqualTo(1);

        // Força a dataExpiracao para o passado para simular vencimento dos 15 minutos
        Reserva reserva = reservaRepository.findById(reservaId).orElseThrow();
        reserva.setDataExpiracao(LocalDateTime.now().minusMinutes(5));
        reservaRepository.saveAndFlush(reserva);

        // Aciona o endpoint administrativo POST /api/reservas/processar-expiradas
        mockMvc.perform(post("/api/reservas/processar-expiradas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservasExpiradasProcessadas").value(1));

        // Verifica que a reserva mudou para EXPIRADA, a cota voltou para 2 e o assento 3 ficou LIVRE
        mockMvc.perform(get("/api/reservas/{id}", reservaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXPIRADA"))
                .andExpect(jsonPath("$.statusAssento").value("LIVRE"));

        Viagem viagemDepois = viagemRepository.findById(viagemId).orElseThrow();
        assertThat(viagemDepois.getVagasIdJovemDesconto()).isEqualTo(2);
    }

    @Test
    @DisplayName("Deve validar respostas padronizadas HTTP 400, 404 e 409 do GlobalExceptionHandler")
    void deveValidarRespostasPadronizadasDeErro() throws Exception {
        // HTTP 400 Validation Error
        ReservaRequest invalida = new ReservaRequest(null, null, -1, null);
        mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalida)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.erro").value("Bad Request"))
                .andExpect(jsonPath("$.path").value("/api/reservas"))
                .andExpect(jsonPath("$.errosValidacao").isArray());

        // HTTP 404 Not Found
        mockMvc.perform(get("/api/reservas/{id}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.erro").value("Not Found"))
                .andExpect(jsonPath("$.path").value("/api/reservas/999999"));

        // HTTP 409 Conflict (disputa pela mesma poltrona)
        ReservaRequest primeira = new ReservaRequest(viagemId, passageiroComum.getId(), 8, TipoTarifa.COMERCIAL);
        mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(primeira)))
                .andExpect(status().isCreated());

        ReservaRequest conflito = new ReservaRequest(viagemId, passageiroIdJovem.getId(), 8, TipoTarifa.COMERCIAL);
        mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(conflito)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.erro").value("Conflict"))
                .andExpect(jsonPath("$.path").value("/api/reservas"));
    }
}
