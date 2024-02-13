CREATE TABLE OPPGAVE (
    id bigint primary key,
    fk_behandling_id bigint references behandling (id) not null,
    gsak_id varchar not null,
    type varchar not null,
    ferdigstilt bool not null,
    opprettet_tid timestamp not null
);

CREATE SEQUENCE OPPGAVE_SEQ INCREMENT BY 50 START WITH 1000000 NO CYCLE;

