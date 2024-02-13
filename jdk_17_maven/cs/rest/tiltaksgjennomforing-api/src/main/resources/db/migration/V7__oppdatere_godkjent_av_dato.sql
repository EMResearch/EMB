update avtale set godkjent_av_deltaker = '2019-01-01T00:00:00.000' where gammel_godkjent_av_deltaker is true;
update avtale set godkjent_av_arbeidsgiver = '2019-01-01T00:00:00.000' where gammel_godkjent_av_arbeidsgiver is true;
update avtale set godkjent_av_veileder = '2019-01-01T00:00:00.000' where gammel_godkjent_av_veileder is true;