alter table avtale_innhold add foreign key (avtale) references avtale (id);
create index on avtale_innhold(avtale);