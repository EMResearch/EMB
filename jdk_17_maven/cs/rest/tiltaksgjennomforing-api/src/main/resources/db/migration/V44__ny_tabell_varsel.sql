create table varsel
(
    id                  uuid primary key,
    lest                boolean,
    identifikator       varchar(11),
    tekst               varchar,
    avtale_id           uuid references avtale (id),
    hendelse_type       varchar,
    tidspunkt           timestamp without time zone not null default now(),
    bjelle              boolean,
    utført_av           varchar,
    mottaker            varchar
);
