update vilkar_resultat set utfall = 'JA' where utfall = 'OPPFYLT';
update vilkar_resultat set utfall = 'NEI' where utfall = 'IKKE_OPPFYLT';
alter table vilkar_resultat rename column utfall to resultat;
