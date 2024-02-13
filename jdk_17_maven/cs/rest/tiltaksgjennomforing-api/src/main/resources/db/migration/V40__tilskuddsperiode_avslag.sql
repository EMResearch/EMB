alter table tilskudd_periode add column avslagsforklaring varchar;
alter table tilskudd_periode add column avslått_av_nav_ident varchar;
alter table tilskudd_periode add column avslått_tidspunkt timestamp without time zone;

create table tilskudd_periode_avslagsårsaker
(
    tilskudd_periode_id  uuid references tilskudd_periode(id),
    avslagsårsaker varchar,
    primary key (tilskudd_periode_id, avslagsårsaker)
);