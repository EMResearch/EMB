create table varslbar_hendelse
(
    id                     uuid primary key,
    tidspunkt              timestamp without time zone not null default now(),
    avtale_id              uuid,
    varslbar_hendelse_type varchar
);

create table sms_varsel
(
    id                uuid primary key,
    varslbar_hendelse uuid,
    status            varchar,
    telefonnummer     varchar(255),
    identifikator     varchar(11),
    meldingstekst       varchar
);