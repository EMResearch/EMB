CREATE TABLE TOTRINNSKONTROLL
(
    ID               BIGINT PRIMARY KEY,
    FK_BEHANDLING_ID BIGINT                              NOT NULL,
    VERSJON          bigint       default 0              not null,
    OPPRETTET_AV     VARCHAR      default 'VL'           not null,
    OPPRETTET_TID    TIMESTAMP(3) default localtimestamp not null,
    ENDRET_AV        VARCHAR,
    ENDRET_TID       TIMESTAMP(3),
    AKTIV            BOOLEAN      DEFAULT TRUE           NOT NULL,
    SAKSBEHANDLER    VARCHAR                             NOT NULL,
    BESLUTTER        VARCHAR,
    GODKJENT         BOOLEAN      DEFAULT TRUE
);

CREATE SEQUENCE TOTRINNSKONTROLL_SEQ INCREMENT BY 50 START WITH 1000000 NO CYCLE;
CREATE INDEX ON TOTRINNSKONTROLL (FK_BEHANDLING_ID);

/*  Forsikrer at kun en rad er aktiv */
CREATE UNIQUE INDEX UIDX_TOTRINNSKONTROLL_01
    ON TOTRINNSKONTROLL
        (
         (CASE
              WHEN aktiv = true
                  THEN fk_behandling_id
             END),
         (CASE
              WHEN aktiv = true
                  THEN aktiv
             END)
            );