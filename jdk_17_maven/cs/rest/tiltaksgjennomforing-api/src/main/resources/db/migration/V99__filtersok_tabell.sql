create table filter_sok
(
    sok_id              varchar primary key,
    sist_sokt_tidspunkt timestamp without time zone not null,
    query_parametre     text    not null,
    antall_ganger_sokt  integer not null
);