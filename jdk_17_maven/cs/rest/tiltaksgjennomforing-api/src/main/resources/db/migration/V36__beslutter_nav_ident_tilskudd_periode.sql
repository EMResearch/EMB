alter table tilskudd_periode
    add column godkjent_av_nav_ident varchar;
alter table tilskudd_periode
    add column godkjent_tidspunkt timestamp without time zone;
