create table arena_rydde_avtale
(
    id     uuid primary key,
    avtale uuid references avtale (id)
);