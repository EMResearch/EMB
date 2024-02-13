create index tilkjent_ytelse_utbetalingsoppdrag_not_null_idx on tilkjent_ytelse (utbetalingsoppdrag) where utbetalingsoppdrag is not null;
create index aty_type_idx on andel_tilkjent_ytelse (type);
