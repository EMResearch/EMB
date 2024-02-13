CREATE TABLE FOEDSELSHENDELSE_PRE_LANSERING
(
    id                                       BIGINT PRIMARY KEY,
    fk_behandling_id                         BIGINT                              NOT NULL,
    person_ident                             VARCHAR,
    ny_behandling_hendelse                   TEXT,
    filtreringsregler_input                  TEXT,
    filtreringsregler_output                 TEXT,
    vilkaarsvurderinger_for_foedselshendelse TEXT,
    opprettet_av                             VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid                            TIMESTAMP(3) default localtimestamp NOT NULL,
    endret_av                                VARCHAR,
    endret_tid                               TIMESTAMP(3),
    versjon                                  BIGINT       DEFAULT 0              NOT NULL
);

CREATE SEQUENCE FOEDSELSHENDELSE_PRE_LANSERING_SEQ INCREMENT BY 50 START WITH 1000000 NO CYCLE;
