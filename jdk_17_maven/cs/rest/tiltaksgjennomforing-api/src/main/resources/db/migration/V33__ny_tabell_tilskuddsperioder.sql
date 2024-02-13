create table tilskudd_periode
(
    id             uuid primary key,
    avtale_innhold uuid,
    beløp          integer,
    start_dato     date,
    slutt_dato     date
);