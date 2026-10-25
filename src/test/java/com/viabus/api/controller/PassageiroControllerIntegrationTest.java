package com.viabus.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viabus.api.dto.BeneficioIdJovemRequest;
import com.viabus.api.dto.PassageiroRequest;
import com.viabus.api.repository.PassageiroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PassageiroControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PassageiroRepository passageiroRepository;

    @BeforeEach
    void setUp() {
        passageiroRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve cadastrar passageiro, buscar por CPF, vincular cartão ID Jovem e validar elegibilidade via REST")
    void deveExecutarFluxoCompletoDePassageiroEIdJovem() throws Exception {
        PassageiroRequest cadastroSemBeneficio = new PassageiroRequest(
                "Beatriz Oliveira",
                "98765432100",
                "beatriz@viabus.com",
                "48999112233",
                LocalDate.now().minusYears(20),
                null,
                null
        );

        String responseJson = mockMvc.perform(post("/api/passageiros")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cadastroSemBeneficio)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.cpf").value("98765432100"))
                .andExpect(jsonPath("$.ativo").value(true))
                .andReturn().getResponse().getContentAsString();

        Long passageiroId = objectMapper.readTree(responseJson).get("id").asLong();

        // Buscar por CPF
        mockMvc.perform(get("/api/passageiros/cpf/{cpf}", "98765432100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(passageiroId))
                .andExpect(jsonPath("$.nome").value("Beatriz Oliveira"));

        // Antes de vincular o cartão ID Jovem, não deve ser elegível
        mockMvc.perform(get("/api/passageiros/{id}/elegibilidade-id-jovem", passageiroId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elegivel").value(false));

        // Vincular cartão ID Jovem via PATCH /api/passageiros/{id}/id-jovem
        BeneficioIdJovemRequest beneficioRequest = new BeneficioIdJovemRequest(
                "IDJ202698765",
                LocalDate.now().plusMonths(8)
        );

        mockMvc.perform(patch("/api/passageiros/{id}/id-jovem", passageiroId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(beneficioRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroIdJovem").value("IDJ202698765"));

        // Consultar elegibilidade novamente -> agora elegível (20 anos, ativo e dentro da validade)
        mockMvc.perform(get("/api/passageiros/{id}/elegibilidade-id-jovem", passageiroId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elegivel").value(true))
                .andExpect(jsonPath("$.idade").value(20))
                .andExpect(jsonPath("$.numeroIdJovem").value("IDJ202698765"));
    }

    @Test
    @DisplayName("Deve validar CPF com 11 dígitos numéricos e recusar passageiro fora da idade do ID Jovem na elegibilidade")
    void deveValidarBeanValidationEIdadeForaDaFaixa() throws Exception {
        PassageiroRequest cpfInvalido = new PassageiroRequest(
                "Teste CPF Inválido",
                "12345ABC",
                "invalido@viabus.com",
                "11999990000",
                LocalDate.now().minusYears(22),
                null,
                null
        );

        mockMvc.perform(post("/api/passageiros")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cpfInvalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.cpf").exists());

        // Cadastra passageiro com 33 anos portando cartão ID Jovem e verifica inelegibilidade por idade
        PassageiroRequest passageiro33Anos = new PassageiroRequest(
                "Carlos Adulto",
                "12312312399",
                "carlos@viabus.com",
                "11977776666",
                LocalDate.now().minusYears(33),
                "IDJ-ADULTO-33",
                LocalDate.now().plusMonths(4)
        );

        String resp = mockMvc.perform(post("/api/passageiros")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(passageiro33Anos)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(resp).get("id").asLong();

        mockMvc.perform(get("/api/passageiros/{id}/elegibilidade-id-jovem", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elegivel").value(false))
                .andExpect(jsonPath("$.idade").value(33));
    }
}
