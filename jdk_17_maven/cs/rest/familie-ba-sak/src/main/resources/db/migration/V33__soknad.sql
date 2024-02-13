CREATE TABLE GR_SOKNAD
(
    ID               BIGINT PRIMARY KEY,
    OPPRETTET_AV     VARCHAR      DEFAULT 'VL'           NOT NULL,
    OPPRETTET_TID    TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    FK_BEHANDLING_ID BIGINT REFERENCES behandling (id)   NOT NULL,
    SOKNAD           TEXT                                NOT NULL,
    AKTIV            BOOLEAN      DEFAULT TRUE           NOT NULL
);

create INDEX ON GR_SOKNAD (FK_BEHANDLING_ID);
CREATE SEQUENCE GR_SOKNAD_SEQ INCREMENT BY 50 START WITH 1000000 NO CYCLE;

CREATE UNIQUE INDEX UIDX_GR_SOKNAD_01
    ON GR_SOKNAD
        ((CASE
              WHEN aktiv = true
                  THEN fk_behandling_id
            END),
         (CASE
              WHEN aktiv = true
                  THEN aktiv
             END));