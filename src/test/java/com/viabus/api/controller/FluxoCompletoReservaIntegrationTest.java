package com.viabus.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.viabus.api.domain.enums.TipoLeito;
import com.viabus.api.domain.enums.TipoTarifa;
import com.viabus.api.dto.CidadeRequestDTO;
import com.viabus.api.dto.OnibusRequestDTO;
import com.viabus.api.dto.PassageiroRequest;
import com.viabus.api.dto.ReservaRequest;
import com.viabus.api.dto.RotaRequestDTO;
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

import static org.hamcrest.Matchers.hasLength;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FluxoCompletoReservaIntegrationTest {

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

    @BeforeEach
    void setUp() {
        reservaRepository.deleteAll();
        assentoViagemRepository.deleteAll();
        viagemRepository.deleteAll();
        passageiroRepository.deleteAll();
        rotaRepository.deleteAll();
        onibusRepository.deleteAll();
        cidadeRepository.deleteAll();
    }

    @Test
    @DisplayName("E2E: Deve executar o ciclo completo desde o cadastro de cidades/ônibus/rota/viagem até validação ID Jovem, reserva, confirmação e emissão de bilhete")
    void deveExecutarCicloCompletoDeReservaEEmissaoDeBilhete() throws Exception {
        // 1. Cadastrar Cidades de Origem e Destino
        String respOrigem = mockMvc.perform(post("/api/cidades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CidadeRequestDTO("Fortaleza", "CE"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long origemId = objectMapper.readTree(respOrigem).get("id").asLong();

        String respDestino = mockMvc.perform(post("/api/cidades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CidadeRequestDTO("Natal", "RN"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long destinoId = objectMapper.readTree(respDestino).get("id").asLong();

        // 2. Cadastrar Ônibus na Frota
        OnibusRequestDTO onibusDto = new OnibusRequestDTO("VBU7E21", "Marcopolo Paradiso G8 1800 DD", TipoLeito.SEMI_LEITO, 20, true);
        String respOnibus = mockMvc.perform(post("/api/onibus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(onibusDto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long onibusId = objectMapper.readTree(respOnibus).get("id").asLong();

        // 3. Cadastrar Rota Intermunicipal
        RotaRequestDTO rotaDto = new RotaRequestDTO(origemId, destinoId, new BigDecimal("537.00"), 420, true);
        String respRota = mockMvc.perform(post("/api/rotas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rotaDto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long rotaId = objectMapper.readTree(respRota).get("id").asLong();

        // 4. Agendar Viagem (gera automaticamente 20 poltronas LIVRE e cotas 2 gratuitas + 2 desconto ID Jovem)
        ViagemRequestDTO viagemDto = new ViagemRequestDTO(
                rotaId,
                onibusId,
                LocalDateTime.now().plusDays(5).withHour(8).withMinute(0).withSecond(0).withNano(0),
                new BigDecimal("140.00")
        );
        String respViagem = mockMvc.perform(post("/api/viagens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(viagemDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.vagasIdJovemGratuitas").value(2))
                .andExpect(jsonPath("$.vagasIdJovemDesconto").value(2))
                .andReturn().getResponse().getContentAsString();
        Long viagemId = objectMapper.readTree(respViagem).get("id").asLong();

        // 5. Cadastrar Passageiro beneficiário do ID Jovem e validar elegibilidade
        PassageiroRequest passageiroDto = new PassageiroRequest(
                "Mariana Cavalcanti",
                "55566677788",
                "mariana.e2e@viabus.com",
                "85999887766",
                LocalDate.now().minusYears(21),
                "IDJ2026E2E01",
                LocalDate.now().plusMonths(10)
        );
        String respPassageiro = mockMvc.perform(post("/api/passageiros")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(passageiroDto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long passageiroId = objectMapper.readTree(respPassageiro).get("id").asLong();

        mockMvc.perform(get("/api/passageiros/{id}/elegibilidade-id-jovem", passageiroId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elegivel").value(true));

        // 6. Realizar Reserva na cota ID_JOVEM_DESCONTO (50% de R$ 140,00 = R$ 70,00 -> PENDENTE_PAGAMENTO)
        ReservaRequest reservaDto = new ReservaRequest(viagemId, passageiroId, 10, TipoTarifa.ID_JOVEM_DESCONTO);
        String respReserva = mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reservaDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDENTE_PAGAMENTO"))
                .andExpect(jsonPath("$.statusAssento").value("RESERVADO"))
                .andExpect(jsonPath("$.valorOriginal").value(140.00))
                .andExpect(jsonPath("$.valorFinal").value(70.00))
                .andReturn().getResponse().getContentAsString();

        JsonNode reservaNode = objectMapper.readTree(respReserva);
        Long reservaId = reservaNode.get("id").asLong();
        String codigoReserva = reservaNode.get("codigoReserva").asText();

        // 7. Confirmar Pagamento da Reserva
        mockMvc.perform(patch("/api/reservas/{id}/confirmar", reservaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMADA"))
                .andExpect(jsonPath("$.statusAssento").value("OCUPADO"));

        // 8. Consultar Mapa de Assentos da Viagem (19 livres de 20 e 1 vaga de desconto ID Jovem restante)
        mockMvc.perform(get("/api/viagens/{id}/assentos", viagemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAssentos").value(20))
                .andExpect(jsonPath("$.assentosLivres").value(19))
                .andExpect(jsonPath("$.vagasIdJovemDesconto").value(1));

        // 9. Emitir Bilhete de Embarque com CPF Mascarado e Hash SHA-256
        mockMvc.perform(get("/api/reservas/{id}/bilhete", reservaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoReserva").value(codigoReserva))
                .andExpect(jsonPath("$.nomePassageiro").value("Mariana Cavalcanti"))
                .andExpect(jsonPath("$.cpfMascarado").value("***.666.777-**"))
                .andExpect(jsonPath("$.cidadeOrigem").value("Fortaleza/CE"))
                .andExpect(jsonPath("$.cidadeDestino").value("Natal/RN"))
                .andExpect(jsonPath("$.numeroAssento").value(10))
                .andExpect(jsonPath("$.valorPago").value(70.00))
                .andExpect(jsonPath("$.hashValidacao", hasLength(64)));

        // 10. Verificar disponibilidade da documentação OpenAPI 3 (/v3/api-docs)
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("ViaBus API - Sistema de Passagens Intermunicipais e Cotas Sociais"))
                .andExpect(jsonPath("$.info.version").value("1.0.0"));
    }
}
