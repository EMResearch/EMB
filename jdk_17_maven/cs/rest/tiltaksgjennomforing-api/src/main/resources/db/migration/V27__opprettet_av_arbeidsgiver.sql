alter table avtale add column opprettet_av_arbeidsgiver boolean not null default false;
alter table avtale add column utkast_akseptert boolean not null default false;