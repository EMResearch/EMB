alter table avtale add column slutt_dato date;
update avtale set slutt_dato = start_dato + interval '7' day * arbeidstrening_lengde;