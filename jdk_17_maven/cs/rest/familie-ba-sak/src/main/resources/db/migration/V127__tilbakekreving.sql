CREATE TABLE tilbakekreving (
    id                           BIGINT PRIMARY KEY,
    fk_vedtak_id                 BIGINT REFERENCES vedtak (id),

    valg                         VARCHAR                                   NOT NULL,
    varsel                       TEXT,
    begrunnelse                  TEXT                                      NOT NULL,
    tilbakekrevingsbehandling_id TEXT,

    opprettet_av                 VARCHAR   DEFAULT 'VL'::CHARACTER VARYING NOT NULL,
    opprettet_tid                TIMESTAMP DEFAULT LOCALTIMESTAMP          NOT NULL,
    endret_av                    VARCHAR,
    endret_tid                   TIMESTAMP(3),
    versjon                      BIGINT    DEFAULT 0
);

CREATE SEQUENCE tilbakekreving_seq INCREMENT BY 50 START WITH 1000000 NO CYCLE;
CREATE INDEX ON tilbakekreving (fk_vedtak_id);