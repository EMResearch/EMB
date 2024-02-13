DROP TABLE IF EXISTS BEHANDLING_STEG_TILSTAND;
CREATE TABLE BEHANDLING_STEG_TILSTAND
(
    id                      BIGINT                            primary key,
    fk_behandling_id        BIGINT references BEHANDLING(id)     not null,
    behandling_steg         VARCHAR                              not null,
    behandling_steg_status  VARCHAR      default 'IKKE_UTFØRT'   not null,
    versjon                 BIGINT       default 0               not null,
    opprettet_av            VARCHAR      default 'VL'            not null,
    opprettet_tid           TIMESTAMP(3) default localtimestamp  not null,
    endret_av               VARCHAR,
    endret_tid              TIMESTAMP(3)
) ;

DROP SEQUENCE IF EXISTS BEHANDLING_STEG_TILSTAND_SEQ;
CREATE SEQUENCE BEHANDLING_STEG_TILSTAND_SEQ INCREMENT BY 50 START WITH 1000000 NO CYCLE;

INSERT INTO
    BEHANDLING_STEG_TILSTAND(id, fk_behandling_id, behandling_steg)
    (
        SELECT
            nextval('BEHANDLING_STEG_TILSTAND_SEQ'), id, steg
        FROM
            BEHANDLING
        WHERE
            steg is not null
    );
