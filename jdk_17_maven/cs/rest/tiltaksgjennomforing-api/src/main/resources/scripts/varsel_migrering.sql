-- OPPRETTET ARBEIDSGIVER
INSERT INTO varsel (id, lest, identifikator, tekst, avtale_id, hendelse_type, tidspunkt, bjelle, utført_av, mottaker)
SELECT uuid_in(md5(random()::text || clock_timestamp()::text)::cstring), true, (select deltaker_fnr from avtale where avtale.id = hendelselogg.avtale_id), 'Avtale er opprettet', avtale_id, hendelse, tidspunkt, true, 'ARBEIDSGIVER', 'DELTAKER'
FROM hendelselogg WHERE hendelse = 'OPPRETTET_AV_ARBEIDSGIVER';

INSERT INTO varsel (id, lest, identifikator, tekst, avtale_id, hendelse_type, tidspunkt, bjelle, utført_av, mottaker)
SELECT uuid_in(md5(random()::text || clock_timestamp()::text)::cstring), true, (select bedrift_nr from avtale where avtale.id = hendelselogg.avtale_id), 'Avtale er opprettet', avtale_id, hendelse, tidspunkt, false, 'ARBEIDSGIVER', 'ARBEIDSGIVER'
FROM hendelselogg WHERE hendelse = 'OPPRETTET_AV_ARBEIDSGIVER';

INSERT INTO varsel (id, lest, identifikator, tekst, avtale_id, hendelse_type, tidspunkt, bjelle, utført_av, mottaker)
SELECT uuid_in(md5(random()::text || clock_timestamp()::text)::cstring), true, (select veileder_nav_ident from avtale where avtale.id = hendelselogg.avtale_id), 'Avtale er opprettet', avtale_id, hendelse, tidspunkt, true, 'ARBEIDSGIVER', 'VEILEDER'
FROM hendelselogg WHERE hendelse = 'OPPRETTET_AV_ARBEIDSGIVER';
-----------------------------------------------------------------------

-- OPPRETTET VEILEDER
INSERT INTO varsel (id, lest, identifikator, tekst, avtale_id, hendelse_type, tidspunkt, bjelle, utført_av, mottaker)
SELECT uuid_in(md5(random()::text || clock_timestamp()::text)::cstring), true, (select deltaker_fnr from avtale where avtale.id = varslbar_hendelse.avtale_id), 'Avtale er opprettet', avtale_id, varslbar_hendelse_type, tidspunkt, true, 'VEILEDER', 'DELTAKER'
FROM varslbar_hendelse WHERE varslbar_hendelse_type = 'OPPRETTET' and exists (select 1 from avtale where avtale.id = varslbar_hendelse.avtale_id);

INSERT INTO varsel (id, lest, identifikator, tekst, avtale_id, hendelse_type, tidspunkt, bjelle, utført_av, mottaker)
SELECT uuid_in(md5(random()::text || clock_timestamp()::text)::cstring), true, (select bedrift_nr from avtale where avtale.id = varslbar_hendelse.avtale_id), 'Avtale er opprettet', avtale_id, varslbar_hendelse_type, tidspunkt, true, 'VEILEDER', 'ARBEIDSGIVER'
FROM varslbar_hendelse WHERE varslbar_hendelse_type = 'OPPRETTET' and exists (select 1 from avtale where avtale.id = varslbar_hendelse.avtale_id);

INSERT INTO varsel (id, lest, identifikator, tekst, avtale_id, hendelse_type, tidspunkt, bjelle, utført_av, mottaker)
SELECT uuid_in(md5(random()::text || clock_timestamp()::text)::cstring), true, (select veileder_nav_ident from avtale where avtale.id = varslbar_hendelse.avtale_id), 'Avtale er opprettet', avtale_id, varslbar_hendelse_type, tidspunkt, false, 'VEILEDER', 'VEILEDER'
FROM varslbar_hendelse WHERE varslbar_hendelse_type = 'OPPRETTET' and exists (select 1 from avtale where avtale.id = varslbar_hendelse.avtale_id);
------------------------------------------

-- GODKJENT_AV_ARBEIDSGIVER
INSERT INTO varsel (id, lest, identifikator, tekst, avtale_id, hendelse_type, tidspunkt, bjelle, utført_av, mottaker)
SELECT uuid_in(md5(random()::text || clock_timestamp()::text)::cstring), true, (select veileder_nav_ident from avtale where avtale.id = varslbar_hendelse.avtale_id), 'Avtale er godkjent av arbeidsgiver', avtale_id, varslbar_hendelse_type, tidspunkt, true, 'ARBEIDSGIVER', 'VEILEDER'
FROM varslbar_hendelse WHERE varslbar_hendelse_type = 'GODKJENT_AV_ARBEIDSGIVER' and exists (select 1 from avtale where avtale.id = varslbar_hendelse.avtale_id);

INSERT INTO varsel (id, lest, identifikator, tekst, avtale_id, hendelse_type, tidspunkt, bjelle, utført_av, mottaker)
SELECT uuid_in(md5(random()::text || clock_timestamp()::text)::cstring), true, (select deltaker_fnr from avtale where avtale.id = varslbar_hendelse.avtale_id), 'Avtale er godkjent av arbeidsgiver', avtale_id, varslbar_hendelse_type, tidspunkt, true, 'ARBEIDSGIVER', 'DELTAKER'
FROM varslbar_hendelse WHERE varslbar_hendelse_type = 'GODKJENT_AV_ARBEIDSGIVER' and exists (select 1 from avtale where avtale.id = varslbar_hendelse.avtale_id);

INSERT INTO varsel (id, lest, identifikator, tekst, avtale_id, hendelse_type, tidspunkt, bjelle, utført_av, mottaker)
SELECT uuid_in(md5(random()::text || clock_timestamp()::text)::cstring), true, (select bedrift_nr from avtale where avtale.id = varslbar_hendelse.avtale_id), 'Avtale er godkjent av arbeidsgiver', avtale_id, varslbar_hendelse_type, tidspunkt, false, 'ARBEIDSGIVER', 'ARBEIDSGIVER'
FROM varslbar_hendelse WHERE varslbar_hendelse_type = 'GODKJENT_AV_ARBEIDSGIVER' and exists (select 1 from avtale where avtale.id = varslbar_hendelse.avtale_id);
-----------------------------------------

-- GODKJENT_AV_VEILEDER
INSERT INTO varsel (id, lest, identifikator, tekst, avtale_id, hendelse_type, tidspunkt, bjelle, utført_av, mottaker)
SELECT uuid_in(md5(random()::text || clock_timestamp()::text)::cstring), true, (select bedrift_nr from avtale where avtale.id = varslbar_hendelse.avtale_id), 'Avtale er godkjent av veileder', avtale_id, varslbar_hendelse_type, tidspunkt, true, 'VEILEDER', 'ARBEIDSGIVER'
FROM varslbar_hendelse WHERE varslbar_hendelse_type = 'GODKJENT_AV_VEILEDER' and exists (select 1 from avtale where avtale.id = varslbar_hendelse.avtale_id);

INSERT INTO varsel (id, lest, identifikator, tekst, avtale_id, hendelse_type, tidspunkt, bjelle, utført_av, mottaker)
SELECT uuid_in(md5(random()::text || clock_timestamp()::text)::cstring), true, (select deltaker_fnr from avtale where avtale.id = varslbar_hendelse.avtale_id), 'Avtale er godkjent av veileder', avtale_id, varslbar_hendelse_type, tidspunkt, true, 'VEILEDER', 'DELTAKER'
FROM varslbar_hendelse WHERE varslbar_hendelse_type = 'GODKJENT_AV_VEILEDER' and exists (select 1 from avtale where avtale.id = varslbar_hendelse.avtale_id);

INSERT INTO varsel (id, lest, identifikator, tekst, avtale_id, hendelse_type, tidspunkt, bjelle, utført_av, mottaker)
SELECT uuid_in(md5(random()::text || clock_timestamp()::text)::cstring), true, (select veileder_nav_ident from avtale where avtale.id = varslbar_hendelse.avtale_id), 'Avtale er godkjent av veileder', avtale_id, varslbar_hendelse_type, tidspunkt, false, 'VEILEDER', 'VEILEDER'
FROM varslbar_hendelse WHERE varslbar_hendelse_type = 'GODKJENT_AV_VEILEDER' and exists (select 1 from avtale where avtale.id = varslbar_hendelse.avtale_id);
-------------------------------------------

-- GODKJENT_AV_DELTAKER
INSERT INTO varsel (id, lest, identifikator, tekst, avtale_id, hendelse_type, tidspunkt, bjelle, utført_av, mottaker)
SELECT uuid_in(md5(random()::text || clock_timestamp()::text)::cstring), true, (select bedrift_nr from avtale where avtale.id = varslbar_hendelse.avtale_id), 'Avtale er godkjent av deltaker', avtale_id, varslbar_hendelse_type, tidspunkt, true, 'DELTAKER', 'ARBEIDSGIVER'
FROM varslbar_hendelse WHERE varslbar_hendelse_type = 'GODKJENT_AV_DELTAKER' and exists (select 1 from avtale where avtale.id = varslbar_hendelse.avtale_id);

INSERT INTO varsel (id, lest, identifikator, tekst, avtale_id, hendelse_type, tidspunkt, bjelle, utført_av, mottaker)
SELECT uuid_in(md5(random()::text || clock_timestamp()::text)::cstring), true, (select veileder_nav_ident from avtale where avtale.id = varslbar_hendelse.avtale_id), 'Avtale er godkjent av deltaker', avtale_id, varslbar_hendelse_type, tidspunkt, true, 'DELTAKER', 'VEILEDER'
FROM varslbar_hendelse WHERE varslbar_hendelse_type = 'GODKJENT_AV_DELTAKER' and exists (select 1 from avtale where avtale.id = varslbar_hendelse.avtale_id);

INSERT INTO varsel (id, lest, identifikator, tekst, avtale_id, hendelse_type, tidspunkt, bjelle, utført_av, mottaker)
SELECT uuid_in(md5(random()::text || clock_timestamp()::text)::cstring), true, (select bedrift_nr from avtale where avtale.id = varslbar_hendelse.avtale_id), 'Avtale er godkjent av deltaker', avtale_id, varslbar_hendelse_type, tidspunkt, false, 'DELTAKER', 'DELTAKER'
FROM varslbar_hendelse WHERE varslbar_hendelse_type = 'GODKJENT_AV_DELTAKER' and exists (select 1 from avtale where avtale.id = varslbar_hendelse.avtale_id);
---------------------------------------------

-- GODKJENT_PAA_VEGNE_AV
INSERT INTO varsel (id, lest, identifikator, tekst, avtale_id, hendelse_type, tidspunkt, bjelle, utført_av, mottaker)
SELECT uuid_in(md5(random()::text || clock_timestamp()::text)::cstring), true, (select bedrift_nr from avtale where avtale.id = varslbar_hendelse.avtale_id), 'Veileder godkjente avtalen på vegne av seg selv og deltaker', avtale_id, varslbar_hendelse_type, tidspunkt, true, 'VEILEDER', 'ARBEIDSGIVER'
FROM varslbar_hendelse WHERE varslbar_hendelse_type = 'GODKJENT_PAA_VEGNE_AV' and exists (select 1 from avtale where avtale.id = varslbar_hendelse.avtale_id);

INSERT INTO varsel (id, lest, identifikator, tekst, avtale_id, hendelse_type, tidspunkt, bjelle, utført_av, mottaker)
SELECT uuid_in(md5(random()::text || clock_timestamp()::text)::cstring), true, (select veileder_nav_ident from avtale where avtale.id = varslbar_hendelse.avtale_id), 'Veileder godkjente avtalen på vegne av seg selv og deltaker', avtale_id, varslbar_hendelse_type, tidspunkt, false, 'VEILEDER', 'VEILEDER'
FROM varslbar_hendelse WHERE varslbar_hendelse_type = 'GODKJENT_PAA_VEGNE_AV' and exists (select 1 from avtale where avtale.id = varslbar_hendelse.avtale_id);

INSERT INTO varsel (id, lest, identifikator, tekst, avtale_id, hendelse_type, tidspunkt, bjelle, utført_av, mottaker)
SELECT uuid_in(md5(random()::text || clock_timestamp()::text)::cstring), true, (select deltaker_fnr from avtale where avtale.id = varslbar_hendelse.avtale_id), 'Veileder godkjente avtalen på vegne av seg selv og deltaker', avtale_id, varslbar_hendelse_type, tidspunkt, true, 'VEILEDER', 'DELTAKER'
FROM varslbar_hendelse WHERE varslbar_hendelse_type = 'GODKJENT_PAA_VEGNE_AV' and exists (select 1 from avtale where avtale.id = varslbar_hendelse.avtale_id);
-------------------------------------------

-- GODKJENNINGER_OPPHEVET_AV_ARBEIDSGIVER
INSERT INTO varsel (id, lest, identifikator, tekst, avtale_id, hendelse_type, tidspunkt, bjelle, utført_av, mottaker)
SELECT uuid_in(md5(random()::text || clock_timestamp()::text)::cstring), true, (select deltaker_fnr from avtale where avtale.id = varslbar_hendelse.avtale_id), 'Avtalens godkjenninger er opphevet av arbeidsgiver', avtale_id, varslbar_hendelse_type, tidspunkt, true, 'ARBEIDSGIVER', 'DELTAKER'
FROM varslbar_hendelse WHERE varslbar_hendelse_type = 'GODKJENNINGER_OPPHEVET_AV_ARBEIDSGIVER' and exists (select 1 from avtale where avtale.id = varslbar_hendelse.avtale_id);

INSERT INTO varsel (id, lest, identifikator, tekst, avtale_id, hendelse_type, tidspunkt, bjelle, utført_av, mottaker)
SELECT uuid_in(md5(random()::text || clock_timestamp()::text)::cstring), true, (select veileder_nav_ident from avtale where avtale.id = varslbar_hendelse.avtale_id), 'Avtalens godkjenninger er opphevet av arbeidsgiver', avtale_id, varslbar_hendelse_type, tidspunkt, false, 'ARBEIDSGIVER', 'VEILEDER'
FROM varslbar_hendelse WHERE varslbar_hendelse_type = 'GODKJENNINGER_OPPHEVET_AV_ARBEIDSGIVER' and exists (select 1 from avtale where avtale.id = varslbar_hendelse.avtale_id);

INSERT INTO varsel (id, lest, identifikator, tekst, avtale_id, hendelse_type, tidspunkt, bjelle, utført_av, mottaker)
SELECT uuid_in(md5(random()::text || clock_timestamp()::text)::cstring), true, (select bedrift_nr from avtale where avtale.id = varslbar_hendelse.avtale_id), 'Avtalens godkjenninger er opphevet av arbeidsgiver', avtale_id, varslbar_hendelse_type, tidspunkt, true, 'ARBEIDSGIVER', 'ARBEIDSGIVER'
FROM varslbar_hendelse WHERE varslbar_hendelse_type = 'GODKJENNINGER_OPPHEVET_AV_ARBEIDSGIVER' and exists (select 1 from avtale where avtale.id = varslbar_hendelse.avtale_id);
---------------------------------------------

-- GODKJENNINGER_OPPHEVET_AV_VEILEDER
INSERT INTO varsel (id, lest, identifikator, tekst, avtale_id, hendelse_type, tidspunkt, bjelle, utført_av, mottaker)
SELECT uuid_in(md5(random()::text || clock_timestamp()::text)::cstring), true, (select bedrift_nr from avtale where avtale.id = varslbar_hendelse.avtale_id), 'Avtalens godkjenninger er opphevet av veileder', avtale_id, varslbar_hendelse_type, tidspunkt, true, 'VEILEDER', 'ARBEIDSGIVER'
FROM varslbar_hendelse WHERE varslbar_hendelse_type = 'GODKJENNINGER_OPPHEVET_AV_VEILEDER' and exists (select 1 from avtale where avtale.id = varslbar_hendelse.avtale_id);

INSERT INTO varsel (id, lest, identifikator, tekst, avtale_id, hendelse_type, tidspunkt, bjelle, utført_av, mottaker)
SELECT uuid_in(md5(random()::text || clock_timestamp()::text)::cstring), true, (select veileder_nav_ident from avtale where avtale.id = varslbar_hendelse.avtale_id), 'Avtalens godkjenninger er opphevet av veileder', avtale_id, varslbar_hendelse_type, tidspunkt, false, 'VEILEDER', 'VEILEDER'
FROM varslbar_hendelse WHERE varslbar_hendelse_type = 'GODKJENNINGER_OPPHEVET_AV_VEILEDER' and exists (select 1 from avtale where avtale.id = varslbar_hendelse.avtale_id);

INSERT INTO varsel (id, lest, identifikator, tekst, avtale_id, hendelse_type, tidspunkt, bjelle, utført_av, mottaker)
SELECT uuid_in(md5(random()::text || clock_timestamp()::text)::cstring), true, (select deltaker_fnr from avtale where avtale.id = varslbar_hendelse.avtale_id), 'Avtalens godkjenninger er opphevet av veileder', avtale_id, varslbar_hendelse_type, tidspunkt, true, 'VEILEDER', 'DELTAKER'
FROM varslbar_hendelse WHERE varslbar_hendelse_type = 'GODKJENNINGER_OPPHEVET_AV_VEILEDER' and exists (select 1 from avtale where avtale.id = varslbar_hendelse.avtale_id);
---------------------------------------------

-- DELT_MED_DELTAKER
INSERT INTO varsel (id, lest, identifikator, tekst, avtale_id, hendelse_type, tidspunkt, bjelle, utført_av, mottaker)
SELECT uuid_in(md5(random()::text || clock_timestamp()::text)::cstring), true, (select deltaker_fnr from avtale where avtale.id = varslbar_hendelse.avtale_id), 'Avtale delt med deltaker', avtale_id, varslbar_hendelse_type, tidspunkt, true, 'VEILEDER', 'DELTAKER'
FROM varslbar_hendelse WHERE varslbar_hendelse_type = 'DELT_MED_DELTAKER' and exists (select 1 from avtale where avtale.id = varslbar_hendelse.avtale_id);

INSERT INTO varsel (id, lest, identifikator, tekst, avtale_id, hendelse_type, tidspunkt, bjelle, utført_av, mottaker)
SELECT uuid_in(md5(random()::text || clock_timestamp()::text)::cstring), true, (select veileder_nav_ident from avtale where avtale.id = varslbar_hendelse.avtale_id), 'Avtale delt med deltaker', avtale_id, varslbar_hendelse_type, tidspunkt, false, 'VEILEDER', 'VEILEDER'
FROM varslbar_hendelse WHERE varslbar_hendelse_type = 'DELT_MED_DELTAKER' and exists (select 1 from avtale where avtale.id = varslbar_hendelse.avtale_id);
--------------------------------------------

-- DELT_MED_ARBEIDSGIVER
INSERT INTO varsel (id, lest, identifikator, tekst, avtale_id, hendelse_type, tidspunkt, bjelle, utført_av, mottaker)
SELECT uuid_in(md5(random()::text || clock_timestamp()::text)::cstring), true, (select deltaker_fnr from avtale where avtale.id = varslbar_hendelse.avtale_id), 'Avtale delt med arbeidsgiver', avtale_id, varslbar_hendelse_type, tidspunkt, true, 'VEILEDER', 'ARBEIDSGIVER'
FROM varslbar_hendelse WHERE varslbar_hendelse_type = 'DELT_MED_ARBEIDSGIVER' and exists (select 1 from avtale where avtale.id = varslbar_hendelse.avtale_id);

INSERT INTO varsel (id, lest, identifikator, tekst, avtale_id, hendelse_type, tidspunkt, bjelle, utført_av, mottaker)
SELECT uuid_in(md5(random()::text || clock_timestamp()::text)::cstring), true, (select veileder_nav_ident from avtale where avtale.id = varslbar_hendelse.avtale_id), 'Avtale delt med arbeidsgiver', avtale_id, varslbar_hendelse_type, tidspunkt, false, 'VEILEDER', 'VEILEDER'
FROM varslbar_hendelse WHERE varslbar_hendelse_type = 'DELT_MED_ARBEIDSGIVER' and exists (select 1 from avtale where avtale.id = varslbar_hendelse.avtale_id);
-------------------------------------------

-- AVBRUTT
INSERT INTO varsel (id, lest, identifikator, tekst, avtale_id, hendelse_type, tidspunkt, bjelle, utført_av, mottaker)
SELECT uuid_in(md5(random()::text || clock_timestamp()::text)::cstring), true, (select veileder_nav_ident from avtale where avtale.id = hendelselogg.avtale_id), 'Avtale avbrutt av veileder', avtale_id, hendelse, tidspunkt, false, 'VEILEDER', 'VEILEDER'
FROM hendelselogg WHERE hendelse = 'AVBRUTT';

INSERT INTO varsel (id, lest, identifikator, tekst, avtale_id, hendelse_type, tidspunkt, bjelle, utført_av, mottaker)
SELECT uuid_in(md5(random()::text || clock_timestamp()::text)::cstring), true, (select bedrift_nr from avtale where avtale.id = hendelselogg.avtale_id), 'Avtale avbrutt av veileder', avtale_id, hendelse, tidspunkt, true, 'VEILEDER', 'ARBEIDSGIVER'
FROM hendelselogg WHERE hendelse = 'AVBRUTT';

INSERT INTO varsel (id, lest, identifikator, tekst, avtale_id, hendelse_type, tidspunkt, bjelle, utført_av, mottaker)
SELECT uuid_in(md5(random()::text || clock_timestamp()::text)::cstring), true, (select deltaker_fnr from avtale where avtale.id = hendelselogg.avtale_id), 'Avtale avbrutt av veileder', avtale_id, hendelse, tidspunkt, true, 'VEILEDER', 'DELTAKER'
FROM hendelselogg WHERE hendelse = 'AVBRUTT';
------------------------------------------

-- LÅST_OPP
INSERT INTO varsel (id, lest, identifikator, tekst, avtale_id, hendelse_type, tidspunkt, bjelle, utført_av, mottaker)
SELECT uuid_in(md5(random()::text || clock_timestamp()::text)::cstring), true, (select veileder_nav_ident from avtale where avtale.id = hendelselogg.avtale_id), 'Avtale låst opp av veileder', avtale_id, hendelse, tidspunkt, false, 'VEILEDER', 'VEILEDER'
FROM hendelselogg WHERE hendelse = 'LÅST_OPP';

INSERT INTO varsel (id, lest, identifikator, tekst, avtale_id, hendelse_type, tidspunkt, bjelle, utført_av, mottaker)
SELECT uuid_in(md5(random()::text || clock_timestamp()::text)::cstring), true, (select bedrift_nr from avtale where avtale.id = hendelselogg.avtale_id), 'Avtale låst opp av veileder', avtale_id, hendelse, tidspunkt, true, 'VEILEDER', 'ARBEIDSGIVER'
FROM hendelselogg WHERE hendelse = 'LÅST_OPP';

INSERT INTO varsel (id, lest, identifikator, tekst, avtale_id, hendelse_type, tidspunkt, bjelle, utført_av, mottaker)
SELECT uuid_in(md5(random()::text || clock_timestamp()::text)::cstring), true, (select deltaker_fnr from avtale where avtale.id = hendelselogg.avtale_id), 'Avtale låst opp av veileder', avtale_id, hendelse, tidspunkt, true, 'VEILEDER', 'DELTAKER'
FROM hendelselogg WHERE hendelse = 'LÅST_OPP';
------------------------------------------

-- GJENOPPRETTET
INSERT INTO varsel (id, lest, identifikator, tekst, avtale_id, hendelse_type, tidspunkt, bjelle, utført_av, mottaker)
SELECT uuid_in(md5(random()::text || clock_timestamp()::text)::cstring), true, (select veileder_nav_ident from avtale where avtale.id = hendelselogg.avtale_id), 'Avtale gjenopprettet', avtale_id, hendelse, tidspunkt, false, 'VEILEDER', 'VEILEDER'
FROM hendelselogg WHERE hendelse = 'GJENOPPRETTET';

INSERT INTO varsel (id, lest, identifikator, tekst, avtale_id, hendelse_type, tidspunkt, bjelle, utført_av, mottaker)
SELECT uuid_in(md5(random()::text || clock_timestamp()::text)::cstring), true, (select bedrift_nr from avtale where avtale.id = hendelselogg.avtale_id), 'Avtale gjenopprettet', avtale_id, hendelse, tidspunkt, true, 'VEILEDER', 'ARBEIDSGIVER'
FROM hendelselogg WHERE hendelse = 'GJENOPPRETTET';

INSERT INTO varsel (id, lest, identifikator, tekst, avtale_id, hendelse_type, tidspunkt, bjelle, utført_av, mottaker)
SELECT uuid_in(md5(random()::text || clock_timestamp()::text)::cstring), true, (select deltaker_fnr from avtale where avtale.id = hendelselogg.avtale_id), 'Avtale gjenopprettet', avtale_id, hendelse, tidspunkt, true, 'VEILEDER', 'DELTAKER'
FROM hendelselogg WHERE hendelse = 'GJENOPPRETTET';
--------------------------------------------

-- NY_VEILEDER
INSERT INTO varsel (id, lest, identifikator, tekst, avtale_id, hendelse_type, tidspunkt, bjelle, utført_av, mottaker)
SELECT uuid_in(md5(random()::text || clock_timestamp()::text)::cstring), true, (select veileder_nav_ident from avtale where avtale.id = hendelselogg.avtale_id), 'Avtale tildelt ny veileder', avtale_id, hendelse, tidspunkt, false, 'VEILEDER', 'VEILEDER'
FROM hendelselogg WHERE hendelse = 'NY_VEILEDER';

INSERT INTO varsel (id, lest, identifikator, tekst, avtale_id, hendelse_type, tidspunkt, bjelle, utført_av, mottaker)
SELECT uuid_in(md5(random()::text || clock_timestamp()::text)::cstring), true, (select bedrift_nr from avtale where avtale.id = hendelselogg.avtale_id), 'Avtale tildelt ny veileder', avtale_id, hendelse, tidspunkt, true, 'VEILEDER', 'ARBEIDSGIVER'
FROM hendelselogg WHERE hendelse = 'NY_VEILEDER';

INSERT INTO varsel (id, lest, identifikator, tekst, avtale_id, hendelse_type, tidspunkt, bjelle, utført_av, mottaker)
SELECT uuid_in(md5(random()::text || clock_timestamp()::text)::cstring), true, (select deltaker_fnr from avtale where avtale.id = hendelselogg.avtale_id), 'Avtale tildelt ny veileder', avtale_id, hendelse, tidspunkt, true, 'VEILEDER', 'DELTAKER'
FROM hendelselogg WHERE hendelse = 'NY_VEILEDER';
--------------------------------------------

-- AVTALE_FORDELT
INSERT INTO varsel (id, lest, identifikator, tekst, avtale_id, hendelse_type, tidspunkt, bjelle, utført_av, mottaker)
SELECT uuid_in(md5(random()::text || clock_timestamp()::text)::cstring), true, (select veileder_nav_ident from avtale where avtale.id = hendelselogg.avtale_id), 'Avtale tildelt veileder', avtale_id, hendelse, tidspunkt, false, 'VEILEDER', 'VEILEDER'
FROM hendelselogg WHERE hendelse = 'AVTALE_FORDELT';

INSERT INTO varsel (id, lest, identifikator, tekst, avtale_id, hendelse_type, tidspunkt, bjelle, utført_av, mottaker)
SELECT uuid_in(md5(random()::text || clock_timestamp()::text)::cstring), true, (select bedrift_nr from avtale where avtale.id = hendelselogg.avtale_id), 'Avtale tildelt veileder', avtale_id, hendelse, tidspunkt, true, 'VEILEDER', 'ARBEIDSGIVER'
FROM hendelselogg WHERE hendelse = 'AVTALE_FORDELT';

INSERT INTO varsel (id, lest, identifikator, tekst, avtale_id, hendelse_type, tidspunkt, bjelle, utført_av, mottaker)
SELECT uuid_in(md5(random()::text || clock_timestamp()::text)::cstring), true, (select deltaker_fnr from avtale where avtale.id = hendelselogg.avtale_id), 'Avtale tildelt veileder', avtale_id, hendelse, tidspunkt, true, 'VEILEDER', 'DELTAKER'
FROM hendelselogg WHERE hendelse = 'AVTALE_FORDELT';

--------------------------------------------

-- ENDRET
INSERT INTO varsel (id, lest, identifikator, tekst, avtale_id, hendelse_type, tidspunkt, bjelle, utført_av, mottaker)
SELECT uuid_in(md5(random()::text || clock_timestamp()::text)::cstring), true, (select veileder_nav_ident from avtale where avtale.id = hendelselogg.avtale_id), 'Avtale endret', avtale_id, hendelse, tidspunkt, false, utført_av, 'VEILEDER'
FROM hendelselogg WHERE hendelse = 'ENDRET';

INSERT INTO varsel (id, lest, identifikator, tekst, avtale_id, hendelse_type, tidspunkt, bjelle, utført_av, mottaker)
SELECT uuid_in(md5(random()::text || clock_timestamp()::text)::cstring), true, (select bedrift_nr from avtale where avtale.id = hendelselogg.avtale_id), 'Avtale endret', avtale_id, hendelse, tidspunkt, false, utført_av, 'ARBEIDSGIVER'
FROM hendelselogg WHERE hendelse = 'ENDRET';

INSERT INTO varsel (id, lest, identifikator, tekst, avtale_id, hendelse_type, tidspunkt, bjelle, utført_av, mottaker)
SELECT uuid_in(md5(random()::text || clock_timestamp()::text)::cstring), true, (select deltaker_fnr from avtale where avtale.id = hendelselogg.avtale_id), 'Avtale endret', avtale_id, hendelse, tidspunkt, false, utført_av, 'DELTAKER'
FROM hendelselogg WHERE hendelse = 'ENDRET';