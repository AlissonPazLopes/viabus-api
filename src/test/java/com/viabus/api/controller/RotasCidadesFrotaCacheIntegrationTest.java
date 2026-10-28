package com.viabus.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viabus.api.domain.enums.TipoLeito;
import com.viabus.api.dto.CidadeRequestDTO;
import com.viabus.api.dto.OnibusRequestDTO;
import com.viabus.api.dto.RotaRequestDTO;
import com.viabus.api.repository.AssentoViagemRepository;
import com.viabus.api.repository.CidadeRepository;
import com.viabus.api.repository.OnibusRepository;
import com.viabus.api.repository.ReservaRepository;
import com.viabus.api.repository.RotaRepository;
import com.viabus.api.repository.ViagemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RotasCidadesFrotaCacheIntegrationTest {

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
    private CidadeRepository cidadeRepository;

    @Autowired
    private OnibusRepository onibusRepository;

    @Autowired
    private RotaRepository rotaRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        reservaRepository.deleteAll();
        assentoViagemRepository.deleteAll();
        viagemRepository.deleteAll();
        rotaRepository.deleteAll();
        onibusRepository.deleteAll();
        cidadeRepository.deleteAll();
        cacheManager.getCacheNames().forEach(name -> {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        });
    }

    @Test
    @DisplayName("Deve cadastrar cidades, validar duplicidade por UF e verificar funcionamento do cache 'cidades'")
    void deveGerenciarCidadesEValidarCache() throws Exception {
        CidadeRequestDTO sp = new CidadeRequestDTO("São Paulo", "SP");

        mockMvc.perform(post("/api/cidades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sp)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nome").value("São Paulo"))
                .andExpect(jsonPath("$.uf").value("SP"));

        // Duplicidade na mesma UF deve retornar 400 Bad Request
        mockMvc.perform(post("/api/cidades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sp)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());

        // Listagem preenche o cache "cidades"
        mockMvc.perform(get("/api/cidades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        Cache cidadesCache = Objects.requireNonNull(cacheManager.getCache("cidades"));
        assertThat(cidadesCache.getNativeCache()).isNotNull();

        // Novo cadastro invalida o cache (@CacheEvict) e reflete na listagem
        CidadeRequestDTO rj = new CidadeRequestDTO("Rio de Janeiro", "RJ");
        mockMvc.perform(post("/api/cidades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rj)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/cidades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("Deve cadastrar ônibus na frota e impedir placas duplicadas")
    void deveGerenciarFrotaDeOnibus() throws Exception {
        OnibusRequestDTO onibus = new OnibusRequestDTO("VBU1A23", "Volvo B450R", TipoLeito.LEITO, 36, true);

        mockMvc.perform(post("/api/onibus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(onibus)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.placa").value("VBU1A23"))
                .andExpect(jsonPath("$.tipoLeito").value("LEITO"))
                .andExpect(jsonPath("$.totalAssentos").value(36));

        // Placa duplicada deve retornar 400 Bad Request
        mockMvc.perform(post("/api/onibus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(onibus)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/onibus?apenasAtivos=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("Deve cadastrar rota intermunicipal, impedir origem igual ao destino e validar cache 'rotas'")
    void deveGerenciarRotasEValidarCache() throws Exception {
        String respOrigem = mockMvc.perform(post("/api/cidades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CidadeRequestDTO("Belo Horizonte", "MG"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String respDestino = mockMvc.perform(post("/api/cidades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CidadeRequestDTO("Vitória", "ES"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long origemId = objectMapper.readTree(respOrigem).get("id").asLong();
        Long destinoId = objectMapper.readTree(respDestino).get("id").asLong();

        // Origem igual ao destino deve falhar com 400 Bad Request
        RotaRequestDTO rotaInvalida = new RotaRequestDTO(origemId, origemId, new BigDecimal("520.00"), 480, true);
        mockMvc.perform(post("/api/rotas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rotaInvalida)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("A cidade de origem e a cidade de destino não podem ser iguais."));

        // Rota válida
        RotaRequestDTO rotaValida = new RotaRequestDTO(origemId, destinoId, new BigDecimal("524.00"), 490, true);
        mockMvc.perform(post("/api/rotas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rotaValida)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cidadeOrigem.nome").value("Belo Horizonte"))
                .andExpect(jsonPath("$.cidadeDestino.nome").value("Vitória"));

        // Busca por origem e destino (armazena no cache "rotas")
        mockMvc.perform(get("/api/rotas")
                        .param("origemId", origemId.toString())
                        .param("destinoId", destinoId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        Cache rotasCache = Objects.requireNonNull(cacheManager.getCache("rotas"));
        String cacheKey = "origem:" + origemId + ":destino:" + destinoId;
        assertThat(rotasCache.get(cacheKey)).isNotNull();
    }
}
