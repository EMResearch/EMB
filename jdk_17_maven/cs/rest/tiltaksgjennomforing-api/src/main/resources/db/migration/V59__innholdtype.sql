alter table avtale_innhold add column innhold_type varchar;
update avtale_innhold set innhold_type = 'INNGÅ' where versjon = 1;
update avtale_innhold set innhold_type = 'LÅSE_OPP' where versjon > 1;