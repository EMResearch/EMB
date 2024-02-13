alter table avtale add dato_godkjent_deltaker timestamp without time zone;
alter table avtale add dato_godkjent_arbeidsgiver timestamp without time zone;
alter table avtale add dato_godkjent_veileder timestamp without time zone;

update avtale set dato_godkjent_deltaker = '2019-01-01T00:00:00.000' where godkjent_av_deltaker is true;
update avtale set dato_godkjent_arbeidsgiver = '2019-01-01T00:00:00.000' where godkjent_av_arbeidsgiver is true;
update avtale set dato_godkjent_veileder = '2019-01-01T00:00:00.000' where godkjent_av_veileder is true;