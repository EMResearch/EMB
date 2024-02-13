alter table VEDTAK drop column resultat;
alter table BEHANDLING add column resultat varchar;

alter table VEDTAK drop column begrunnelse;
alter table BEHANDLING add column begrunnelse TEXT;