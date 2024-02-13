create table bjelle_varsel
(
    id                uuid primary key,
    varslbar_hendelse uuid,
    lest              boolean,
    identifikator     varchar(11),
    varslingstekst    varchar,
    avtale_id         uuid,
    tidspunkt         timestamp without time zone not null default now()
);