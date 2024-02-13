CREATE TABLE verge (
    id               BIGINT PRIMARY KEY,
    navn             VARCHAR                             NOT NULL,
    adresse          VARCHAR                             NOT NULL,
    ident            VARCHAR,
    fk_behandling_id BIGINT                              NOT NULL REFERENCES behandling (id),
    versjon          BIGINT       DEFAULT 0              NOT NULL,
    opprettet_av     VARCHAR(20)  DEFAULT 'VL'           NOT NULL,
    opprettet_tid    TIMESTAMP(3) DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av        VARCHAR(20),
    endret_tid       TIMESTAMP(3)
);

CREATE SEQUENCE verge_seq INCREMENT BY 50 START WITH 1000000 NO CYCLE;

CREATE UNIQUE INDEX uidx_verge_navn ON verge (navn);
CREATE UNIQUE INDEX uidx_verge_ident ON verge (ident);
CREATE UNIQUE INDEX uidx_verge_behandling_id ON verge (fk_behandling_id)
