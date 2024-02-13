alter table institusjon
    alter column tss_ekstern_id drop not null;
alter table institusjon
    alter column org_nummer set not null;
