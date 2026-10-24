package com.viabus.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viabus.api.domain.entity.Cidade;
import com.viabus.api.domain.entity.Onibus;
import com.viabus.api.domain.entity.Rota;
import com.viabus.api.domain.enums.StatusViagem;
import com.viabus.api.domain.enums.TipoLeito;
import com.viabus.api.dto.StatusViagemRequestDTO;
import com.viabus.api.dto.ViagemRequestDTO;
import com.viabus.api.repository.AssentoViagemRepository;
import com.viabus.api.repository.CidadeRepository;
import com.viabus.api.repository.OnibusRepository;
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
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ViagemControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AssentoViagemRepository assentoViagemRepository;

    @Autowired
    private ViagemRepository viagemRepository;

    @Autowired
    private RotaRepository rotaRepository;

    @Autowired
    private OnibusRepository onibusRepository;

    @Autowired
    private CidadeRepository cidadeRepository;

    private Rota rotaPadrao;
    private Onibus onibusPadrao;

    @BeforeEach
    void setUp() {
        assentoViagemRepository.deleteAll();
        viagemRepository.deleteAll();
        rotaRepository.deleteAll();
        onibusRepository.deleteAll();
        cidadeRepository.deleteAll();

        Cidade origem = cidadeRepository.save(Cidade.builder().nome("Florianópolis").uf("SC").build());
        Cidade destino = cidadeRepository.save(Cidade.builder().nome("Porto Alegre").uf("RS").build());

        rotaPadrao = rotaRepository.save(Rota.builder()
                .cidadeOrigem(origem)
                .cidadeDestino(destino)
                .distanciaKm(new BigDecimal("460.00"))
                .tempoEstimadoMinutos(360)
                .ativa(true)
                .build());

        onibusPadrao = onibusRepository.save(Onibus.builder()
                .placa("VBU9Z88")
                .modelo("Scania K400")
                .tipoLeito(TipoLeito.SEMI_LEITO)
                .totalAssentos(24)
                .ativo(true)
                .build());
    }

    @Test
    @DisplayName("Deve agendar viagem, inicializar cotas do ID Jovem e gerar automaticamente o mapa de assentos")
    void deveAgendarViagemEConsultarMapaAssentos() throws Exception {
        LocalDateTime partida = LocalDateTime.of(2026, 12, 1, 8, 0);
        ViagemRequestDTO request = new ViagemRequestDTO(
                rotaPadrao.getId(),
                onibusPadrao.getId(),
                partida,
                new BigDecimal("180.00")
        );

        String responseJson = mockMvc.perform(post("/api/viagens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.status").value("AGENDADA"))
                .andExpect(jsonPath("$.vagasIdJovemGratuitas").value(2))
                .andExpect(jsonPath("$.vagasIdJovemDesconto").value(2))
                .andExpect(jsonPath("$.dataHoraChegadaPrevista").value("2026-12-01T14:00:00"))
                .andReturn().getResponse().getContentAsString();

        Long viagemId = objectMapper.readTree(responseJson).get("id").asLong();

        // Consulta do mapa de assentos e cotas da viagem
        mockMvc.perform(get("/api/viagens/{id}/assentos", viagemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viagemId").value(viagemId))
                .andExpect(jsonPath("$.totalAssentos").value(24))
                .andExpect(jsonPath("$.assentosLivres").value(24))
                .andExpect(jsonPath("$.vagasIdJovemGratuitas").value(2))
                .andExpect(jsonPath("$.vagasIdJovemDesconto").value(2))
                .andExpect(jsonPath("$.assentos.length()").value(24))
                .andExpect(jsonPath("$.assentos[0].numeroAssento").value(1))
                .andExpect(jsonPath("$.assentos[0].status").value("LIVRE"))
                .andExpect(jsonPath("$.assentos[23].numeroAssento").value(24))
                .andExpect(jsonPath("$.assentos[23].status").value("LIVRE"));
    }

    @Test
    @DisplayName("Deve bloquear agendamento do mesmo ônibus em horário conflitante e permitir após atualização de status para CANCELADA")
    void deveBloquearConflitoHorarioEPermitirAposCancelamento() throws Exception {
        LocalDateTime partida1 = LocalDateTime.of(2026, 12, 5, 10, 0);
        ViagemRequestDTO viagem1 = new ViagemRequestDTO(
                rotaPadrao.getId(),
                onibusPadrao.getId(),
                partida1,
                new BigDecimal("180.00")
        );

        String resp1 = mockMvc.perform(post("/api/viagens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(viagem1)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long viagem1Id = objectMapper.readTree(resp1).get("id").asLong();

        // Tentativa de agendar o mesmo ônibus às 13:00 (conflita com 10:00 -> 16:00)
        ViagemRequestDTO viagemConflitante = new ViagemRequestDTO(
                rotaPadrao.getId(),
                onibusPadrao.getId(),
                LocalDateTime.of(2026, 12, 5, 13, 0),
                new BigDecimal("180.00")
        );

        mockMvc.perform(post("/api/viagens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(viagemConflitante)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("O ônibus informado já está escalado em outra viagem com horário conflitante."));

        // Cancela a primeira viagem via PATCH /api/viagens/{id}/status
        mockMvc.perform(patch("/api/viagens/{id}/status", viagem1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StatusViagemRequestDTO(StatusViagem.CANCELADA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADA"));

        // Agora o ônibus está liberado para o novo horário
        mockMvc.perform(post("/api/viagens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(viagemConflitante)))
                .andExpect(status().isCreated());
    }
}
