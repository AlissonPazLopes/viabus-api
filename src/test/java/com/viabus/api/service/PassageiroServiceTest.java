package com.viabus.api.service;

import com.viabus.api.domain.entity.Passageiro;
import com.viabus.api.dto.BeneficioIdJovemRequest;
import com.viabus.api.dto.ElegibilidadeIdJovemResponse;
import com.viabus.api.dto.PassageiroRequest;
import com.viabus.api.dto.PassageiroResponse;
import com.viabus.api.exception.BusinessException;
import com.viabus.api.repository.PassageiroRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PassageiroServiceTest {

    @Mock
    private PassageiroRepository passageiroRepository;

    @InjectMocks
    private PassageiroService passageiroService;

    @Test
    @DisplayName("Deve cadastrar passageiro com sucesso quando CPF, e-mail e ID Jovem são únicos")
    void deveCadastrarPassageiroComSucesso() {
        LocalDate nascimento = LocalDate.now().minusYears(22);
        LocalDate validade = LocalDate.now().plusMonths(6);
        PassageiroRequest request = new PassageiroRequest(
                "Lucas Ferreira",
                "12345678901",
                "lucas@email.com",
                "11999998888",
                nascimento,
                "IDJ20260001",
                validade
        );

        when(passageiroRepository.existsByCpf("12345678901")).thenReturn(false);
        when(passageiroRepository.existsByEmail("lucas@email.com")).thenReturn(false);
        when(passageiroRepository.existsByNumeroIdJovem("IDJ20260001")).thenReturn(false);
        when(passageiroRepository.save(any(Passageiro.class))).thenAnswer(inv -> {
            Passageiro p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        PassageiroResponse response = passageiroService.cadastrar(request);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("12345678901", response.cpf());
        assertEquals("IDJ20260001", response.numeroIdJovem());
        assertTrue(response.ativo());
    }

    @Test
    @DisplayName("Deve impedir cadastro de passageiro com CPF, e-mail ou número de ID Jovem duplicado")
    void deveImpedirCadastroDuplicado() {
        PassageiroRequest request = new PassageiroRequest(
                "Maria Souza",
                "11122233344",
                "maria@email.com",
                "11988887777",
                LocalDate.now().minusYears(20),
                "IDJ999",
                LocalDate.now().plusMonths(3)
        );

        when(passageiroRepository.existsByCpf("11122233344")).thenReturn(true);
        assertThrows(BusinessException.class, () -> passageiroService.cadastrar(request));

        when(passageiroRepository.existsByCpf("11122233344")).thenReturn(false);
        when(passageiroRepository.existsByEmail("maria@email.com")).thenReturn(true);
        assertThrows(BusinessException.class, () -> passageiroService.cadastrar(request));

        when(passageiroRepository.existsByEmail("maria@email.com")).thenReturn(false);
        when(passageiroRepository.existsByNumeroIdJovem("IDJ999")).thenReturn(true);
        assertThrows(BusinessException.class, () -> passageiroService.cadastrar(request));

        verify(passageiroRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve vincular ou atualizar cartão ID Jovem e impedir uso de número de outro passageiro")
    void deveVincularOuAtualizarCartaoIdJovem() {
        Passageiro passageiro = Passageiro.builder()
                .id(10L)
                .nome("Ana Paula")
                .cpf("55566677788")
                .email("ana@email.com")
                .dataNascimento(LocalDate.now().minusYears(19))
                .ativo(true)
                .dataCadastro(LocalDateTime.now())
                .build();

        BeneficioIdJovemRequest dto = new BeneficioIdJovemRequest("IDJ-ANA-10", LocalDate.now().plusDays(90));

        when(passageiroRepository.findById(10L)).thenReturn(Optional.of(passageiro));
        when(passageiroRepository.existsByNumeroIdJovemAndIdNot("IDJ-ANA-10", 10L)).thenReturn(false);
        when(passageiroRepository.save(any(Passageiro.class))).thenAnswer(inv -> inv.getArgument(0));

        PassageiroResponse atualizado = passageiroService.vincularOuAtualizarIdJovem(10L, dto);
        assertEquals("IDJ-ANA-10", atualizado.numeroIdJovem());

        when(passageiroRepository.existsByNumeroIdJovemAndIdNot("IDJ-ANA-10", 10L)).thenReturn(true);
        assertThrows(BusinessException.class, () -> passageiroService.vincularOuAtualizarIdJovem(10L, dto));
    }

    @Test
    @DisplayName("Deve validar todas as 4 regras de elegibilidade ao benefício ID Jovem")
    void deveValidarRegrasDeElegibilidadeIdJovem() {
        // Caso 1: Elegível (ativo, 21 anos, cartão preenchido e válido hoje)
        Passageiro elegivel = Passageiro.builder()
                .id(1L)
                .nome("Jovem Elegível")
                .cpf("10020030040")
                .email("jovem@email.com")
                .dataNascimento(LocalDate.now().minusYears(21))
                .numeroIdJovem("IDJ-OK-01")
                .validadeIdJovem(LocalDate.now())
                .ativo(true)
                .build();

        when(passageiroRepository.findById(1L)).thenReturn(Optional.of(elegivel));
        ElegibilidadeIdJovemResponse resp1 = passageiroService.verificarElegibilidadeIdJovem(1L);
        assertTrue(resp1.elegivel());

        // Caso 2: Passageiro inativo
        elegivel.setAtivo(false);
        ElegibilidadeIdJovemResponse respInativo = passageiroService.verificarElegibilidadeIdJovem(1L);
        assertFalse(respInativo.elegivel());
        assertTrue(respInativo.motivo().contains("inativo"));

        // Caso 3: Sem número de ID Jovem
        elegivel.setAtivo(true);
        elegivel.setNumeroIdJovem(null);
        ElegibilidadeIdJovemResponse respSemCartao = passageiroService.verificarElegibilidadeIdJovem(1L);
        assertFalse(respSemCartao.elegivel());

        // Caso 4: Cartão vencido (ontem)
        elegivel.setNumeroIdJovem("IDJ-OK-01");
        elegivel.setValidadeIdJovem(LocalDate.now().minusDays(1));
        ElegibilidadeIdJovemResponse respVencido = passageiroService.verificarElegibilidadeIdJovem(1L);
        assertFalse(respVencido.elegivel());
        assertTrue(respVencido.motivo().contains("vencido"));

        // Caso 5: Idade fora da faixa 15 a 29 anos (ex: 30 anos)
        elegivel.setValidadeIdJovem(LocalDate.now().plusMonths(2));
        elegivel.setDataNascimento(LocalDate.now().minusYears(30));
        ElegibilidadeIdJovemResponse respIdadeSuperior = passageiroService.verificarElegibilidadeIdJovem(1L);
        assertFalse(respIdadeSuperior.elegivel());
        assertTrue(respIdadeSuperior.motivo().contains("15 a 29 anos"));
    }
}
