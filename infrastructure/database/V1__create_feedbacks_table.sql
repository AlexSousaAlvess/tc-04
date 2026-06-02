CREATE TABLE IF NOT EXISTS feedbacks (
    id           BIGSERIAL    PRIMARY KEY,
    feedback_id  VARCHAR(36)  NOT NULL UNIQUE,
    descricao    TEXT         NOT NULL,
    nota         INTEGER      NOT NULL CHECK (nota >= 0 AND nota <= 10),
    urgencia     VARCHAR(10)  NOT NULL CHECK (urgencia IN ('CRITICA', 'MEDIA', 'BAIXA')),
    criado_em    TIMESTAMP    NOT NULL,
    processado_em TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_feedbacks_criado_em  ON feedbacks (criado_em);
CREATE INDEX idx_feedbacks_urgencia   ON feedbacks (urgencia);
CREATE INDEX idx_feedbacks_nota       ON feedbacks (nota);
