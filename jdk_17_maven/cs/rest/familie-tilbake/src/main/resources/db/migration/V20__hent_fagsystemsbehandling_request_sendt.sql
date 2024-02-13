CREATE TABLE hent_fagsystemsbehandling_request_sendt (
    id                UUID PRIMARY KEY,
    ekstern_fagsak_id VARCHAR                             NOT NULL,
    ytelsestype       VARCHAR                             NOT NULL,
    ekstern_id        VARCHAR                             NOT NULL,
    respons           TEXT,
    versjon           BIGINT                              NOT NULL,
    opprettet_av      VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid     TIMESTAMP(3) DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av         VARCHAR,
    endret_tid        TIMESTAMP(3)
);

COMMENT ON TABLE hent_fagsystemsbehandling_request_sendt
    IS 'Tabell for å lagre hentFagsystemsbehandling data som sendes til fagsystem via kafka';

COMMENT ON COLUMN hent_fagsystemsbehandling_request_sendt.id
    IS 'Primary key';

COMMENT ON COLUMN hent_fagsystemsbehandling_request_sendt.ekstern_fagsak_id
    IS 'Saksnummer (som gsak har mottatt)';

COMMENT ON COLUMN hent_fagsystemsbehandling_request_sendt.ytelsestype
    IS 'Ytelsestypen til fagsystemsbehandling';

COMMENT ON COLUMN hent_fagsystemsbehandling_request_sendt.ekstern_id
    IS 'Referansen til fagsystemsbehandling';

COMMENT ON COLUMN hent_fagsystemsbehandling_request_sendt.respons
    IS 'Respons-en mottas fra fagsystem på Kafka';

CREATE UNIQUE INDEX ON hent_fagsystemsbehandling_request_sendt (ekstern_fagsak_id, ytelsestype, ekstern_id);