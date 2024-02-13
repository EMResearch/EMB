create table BEHANDLING_VEDTAK_BARN
(
    id                      bigint                                                  primary key,
    fk_behandling_vedtak_id bigint       references BEHANDLING_VEDTAK (id)          not null,
    fk_person_id            bigint       references po_person (id)                  not null,
    versjon                 bigint       default 0                                  not null,
    opprettet_av            VARCHAR(20)  default 'VL'                               not null,
    opprettet_tid           TIMESTAMP(3) default localtimestamp                     not null,
    stonad_fom              TIMESTAMP(3)                                            not null,
    stonad_tom              TIMESTAMP(3)                                            not null,
    belop                   numeric,
    endret_av               VARCHAR(20),
    endret_tid              TIMESTAMP(3)
);

CREATE SEQUENCE BEHANDLING_VEDTAK_BARN_SEQ INCREMENT BY 50 START WITH 1000000 NO CYCLE;
create index on BEHANDLING_VEDTAK_BARN (fk_behandling_vedtak_id);
create index on BEHANDLING_VEDTAK_BARN (fk_person_id);