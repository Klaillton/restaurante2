-- ADR 0002: pedido (estado atual) + pedido_evento (log imutável, só inserts).
-- Corre como "migracao" (dono). Os utilizadores são criados por docker/postgres/init/01-roles.sh.

CREATE TABLE pedido (
    pedido_id     UUID        PRIMARY KEY,               -- gerado no cliente
    estado        TEXT        NOT NULL,                  -- valores/transições: trabalho da feature
    criado_em     TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE pedido_evento (
    evento_id   UUID        PRIMARY KEY,                 -- gerado no cliente; idempotência (ON CONFLICT DO NOTHING)
    pedido_id   UUID        NOT NULL REFERENCES pedido (pedido_id),
    tipo        TEXT        NOT NULL
        CHECK (tipo IN ('CRIADO', 'ALTERADO', 'NA_COZINHA', 'PRONTO', 'ENTREGUE', 'ENVIADO_FATURACAO')),
    payload     JSONB       NOT NULL,                    -- ALTERADO guarda o pedido inteiro (snapshot)
    ocorrido_em TIMESTAMPTZ NOT NULL,                    -- relógio do cliente (só informativo)
    recebido_em TIMESTAMPTZ NOT NULL DEFAULT now(),      -- relógio do servidor
    sequencia   INTEGER     NOT NULL CHECK (sequencia >= 1),
    CONSTRAINT pedido_evento_pedido_sequencia_uk UNIQUE (pedido_id, sequencia)
);

-- A ordem é recebido_em + sequencia dentro do pedido, nunca o relógio do cliente.
CREATE INDEX pedido_evento_recebido_em_idx ON pedido_evento (recebido_em);

-- Permissões (ADR 0002, ponto 4). O app não é dono de nada.
REVOKE ALL ON pedido, pedido_evento FROM PUBLIC;

GRANT SELECT, INSERT ON pedido TO app;
GRANT UPDATE (estado, atualizado_em) ON pedido TO app;

GRANT SELECT, INSERT ON pedido_evento TO app;
REVOKE UPDATE, DELETE, TRUNCATE ON pedido_evento FROM app;
