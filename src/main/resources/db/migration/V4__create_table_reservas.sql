-- Criação da tabela de reservas de passagens
CREATE TABLE reservas (
    id BIGSERIAL PRIMARY KEY,
    codigo_reserva VARCHAR(20) UNIQUE NOT NULL,
    viagem_id BIGINT NOT NULL,
    passageiro_id BIGINT NOT NULL,
    assento_viagem_id BIGINT NOT NULL,
    tipo_tarifa VARCHAR(30) NOT NULL,
    valor_original NUMERIC(10, 2) NOT NULL,
    valor_final NUMERIC(10, 2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    data_reserva TIMESTAMP NOT NULL,
    data_expiracao TIMESTAMP NOT NULL,
    data_confirmacao TIMESTAMP,
    versao BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_reservas_viagem FOREIGN KEY (viagem_id) REFERENCES viagens (id),
    CONSTRAINT fk_reservas_passageiro FOREIGN KEY (passageiro_id) REFERENCES passageiros (id),
    CONSTRAINT fk_reservas_assento_viagem FOREIGN KEY (assento_viagem_id) REFERENCES assentos_viagem (id)
);

CREATE INDEX idx_reservas_viagem_id ON reservas (viagem_id);
CREATE INDEX idx_reservas_passageiro_id ON reservas (passageiro_id);
CREATE INDEX idx_reservas_status_expiracao ON reservas (status, data_expiracao);
