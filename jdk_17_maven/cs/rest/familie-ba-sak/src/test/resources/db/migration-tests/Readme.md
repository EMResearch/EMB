Skriptet som ligg her er ei samanslåing av alle skripta frå main/resources/db/migration, per skrivande stund t.o.m. 247.

Viss du vil gjenskape det som er gjort, køyr skriptet _lagBaseline.sh_.

Merk at skriptet fjernar dei første n radene i flyway_schema_history-tabellen. 
Dette er ein workaround som primært for å ta bort dei to første innslaga i tabellen, som er dei to skripta som ligg i db\init-mappa og dermed ikkje køyrast i test.

Merk at du først må 
1. Starte databasen i ein postgres-container etter oppskrifta i readme
1. Starte applikasjonen og dermed få køyrd flyway-migreringa med desse
1. Pass på at du har postgresql-kommandoar i _path_ eller at du står i postgresql\bin-mappa

Merk at du heilt fint kan leggje inn nye skript i src/db/migration på vanleg måte, men desse vil da bli køyrd som separate steg i kvar test (altså tilsvarande som alle vart før denne endringa).