alter table avtale add column sist_endret timestamp default now();
alter table avtale drop column versjon;
