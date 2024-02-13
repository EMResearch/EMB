CREATE TABLE avstemmingsfil (
    id            UUID PRIMARY KEY NOT NULL,
    navn          VARCHAR          NOT NULL,
    innhold       BYTEA            NOT NULL,
    versjon       BIGINT           NOT NULL,
    opprettet_av  VARCHAR      DEFAULT 'VL',
    opprettet_tid TIMESTAMP(3) DEFAULT LOCALTIMESTAMP,
    endret_av     VARCHAR,
    endret_tid    TIMESTAMP(3)
);
