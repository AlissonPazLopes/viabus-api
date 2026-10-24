-- Criação da tabela de viagens
CREATE TABLE viagens (
    id BIGSERIAL PRIMARY KEY,
    rota_id BIGINT NOT NULL,
    onibus_id BIGINT NOT NULL,
    data_hora_partida TIMESTAMP NOT NULL,
    data_hora_chegada_prevista TIMESTAMP NOT NULL,
    preco_passagem NUMERIC(10, 2) NOT NULL,
    vagas_id_jovem_gratuitas INTEGER NOT NULL DEFAULT 2,
    vagas_id_jovem_desconto INTEGER NOT NULL DEFAULT 2,
    status VARCHAR(30) NOT NULL,
    versao BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_viagens_rota FOREIGN KEY (rota_id) REFERENCES rotas (id),
    CONSTRAINT fk_viagens_onibus FOREIGN KEY (onibus_id) REFERENCES onibus (id)
);

-- Criação da tabela de assentos da viagem (mapa de poltronas)
CREATE TABLE assentos_viagem (
    id BIGSERIAL PRIMARY KEY,
    viagem_id BIGINT NOT NULL,
    numero_assento INTEGER NOT NULL,
    status VARCHAR(30) NOT NULL,
    versao BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_assentos_viagem FOREIGN KEY (viagem_id) REFERENCES viagens (id) ON DELETE CASCADE,
    CONSTRAINT uk_assentos_viagem_numero UNIQUE (viagem_id, numero_assento)
);

CREATE INDEX idx_viagens_rota_partida ON viagens (rota_id, data_hora_partida);
CREATE INDEX idx_viagens_onibus_horario ON viagens (onibus_id, data_hora_partida, data_hora_chegada_prevista);
CREATE INDEX idx_assentos_viagem_id ON assentos_viagem (viagem_id);
