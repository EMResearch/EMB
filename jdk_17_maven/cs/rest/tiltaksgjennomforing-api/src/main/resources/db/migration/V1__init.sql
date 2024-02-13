CREATE TABLE avtale (
  id uuid primary key,
  opprettet_tidspunkt timestamp without time zone not null default now(),
  versjon integer not null default 1,

  deltaker_fornavn varchar(255),
  deltaker_etternavn varchar(255),
  deltaker_fnr varchar(11),

  bedrift_navn varchar(255),
  bedrift_nr varchar(255),

  arbeidsgiver_fnr varchar(11),
  arbeidsgiver_fornavn varchar(255),
  arbeidsgiver_etternavn varchar(255),
  arbeidsgiver_tlf varchar(255),

  veileder_nav_ident varchar(7),
  veileder_fornavn varchar(255),
  veileder_etternavn varchar(255),
  veileder_tlf varchar(255),

  oppfolging varchar(255),
  tilrettelegging varchar(255),

  start_dato date,
  arbeidstrening_lengde integer,
  arbeidstrening_stillingprosent integer,

  godkjent_av_deltaker boolean,
  godkjent_av_arbeidsgiver boolean,
  godkjent_av_veileder boolean
);

CREATE TABLE maal (
  id uuid primary key,
  opprettet_tidspunkt timestamp without time zone not null default now(),
  kategori varchar(255),
  beskrivelse varchar(255),
  avtale uuid
);


CREATE TABLE oppgave (
  id uuid primary key,
  opprettet_tidspunkt timestamp without time zone not null default now(),
  tittel varchar(255),
  beskrivelse varchar(255),
  opplaering varchar(255),
  avtale uuid
);
