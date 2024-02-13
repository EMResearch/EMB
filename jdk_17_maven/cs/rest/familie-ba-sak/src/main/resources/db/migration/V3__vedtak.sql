create table BEHANDLING_VEDTAK
(
    id                      bigint                                      primary key,
    fk_behandling_id        bigint       references BEHANDLING (id)     not null,
    versjon                 bigint       default 0                      not null,
    opprettet_av            VARCHAR(20)  default 'VL'                   not null,
    opprettet_tid           TIMESTAMP(3) default localtimestamp         not null,
    ansvarlig_saksbehandler VARCHAR(50)                                 not null,
    vedtaksdato             TIMESTAMP(3) default localtimestamp         not null,
    stonad_fom              TIMESTAMP(3)                                not null,
    stonad_tom              TIMESTAMP(3)                                not null,
    stonad_brev_markdown    TEXT,
    endret_av               VARCHAR(20),
    endret_tid              TIMESTAMP(3)
);

CREATE SEQUENCE BEHANDLING_VEDTAK_SEQ INCREMENT BY 50 START WITH 1000000 NO CYCLE;
create index on BEHANDLING_VEDTAK (fk_behandling_id);

alter table BEHANDLING add column behandling_type VARCHAR(50);