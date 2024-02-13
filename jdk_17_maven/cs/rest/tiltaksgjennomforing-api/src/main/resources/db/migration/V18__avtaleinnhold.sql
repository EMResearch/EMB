create table avtale_innhold as
select id,
       id as avtale,
       deltaker_fornavn,
       deltaker_etternavn,
       deltaker_tlf,
       bedrift_navn,
       arbeidsgiver_fornavn,
       arbeidsgiver_etternavn,
       arbeidsgiver_tlf,
       veileder_fornavn,
       veileder_etternavn,
       veileder_tlf,
       oppfolging,
       tilrettelegging,
       start_dato,
       slutt_dato,
       stillingprosent,
       journalpost_id,
       godkjent_av_deltaker,
       godkjent_av_arbeidsgiver,
       godkjent_av_veileder,
       godkjent_pa_vegne_av,
       ikke_bank_id,
       reservert,
       digital_kompetanse,
       arbeidsgiver_kontonummer,
       stillingtype,
       stillingbeskrivelse,
       lonnstilskudd_prosent,
       manedslonn,
       feriepengesats,
       arbeidsgiveravgift
from avtale;

alter table avtale_innhold add column versjon integer;
update avtale_innhold set versjon = 1;

alter table avtale drop column deltaker_fornavn;
alter table avtale drop column deltaker_etternavn;
alter table avtale drop column deltaker_tlf;
alter table avtale drop column bedrift_navn;
alter table avtale drop column arbeidsgiver_fornavn;
alter table avtale drop column arbeidsgiver_etternavn;
alter table avtale drop column arbeidsgiver_tlf;
alter table avtale drop column veileder_fornavn;
alter table avtale drop column veileder_etternavn;
alter table avtale drop column veileder_tlf;
alter table avtale drop column oppfolging;
alter table avtale drop column tilrettelegging;
alter table avtale drop column start_dato;
alter table avtale drop column slutt_dato;
alter table avtale drop column stillingprosent;
alter table avtale drop column journalpost_id;
alter table avtale drop column godkjent_av_deltaker;
alter table avtale drop column godkjent_av_arbeidsgiver;
alter table avtale drop column godkjent_av_veileder;
alter table avtale drop column godkjent_pa_vegne_av;
alter table avtale drop column ikke_bank_id;
alter table avtale drop column reservert;
alter table avtale drop column digital_kompetanse;
alter table avtale drop column arbeidsgiver_kontonummer;
alter table avtale drop column stillingtype;
alter table avtale drop column stillingbeskrivelse;
alter table avtale drop column lonnstilskudd_prosent;
alter table avtale drop column manedslonn;
alter table avtale drop column feriepengesats;
alter table avtale drop column arbeidsgiveravgift;

alter table maal rename column avtale to avtale_innhold;
alter table oppgave rename column avtale to avtale_innhold;