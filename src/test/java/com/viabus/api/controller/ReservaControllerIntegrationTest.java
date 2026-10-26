package com.viabus.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viabus.api.domain.entity.Cidade;
import com.viabus.api.domain.entity.Onibus;
import com.viabus.api.domain.entity.Passageiro;
import com.viabus.api.domain.entity.Rota;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReservaControllerIntegrationTest {

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
    private Passageiro passageiroComum;
    private Passageiro passageiroIdJovem1;
    private Passageiro passageiroIdJovem2;
    private Passageiro passageiroIdJovem3;

    @BeforeEach
    void setUp() throws Exception {
        reservaRepository.deleteAll();
        assentoViagemRepository.deleteAll();
        viagemRepository.deleteAll();
        passageiroRepository.deleteAll();
        rotaRepository.deleteAll();
        onibusRepository.deleteAll();
        cidadeRepository.deleteAll();

        Cidade origem = cidadeRepository.save(Cidade.builder().nome("Recife").uf("PE").build());
        Cidade destino = cidadeRepository.save(Cidade.builder().nome("João Pessoa").uf("PB").build());

        Rota rota = rotaRepository.save(Rota.builder()
                .cidadeOrigem(origem)
                .cidadeDestino(destino)
                .distanciaKm(new BigDecimal("120.00"))
                .tempoEstimadoMinutos(120)
                .ativa(true)
                .build());

        Onibus onibus = onibusRepository.save(Onibus.builder()
                .placa("VBU8R99")
                .modelo("Marcopolo G8")
                .tipoLeito(TipoLeito.CONVENCIONAL)
                .totalAssentos(10)
                .ativo(true)
                .build());

        ViagemRequestDTO viagemRequest = new ViagemRequestDTO(
                rota.getId(),
                onibus.getId(),
                LocalDateTime.now().plusDays(3),
                new BigDecimal("80.00")
        );

        String viagemJson = mockMvc.perform(post("/api/viagens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(viagemRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        viagemId = objectMapper.readTree(viagemJson).get("id").asLong();

        passageiroComum = passageiroRepository.save(Passageiro.builder()
                .nome("Carlos Mendes")
                .cpf("11122233344")
                .email("carlos@email.com")
                .dataNascimento(LocalDate.now().minusYears(35))
                .ativo(true)
                .build());

        passageiroIdJovem1 = passageiroRepository.save(Passageiro.builder()
                .nome("Ana Beatriz")
                .cpf("22233344455")
                .email("ana@email.com")
                .dataNascimento(LocalDate.now().minusYears(19))
                .numeroIdJovem("IDJ0001")
                .validadeIdJovem(LocalDate.now().plusMonths(8))
                .ativo(true)
                .build());

        passageiroIdJovem2 = passageiroRepository.save(Passageiro.builder()
                .nome("Bruno Costa")
                .cpf("33344455566")
                .email("bruno@email.com")
                .dataNascimento(LocalDate.now().minusYears(22))
                .numeroIdJovem("IDJ0002")
                .validadeIdJovem(LocalDate.now().plusMonths(8))
                .ativo(true)
                .build());

        passageiroIdJovem3 = passageiroRepository.save(Passageiro.builder()
                .nome("Daniela Souza")
                .cpf("44455566677")
                .email("daniela@email.com")
                .dataNascimento(LocalDate.now().minusYears(24))
                .numeroIdJovem("IDJ0003")
                .validadeIdJovem(LocalDate.now().plusMonths(8))
                .ativo(true)
                .build());
    }

    @Test
    @DisplayName("Deve realizar reserva comercial, confirmar pagamento, buscar por código e impedir disputa do mesmo assento (409 Conflict)")
    void deveExecutarFluxoReservaComercialEValidarConflitoDeAssento() throws Exception {
        ReservaRequest request = new ReservaRequest(viagemId, passageiroComum.getId(), 1, TipoTarifa.COMERCIAL);

        String respJson = mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.codigoReserva").isNotEmpty())
                .andExpect(jsonPath("$.status").value("PENDENTE_PAGAMENTO"))
                .andExpect(jsonPath("$.statusAssento").value("RESERVADO"))
                .andExpect(jsonPath("$.valorOriginal").value(80.00))
                .andExpect(jsonPath("$.valorFinal").value(80.00))
                .andReturn().getResponse().getContentAsString();

        Long reservaId = objectMapper.readTree(respJson).get("id").asLong();
        String codigoReserva = objectMapper.readTree(respJson).get("codigoReserva").asText();

        // Outro passageiro tentando reservar a mesma poltrona 1 recebe 409 Conflict
        ReservaRequest disputaMesmoAssento = new ReservaRequest(viagemId, passageiroIdJovem1.getId(), 1, TipoTarifa.COMERCIAL);
        mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(disputaMesmoAssento)))
                .andExpect(status().isConflict());

        // Confirma o pagamento da reserva
        mockMvc.perform(patch("/api/reservas/{id}/confirmar", reservaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMADA"))
                .andExpect(jsonPath("$.statusAssento").value("OCUPADO"))
                .andExpect(jsonPath("$.dataConfirmacao").isNotEmpty());

        // Busca por código localizador
        mockMvc.perform(get("/api/reservas/codigo/{codigo}", codigoReserva))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reservaId))
                .andExpect(jsonPath("$.status").value("CONFIRMADA"));

        // Lista reservas por passageiro
        mockMvc.perform(get("/api/reservas/passageiro/{passageiroId}", passageiroComum.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("Deve controlar limite de 2 vagas gratuitas do ID Jovem e estornar a cota na viagem ao cancelar")
    void deveControlarCotasIdJovemEEstornarNoCancelamento() throws Exception {
        // 1ª vaga gratuita ID Jovem
        String resp1 = mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ReservaRequest(viagemId, passageiroIdJovem1.getId(), 2, TipoTarifa.ID_JOVEM_GRATUITO)
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMADA"))
                .andExpect(jsonPath("$.valorFinal").value(0.00))
                .andReturn().getResponse().getContentAsString();

        Long reserva1Id = objectMapper.readTree(resp1).get("id").asLong();

        // 2ª vaga gratuita ID Jovem
        mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ReservaRequest(viagemId, passageiroIdJovem2.getId(), 3, TipoTarifa.ID_JOVEM_GRATUITO)
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMADA"))
                .andExpect(jsonPath("$.valorFinal").value(0.00));

        // 3º passageiro tentando vaga gratuita deve ser bloqueado (esgotou as 2 vagas gratuitas)
        mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ReservaRequest(viagemId, passageiroIdJovem3.getId(), 4, TipoTarifa.ID_JOVEM_GRATUITO)
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Não há mais vagas gratuitas (100%) do ID Jovem disponíveis nesta viagem."));

        // Cancela a 1ª reserva gratuita -> devolve 1 vaga gratuita e libera o assento 2
        mockMvc.perform(patch("/api/reservas/{id}/cancelar", reserva1Id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADA"));

        // Agora o 3º passageiro consegue reservar a vaga gratuita devolvida no assento 2!
        mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ReservaRequest(viagemId, passageiroIdJovem3.getId(), 2, TipoTarifa.ID_JOVEM_GRATUITO)
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMADA"))
                .andExpect(jsonPath("$.numeroAssento").value(2));
    }
}
