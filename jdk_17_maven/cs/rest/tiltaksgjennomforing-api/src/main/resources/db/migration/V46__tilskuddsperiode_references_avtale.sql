delete from tilskudd_periode_avslagsårsaker;
delete from tilskudd_periode;

alter table tilskudd_periode drop column avtale_innhold;
alter table tilskudd_periode add column avtale_id uuid references avtale(id);