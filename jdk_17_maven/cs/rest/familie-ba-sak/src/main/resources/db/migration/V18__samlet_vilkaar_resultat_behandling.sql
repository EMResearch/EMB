alter table SAMLET_VILKAR_RESULTAT
    add column fk_behandling_id bigint references BEHANDLING (id);

alter table SAMLET_VILKAR_RESULTAT
    add column aktiv boolean default false;

alter table BEHANDLING drop column samlet_vilkar_resultat_id;