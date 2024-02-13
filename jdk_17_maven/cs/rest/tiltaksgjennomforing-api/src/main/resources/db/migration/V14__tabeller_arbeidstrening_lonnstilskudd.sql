alter table avtale add column tiltakstype                   varchar not null default 'ARBEIDSTRENING';
alter table avtale add column arbeidsgiver_kontonummer      varchar(11);
alter table avtale add column stillingtype                  varchar;
alter table avtale add column stillingbeskrivelse           varchar;
alter table avtale add column lonnstilskudd_prosent         integer;
alter table avtale add column manedslonn                    integer;
alter table avtale add column feriepengesats                decimal;
alter table avtale add column arbeidsgiveravgift            decimal;