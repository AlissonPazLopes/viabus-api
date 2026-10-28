package com.viabus.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ViaBus API - Sistema de Passagens Intermunicipais e Cotas Sociais")
                        .description("""
                                API REST para gestão de transporte rodoviário intermunicipal, frota de ônibus, rotas com cache Redis, \
                                viagens com geração automática de mapa de poltronas, validação de benefício social ID Jovem \
                                (2 vagas 100% gratuitas e 2 vagas com 50% de desconto por viagem), motor de reservas protegido \
                                contra overbooking com Optimistic Locking (@Version), expiração automática (@Scheduled) e \
                                emissão de bilhete de embarque autenticado via SHA-256.""")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("José Alisson Paz Lopes")
                                .email("alissonlino31@gmail.com")
                                .url("https://github.com/AlissonPazLopes/viabus-api"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")));
    }
}
