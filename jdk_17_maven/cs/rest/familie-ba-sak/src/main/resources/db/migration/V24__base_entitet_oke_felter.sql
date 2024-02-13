alter table fagsak alter column endret_av set data type varchar(512);
alter table behandling alter column endret_av set data type varchar(512);
alter table po_person alter column endret_av set data type varchar(512);
alter table gr_personopplysninger alter column endret_av set data type varchar(512);
alter table vedtak alter column endret_av set data type varchar(512);
alter table vedtak_person alter column endret_av set data type varchar(512);
alter table samlet_vilkar_resultat alter column endret_av set data type varchar(512);
alter table vilkar_resultat alter column endret_av set data type varchar(512);

alter table fagsak alter column opprettet_av set data type varchar(512);
alter table behandling alter column opprettet_av set data type varchar(512);
alter table po_person alter column opprettet_av set data type varchar(512);
alter table gr_personopplysninger alter column opprettet_av set data type varchar(512);
alter table vedtak alter column opprettet_av set data type varchar(512);
alter table vedtak_person alter column opprettet_av set data type varchar(512);
alter table samlet_vilkar_resultat alter column opprettet_av set data type varchar(512);
alter table vilkar_resultat alter column opprettet_av set data type varchar(512);