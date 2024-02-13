CREATE TABLE LOGG
(
    ID                  BIGINT PRIMARY KEY,
    OPPRETTET_AV        VARCHAR             DEFAULT 'VL'           NOT NULL,
    OPPRETTET_TID       TIMESTAMP(3)        DEFAULT localtimestamp NOT NULL,
    FK_BEHANDLING_ID    BIGINT              REFERENCES behandling (id)   NOT NULL,
    TYPE                VARCHAR             NOT NULL,
    TITTEL              VARCHAR             NOT NULL,
    ROLLE               VARCHAR             NOT NULL,
    TEKST               TEXT                NOT NULL
);

create INDEX ON LOGG (FK_BEHANDLING_ID);
CREATE SEQUENCE LOGG_SEQ INCREMENT BY 50 START WITH 1000000 NO CYCLE;