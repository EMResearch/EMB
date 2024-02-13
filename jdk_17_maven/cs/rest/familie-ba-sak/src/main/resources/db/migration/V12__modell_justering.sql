alter table fagsak add column status varchar(50) default 'OPPRETTET';

alter table behandling add column status varchar(50) default 'OPPRETTET';

alter table behandling_vedtak rename to vedtak;
alter sequence BEHANDLING_VEDTAK_SEQ RENAME TO VEDTAK_SEQ;


alter table behandling_vedtak_barn rename to vedtak_barn;
alter table vedtak_barn rename column fk_behandling_vedtak_id to fk_vedtak_id;
alter sequence BEHANDLING_VEDTAK_BARN_SEQ RENAME TO VEDTAK_BARN_SEQ;