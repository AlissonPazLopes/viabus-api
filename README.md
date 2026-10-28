# 🚌 ViaBus API — Sistema de Reserva de Passagens Intermunicipais e Cotas Sociais

![Java 21](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot 3](https://img.shields.io/badge/Spring_Boot-3.3.4-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![PostgreSQL 16](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Redis 7](https://img.shields.io/badge/Redis-7-DC382D?style=for-the-badge&logo=redis&logoColor=white)
![Flyway](https://img.shields.io/badge/Flyway-Migrations-CC0200?style=for-the-badge&logo=flyway&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Swagger OpenAPI 3](https://img.shields.io/badge/Swagger-OpenAPI_3.0-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)

---

## 📌 Visão Geral do Projeto

O **ViaBus API** é uma API REST corporativa desenvolvida em **Java 21** e **Spring Boot 3** para gestão completa de **transporte rodoviário intermunicipal**, contemplando o cadastro de cidades, frota de ônibus, rotas intermunicipais cacheadas no **Redis**, agendamento de viagens com geração automática do mapa de poltronas, aplicação das cotas legais do programa **ID Jovem**, motor de reservas com proteção contra **overbooking** via **Optimistic Locking (`@Version`)**, expiração automática de reservas pendentes (`@Scheduled`) e emissão de **bilhete de embarque autenticado com hash SHA-256**.

---

## 🏗️ Arquitetura e Stack Tecnológica

```text
viabus-api/
├── src/main/java/com/viabus/api/
│   ├── config/        # Configuração OpenAPI 3 / Swagger
│   ├── controller/    # Endpoints REST documentados com @Tag e @Operation
│   ├── domain/
│   │   ├── entity/    # Entidades JPA (Cidade, Onibus, Rota, Viagem, AssentoViagem, Passageiro, Reserva)
│   │   └── enums/     # Enums de domínio (TipoLeito, StatusViagem, StatusAssento, TipoTarifa, StatusReserva)
│   ├── dto/           # Records imutáveis de Request/Response com Bean Validation
│   ├── exception/     # Tratamento global (@RestControllerAdvice) e respostas padronizadas (HTTP 400/404/409)
│   ├── repository/    # Interfaces Spring Data JPA com JPQL customizado
│   ├── scheduler/     # Rotina agendada (@Scheduled) para expiração de reservas e estorno de assentos/cotas
│   └── service/       # Regras de negócio, controle transacional, cache Redis e cotas do ID Jovem
└── src/main/resources/
    ├── db/migration/  # Migrations versionadas do Flyway (V1 a V4)
    └── application.yml
```

| Camada / Tecnologia | Descrição |
| :--- | :--- |
| **Linguagem & Runtime** | Java 21 LTS (Records, Pattern Matching, Switch Expressions) |
| **Framework Principal** | Spring Boot 3.3.4 (Web, Data JPA, Validation, Cache, Scheduling) |
| **Banco de Dados Relacional** | PostgreSQL 16 (produção/local) e H2 em memória (testes automatizados) |
| **Versionamento de Schema** | Flyway SQL Migrations (`V1` a `V4`) |
| **Cache Distribuído** | Redis 7 Alpine (`@Cacheable` e `@CacheEvict` em Cidades e Rotas) |
| **Documentação Interativa** | SpringDoc OpenAPI 3 / Swagger UI |
| **Containerização & CI** | Dockerfile Multi-Stage, Docker Compose e GitHub Actions CI |

---

## 🔒 Destaque Técnico: Controle de Concorrência (`@Version` / Optimistic Locking)

Em sistemas de venda de passagens rodoviárias, múltiplos passageiros podem tentar reservar a **mesma poltrona** ou a **última vaga da cota social ID Jovem** no mesmo milissegundo.

Para impedir **overbooking** sem degradar a escalabilidade com bloqueios pessimistas de longa duração:
1. As entidades críticas **`Viagem`**, **`AssentoViagem`** e **`Reserva`** possuem a coluna `versao` anotada com `@Version` do JPA/Hibernate.
2. Durante a transação de reserva (`@Transactional` em `ReservaService`), o Hibernate executa:
   ```sql
   UPDATE assentos_viagem
      SET status = 'RESERVADO', versao = versao + 1
    WHERE id = ? AND versao = ?
   ```
3. Caso duas requisições concorrentes leiam a mesma versão da poltrona, apenas a primeira confirma o `COMMIT`; a segunda lança `ObjectOptimisticLockingFailureException`, que é interceptada e convertida em **`ConflitoConcorrenciaException` (HTTP `409 Conflict`)** pelo `GlobalExceptionHandler`.

---

## 🎓 Regras de Negócio das Cotas Sociais (Programa ID Jovem)

Cada viagem agendada nasce automaticamente com as cotas regulamentares do benefício social **ID Jovem**:
- **2 vagas com 100% de gratuidade** (`TipoTarifa.ID_JOVEM_GRATUITO`):
  - Valor final `R$ 0,00`.
  - A reserva já nasce com status **`CONFIRMADA`**, `dataConfirmacao` preenchida e poltrona **`OCUPADO`**.
- **2 vagas com 50% de desconto** (`TipoTarifa.ID_JOVEM_DESCONTO`):
  - Valor final equivalente a `50%` do `precoPassagem` da viagem.
  - Nasce como **`PENDENTE_PAGAMENTO`** com janela de 15 minutos para confirmação e poltrona **`RESERVADO`**.

### Critérios de Elegibilidade (`PassageiroService.verificarElegibilidadeIdJovem`)
Para utilizar qualquer uma das cotas sociais, o passageiro deve obrigatoriamente:
1. Estar com cadastro **ativo**;
2. Possuir **idade entre 15 e 29 anos** (calculada a partir da `dataNascimento`);
3. Possuir número de **cartão ID Jovem vinculado**;
4. Estar com a **data de validade do cartão ID Jovem vigente** (`validadeIdJovem >= hoje`).

> **Estorno Automático de Cota:** Se uma reserva `ID_JOVEM_GRATUITO` ou `ID_JOVEM_DESCONTO` for **cancelada** ou **expirar**, a poltrona retorna imediatamente para `LIVRE` e a vaga da cota social correspondente é **devolvida** ao saldo da `Viagem`.

---

## ⚡ Estratégia de Cache com Redis e Expiração Automática (`@Scheduled`)

- **Cache Distribuído (`cidades` e `rotas`):**
  - Consultas de cidades (`/api/cidades`) e rotas intermunicipais (`/api/rotas`) são armazenadas no **Redis** com `@Cacheable` e TTL configurável (`300.000 ms` = 5 minutos).
  - Operações de criação ou atualização (`POST` / `PUT`) executam `@CacheEvict(allEntries = true)` garantindo consistência imediata.
- **Expiração de Reservas Pendentes (`ExpiracaoReservaScheduler`):**
  - Reservas `COMERCIAL` e `ID_JOVEM_DESCONTO` em estado `PENDENTE_PAGAMENTO` possuem validade de **15 minutos** (`dataExpiracao`).
  - O job `@Scheduled(fixedDelayString = "${viabus.scheduler.expiracao-ms:60000}")` varre periodicamente as reservas vencidas, altera o status para **`EXPIRADA`**, libera a poltrona para **`LIVRE`** e restitui a cota do ID Jovem na viagem.

---

## 🌐 Tabela Completa de Endpoints da API

### 🩺 Health Check
| Método | Endpoint | Descrição | Status HTTP |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/health` | Verifica disponibilidade e horário do servidor | `200` |

### 🏙️ Cidades (`/api/cidades`)
| Método | Endpoint | Descrição | Status HTTP |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/cidades` | Cadastra nova cidade (invalida cache `cidades`) | `201`, `400` |
| `PUT` | `/api/cidades/{id}` | Atualiza dados de uma cidade | `200`, `400`, `404` |
| `GET` | `/api/cidades?uf=SP` | Lista todas as cidades ou filtra por UF (cacheado) | `200` |
| `GET` | `/api/cidades/{id}` | Busca cidade por ID (cacheado) | `200`, `404` |

### 🚍 Frota de Ônibus (`/api/onibus`)
| Método | Endpoint | Descrição | Status HTTP |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/onibus` | Cadastra novo ônibus na frota | `201`, `400` |
| `PUT` | `/api/onibus/{id}` | Atualiza dados do ônibus | `200`, `400`, `404` |
| `GET` | `/api/onibus?apenasAtivos=true` | Lista todos os ônibus ou apenas ativos | `200` |
| `GET` | `/api/onibus/{id}` | Busca ônibus por ID | `200`, `404` |

### 🛣️ Rotas Intermunicipais (`/api/rotas`)
| Método | Endpoint | Descrição | Status HTTP |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/rotas` | Cadastra rota intermunicipal (origem ≠ destino) | `201`, `400`, `404` |
| `PUT` | `/api/rotas/{id}` | Atualiza rota intermunicipal | `200`, `400`, `404` |
| `GET` | `/api/rotas?origemId=1&destinoId=2` | Lista rotas ou filtra por origem/destino (cacheado) | `200` |
| `GET` | `/api/rotas/{id}` | Busca rota por ID (cacheado) | `200`, `404` |

### 🗓️ Viagens e Mapa de Assentos (`/api/viagens`)
| Método | Endpoint | Descrição | Status HTTP |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/viagens` | Agenda viagem, gera poltronas e inicializa cotas ID Jovem | `201`, `400`, `404` |
| `GET` | `/api/viagens` | Lista/filtra viagens (`rotaId`, `origemId`, `destinoId`, `dataPartida`) | `200` |
| `GET` | `/api/viagens/{id}` | Busca detalhes da viagem por ID | `200`, `404` |
| `GET` | `/api/viagens/{id}/assentos` | Consulta mapa de poltronas e saldo de cotas ID Jovem | `200`, `404` |
| `PATCH` | `/api/viagens/{id}/status` | Atualiza status da viagem (`AGENDADA`, `EM_ANDAMENTO`, `FINALIZADA`, `CANCELADA`) | `200`, `400`, `404` |

### 👤 Passageiros e Elegibilidade ID Jovem (`/api/passageiros`)
| Método | Endpoint | Descrição | Status HTTP |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/passageiros` | Cadastra passageiro (CPF de 11 dígitos e e-mail únicos) | `201`, `400` |
| `GET` | `/api/passageiros` | Lista passageiros cadastrados | `200` |
| `GET` | `/api/passageiros/{id}` | Busca passageiro por ID | `200`, `404` |
| `GET` | `/api/passageiros/cpf/{cpf}` | Busca passageiro pelo CPF | `200`, `404` |
| `PATCH` | `/api/passageiros/{id}/id-jovem` | Vincula ou renova cartão ID Jovem do passageiro | `200`, `400`, `404` |
| `GET` | `/api/passageiros/{id}/elegibilidade-id-jovem` | Verifica elegibilidade ao benefício ID Jovem (15 a 29 anos) | `200`, `404` |

### 🎟️ Reservas e Emissão de Bilhetes (`/api/reservas`)
| Método | Endpoint | Descrição | Status HTTP |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/reservas` | Cria reserva com Optimistic Locking e tarifação social/comercial | `201`, `400`, `404`, `409` |
| `PATCH` | `/api/reservas/{id}/confirmar` | Confirma pagamento e ocupa definitivamente a poltrona | `200`, `400`, `404`, `409` |
| `PATCH` | `/api/reservas/{id}/cancelar` | Cancela reserva, libera poltrona e estorna cota ID Jovem | `200`, `400`, `404`, `409` |
| `GET` | `/api/reservas/{id}` | Busca reserva por ID | `200`, `404` |
| `GET` | `/api/reservas/codigo/{codigo}` | Busca reserva pelo código localizador (`VB-XXXXXXXX`) | `200`, `404` |
| `GET` | `/api/reservas/passageiro/{passageiroId}` | Lista histórico de reservas do passageiro | `200`, `404` |
| `GET` | `/api/reservas/{id}/bilhete` | Emite bilhete de embarque com CPF mascarado e hash SHA-256 | `200`, `400`, `404` |
| `POST` | `/api/reservas/processar-expiradas` | Aciona sob demanda a expiração de reservas vencidas | `200` |

---

## 🚀 Como Executar o Projeto

### Pré-requisitos
- **Java 21+**
- **Maven 3.9+** (ou utilize o `./mvnw` incluso)
- **Docker** e **Docker Compose**

### Opção 1: Subindo Infraestrutura (PostgreSQL + Redis) e Executando via Maven
```bash
# 1. Subir os containers do PostgreSQL 16 e Redis 7
docker compose up -d postgres redis

# 2. Executar a aplicação Spring Boot
./mvnw spring-boot:run
```

### Opção 2: Subindo Tudo Containerizado (API + PostgreSQL + Redis)
```bash
docker compose up --build -d
```

### Executando a Suíte de Testes Automatizados (Unitários, Integração e E2E)
Os testes utilizam o perfil `test` com banco **H2 em memória** e cache `simple`, rodando de forma rápida e independente de infraestrutura externa:
```bash
./mvnw clean test
```

---

## 📖 Documentação Interativa (Swagger UI / OpenAPI 3)

Com a aplicação em execução na porta `8080`, acesse:
- **Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON Spec:** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

---

## 👨‍💻 Autor

Desenvolvido por **José Alisson Paz Lopes**
- GitHub: [@AlissonPazLopes](https://github.com/AlissonPazLopes)
