alter table avtale_innhold add column avtale_inngått timestamp without time zone;
update avtale_innhold set avtale_inngått=godkjent_av_veileder;
alter table avtale_innhold add column godkjent_av_beslutter timestamp without time zone;
alter table avtale_innhold add column godkjent_av_beslutter_nav_ident varchar;