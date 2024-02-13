CREATE TABLE IF NOT EXISTS brevmottaker (
    id               BIGINT       PRIMARY KEY,
    fk_behandling_id BIGINT       REFERENCES behandling (id) ON DELETE CASCADE NOT NULL,
    type             VARCHAR(50)                             NOT NULL,
    navn             VARCHAR                                 NOT NULL,
    adresselinje_1   VARCHAR                                 NOT NULL,
    adresselinje_2   VARCHAR,
    postnummer       VARCHAR                                 NOT NULL,
    poststed         VARCHAR                                 NOT NULL,
    landkode         VARCHAR(2)                              NOT NULL,
    versjon          BIGINT       DEFAULT 0                  NOT NULL,
    opprettet_av     VARCHAR      DEFAULT 'VL'               NOT NULL,
    opprettet_tid    TIMESTAMP(3) DEFAULT LOCALTIMESTAMP     NOT NULL,
    endret_av        VARCHAR,
    endret_tid       TIMESTAMP(3)
);

CREATE SEQUENCE IF NOT EXISTS brevmottaker_seq INCREMENT BY 50 START WITH 1000000 NO CYCLE;
CREATE INDEX IF NOT EXISTS brevmottaker_fk_behandling_id_idx ON brevmottaker (fk_behandling_id);
