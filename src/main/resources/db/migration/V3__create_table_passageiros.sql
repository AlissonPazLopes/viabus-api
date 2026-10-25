-- Criação da tabela de passageiros e dados do benefício social ID Jovem
CREATE TABLE passageiros (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    cpf VARCHAR(11) NOT NULL UNIQUE,
    email VARCHAR(120) NOT NULL UNIQUE,
    telefone VARCHAR(20),
    data_nascimento DATE NOT NULL,
    numero_id_jovem VARCHAR(30) UNIQUE,
    validade_id_jovem DATE,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    data_cadastro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_passageiros_cpf ON passageiros (cpf);
CREATE INDEX idx_passageiros_numero_id_jovem ON passageiros (numero_id_jovem);
