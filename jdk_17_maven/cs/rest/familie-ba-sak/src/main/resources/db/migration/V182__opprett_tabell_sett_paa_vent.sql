create TABLE sett_paa_vent
(
    id                      bigint                                      PRIMARY KEY,
    fk_behandling_id        bigint       REFERENCES BEHANDLING (id)     NOT NULL,
    versjon                 bigint       DEFAULT 0                      NOT NULL,
    opprettet_av            VARCHAR      DEFAULT 'VL'                   NOT NULL,
    opprettet_tid           TIMESTAMP(3) DEFAULT localtimestamp         NOT NULL,
    frist                   TIMESTAMP(3)                                NOT NULL,
    aktiv                   BOOLEAN      DEFAULT FALSE                  NOT NULL,
    aarsak                  VARCHAR                                     NOT NULL,
    endret_av               VARCHAR,
    endret_tid              TIMESTAMP(3)
);

create sequence sett_paa_vent_seq increment by 50 start with 1000000 NO CYCLE;

create INDEX sett_paa_vent_fk_behandling_id_idx ON sett_paa_vent(fk_behandling_id);

create UNIQUE INDEX uidx_sett_paa_vent_aktiv ON sett_paa_vent(fk_behandling_id, aktiv)
    WHERE AKTIV = true;


