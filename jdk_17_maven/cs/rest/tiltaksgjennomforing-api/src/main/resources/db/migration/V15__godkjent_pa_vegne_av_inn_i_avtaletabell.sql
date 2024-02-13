alter table avtale add column ikke_bank_id boolean;
alter table avtale add column reservert boolean;
alter table avtale add column digital_kompetanse boolean;

update avtale a set ikke_bank_id=(select g.ikke_bank_id from godkjent_pa_vegne_grunn g where g.avtale=a.id);
update avtale a set reservert=(select g.reservert from godkjent_pa_vegne_grunn g where g.avtale=a.id);
update avtale a set digital_kompetanse=(select g.digital_kompetanse from godkjent_pa_vegne_grunn g where g.avtale=a.id);
