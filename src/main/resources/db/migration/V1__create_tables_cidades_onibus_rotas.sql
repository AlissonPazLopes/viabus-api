-- Criação da tabela de cidades
CREATE TABLE cidades (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    uf VARCHAR(2) NOT NULL,
    CONSTRAINT uk_cidades_nome_uf UNIQUE (nome, uf)
);

-- Criação da tabela de ônibus (frota)
CREATE TABLE onibus (
    id BIGSERIAL PRIMARY KEY,
    placa VARCHAR(10) UNIQUE NOT NULL,
    modelo VARCHAR(100) NOT NULL,
    tipo_leito VARCHAR(30) NOT NULL,
    total_assentos INTEGER NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);

-- Criação da tabela de rotas intermunicipais
CREATE TABLE rotas (
    id BIGSERIAL PRIMARY KEY,
    cidade_origem_id BIGINT NOT NULL,
    cidade_destino_id BIGINT NOT NULL,
    distancia_km NUMERIC(10, 2) NOT NULL,
    tempo_estimado_minutos INTEGER NOT NULL,
    ativa BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_rotas_cidade_origem FOREIGN KEY (cidade_origem_id) REFERENCES cidades (id),
    CONSTRAINT fk_rotas_cidade_destino FOREIGN KEY (cidade_destino_id) REFERENCES cidades (id)
);

CREATE INDEX idx_rotas_origem_destino ON rotas (cidade_origem_id, cidade_destino_id);
