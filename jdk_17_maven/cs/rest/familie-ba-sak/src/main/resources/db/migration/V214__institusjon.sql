CREATE TABLE institusjon (
    id             BIGINT PRIMARY KEY,
    org_nummer     VARCHAR,
    tss_ekstern_id VARCHAR                             NOT NULL,
    versjon        BIGINT       DEFAULT 0              NOT NULL,
    opprettet_av   VARCHAR(20)  DEFAULT 'VL'           NOT NULL,
    opprettet_tid  TIMESTAMP(3) DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av      VARCHAR(20),
    endret_tid     TIMESTAMP(3)
);

CREATE SEQUENCE institusjon_seq INCREMENT BY 50 START WITH 1000000 NO CYCLE;

CREATE UNIQUE INDEX uidx_institusjon_org_nummer ON institusjon (org_nummer);
CREATE UNIQUE INDEX uidx_institusjon_tss_ekstern_id ON institusjon (tss_ekstern_id)