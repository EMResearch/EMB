--
-- PostgreSQL database dump
--

-- Dumped from database version 14.5 (Debian 14.5-1.pgdg110+1)
-- Dumped by pg_dump version 14.6 (Homebrew)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: aktoer; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.aktoer (
    aktoer_id character varying NOT NULL,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av character varying,
    endret_tid timestamp(3) without time zone
);


--
-- Name: aktoer_til_kompetanse; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.aktoer_til_kompetanse (
    fk_kompetanse_id bigint NOT NULL,
    fk_aktoer_id character varying NOT NULL
);


--
-- Name: aktoer_til_utenlandsk_periodebeloep; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.aktoer_til_utenlandsk_periodebeloep (
    fk_utenlandsk_periodebeloep_id bigint NOT NULL,
    fk_aktoer_id character varying NOT NULL
);


--
-- Name: aktoer_til_valutakurs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.aktoer_til_valutakurs (
    fk_valutakurs_id bigint NOT NULL,
    fk_aktoer_id character varying NOT NULL
);


--
-- Name: andel_tilkjent_ytelse; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.andel_tilkjent_ytelse (
    id bigint NOT NULL,
    fk_behandling_id bigint NOT NULL,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying(512) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT LOCALTIMESTAMP NOT NULL,
    stonad_fom timestamp(3) without time zone NOT NULL,
    stonad_tom timestamp(3) without time zone NOT NULL,
    type character varying(50) NOT NULL,
    kalkulert_utbetalingsbelop numeric,
    endret_av character varying(512),
    endret_tid timestamp(3) without time zone,
    tilkjent_ytelse_id bigint,
    periode_offset bigint,
    forrige_periode_offset bigint,
    kilde_behandling_id bigint,
    prosent numeric NOT NULL,
    sats bigint NOT NULL,
    fk_aktoer_id character varying,
    nasjonalt_periodebelop numeric,
    differanseberegnet_periodebelop numeric
);


--
-- Name: andel_tilkjent_ytelse_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.andel_tilkjent_ytelse_seq
    START WITH 2000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: annen_vurdering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.annen_vurdering (
    id bigint NOT NULL,
    fk_person_resultat_id bigint NOT NULL,
    resultat character varying NOT NULL,
    type character varying NOT NULL,
    begrunnelse text,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av character varying,
    endret_tid timestamp(3) without time zone
);


--
-- Name: annen_vurdering_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.annen_vurdering_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: arbeidsfordeling_pa_behandling; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.arbeidsfordeling_pa_behandling (
    id bigint NOT NULL,
    fk_behandling_id bigint NOT NULL,
    behandlende_enhet_id character varying NOT NULL,
    behandlende_enhet_navn character varying NOT NULL,
    manuelt_overstyrt boolean NOT NULL
);


--
-- Name: arbeidsfordeling_pa_behandling_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.arbeidsfordeling_pa_behandling_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: batch; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.batch (
    id bigint NOT NULL,
    kjoredato timestamp(3) without time zone NOT NULL,
    status character varying(50) DEFAULT 'LEDIG'::character varying NOT NULL
);


--
-- Name: batch_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.batch_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: behandling; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.behandling (
    id bigint NOT NULL,
    fk_fagsak_id bigint,
    versjon bigint DEFAULT 0,
    opprettet_av character varying(512) DEFAULT 'VL'::character varying,
    opprettet_tid timestamp(3) without time zone DEFAULT LOCALTIMESTAMP,
    endret_av character varying(512),
    endret_tid timestamp(3) without time zone,
    behandling_type character varying(50),
    aktiv boolean DEFAULT true,
    status character varying(50) DEFAULT 'OPPRETTET'::character varying,
    kategori character varying(50) DEFAULT 'NATIONAL'::character varying,
    underkategori character varying(50) DEFAULT 'ORDINÆR'::character varying,
    opprettet_aarsak character varying DEFAULT 'MANUELL'::character varying,
    skal_behandles_automatisk boolean DEFAULT false,
    resultat character varying DEFAULT 'IKKE_VURDERT'::character varying NOT NULL,
    overstyrt_endringstidspunkt timestamp(3) without time zone,
    aktivert_tid timestamp(3) without time zone NOT NULL
);


--
-- Name: behandling_migreringsinfo; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.behandling_migreringsinfo (
    id bigint NOT NULL,
    fk_behandling_id bigint NOT NULL,
    migreringsdato date NOT NULL,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av character varying,
    endret_tid timestamp(3) without time zone
);


--
-- Name: behandling_migreringsinfo_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.behandling_migreringsinfo_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: behandling_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.behandling_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: behandling_soknadsinfo; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.behandling_soknadsinfo (
    id bigint NOT NULL,
    fk_behandling_id bigint NOT NULL,
    mottatt_dato timestamp(3) without time zone NOT NULL,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av character varying,
    endret_tid timestamp(3) without time zone,
    er_digital boolean,
    journalpost_id character varying,
    brevkode character varying
);


--
-- Name: behandling_soknadsinfo_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.behandling_soknadsinfo_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: behandling_steg_tilstand; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.behandling_steg_tilstand (
    id bigint NOT NULL,
    fk_behandling_id bigint NOT NULL,
    behandling_steg character varying NOT NULL,
    behandling_steg_status character varying DEFAULT 'IKKE_UTFØRT'::character varying NOT NULL,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av character varying,
    endret_tid timestamp(3) without time zone
);


--
-- Name: behandling_steg_tilstand_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.behandling_steg_tilstand_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: brevmottaker; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.brevmottaker (
    id bigint NOT NULL,
    fk_behandling_id bigint NOT NULL,
    type character varying(50) NOT NULL,
    navn character varying NOT NULL,
    adresselinje_1 character varying NOT NULL,
    adresselinje_2 character varying,
    postnummer character varying NOT NULL,
    poststed character varying NOT NULL,
    landkode character varying(2) NOT NULL,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av character varying,
    endret_tid timestamp(3) without time zone
);


--
-- Name: brevmottaker_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.brevmottaker_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: data_chunk; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.data_chunk (
    id bigint NOT NULL,
    fk_batch_id bigint NOT NULL,
    transaksjons_id uuid NOT NULL,
    chunk_nr bigint NOT NULL,
    er_sendt boolean NOT NULL,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av character varying,
    endret_tid timestamp(3) without time zone
);


--
-- Name: data_chunk_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.data_chunk_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: endret_utbetaling_andel; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.endret_utbetaling_andel (
    id bigint NOT NULL,
    fk_behandling_id bigint NOT NULL,
    fk_po_person_id bigint,
    fom timestamp(3) without time zone,
    tom timestamp(3) without time zone,
    prosent numeric,
    aarsak character varying,
    begrunnelse text,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av character varying,
    endret_tid timestamp(3) without time zone,
    vedtak_begrunnelse_spesifikasjoner text DEFAULT ''::text,
    avtaletidspunkt_delt_bosted timestamp(3) without time zone,
    soknadstidspunkt timestamp(3) without time zone
);


--
-- Name: endret_utbetaling_andel_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.endret_utbetaling_andel_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: eos_begrunnelse; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.eos_begrunnelse (
    id bigint NOT NULL,
    fk_vedtaksperiode_id bigint,
    begrunnelse character varying NOT NULL
);


--
-- Name: eos_begrunnelse_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.eos_begrunnelse_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: fagsak; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.fagsak (
    id bigint NOT NULL,
    versjon bigint DEFAULT 0,
    opprettet_av character varying(512) DEFAULT 'VL'::character varying,
    opprettet_tid timestamp(3) without time zone DEFAULT LOCALTIMESTAMP,
    endret_av character varying(512),
    endret_tid timestamp(3) without time zone,
    status character varying(50) DEFAULT 'OPPRETTET'::character varying,
    arkivert boolean DEFAULT false NOT NULL,
    fk_aktoer_id character varying,
    type character varying(50) DEFAULT 'NORMAL'::character varying NOT NULL,
    fk_institusjon_id bigint
);


--
-- Name: fagsak_person_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.fagsak_person_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: fagsak_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.fagsak_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: feilutbetalt_valuta; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.feilutbetalt_valuta (
    id bigint NOT NULL,
    fk_behandling_id bigint NOT NULL,
    fom timestamp(3) without time zone NOT NULL,
    tom timestamp(3) without time zone NOT NULL,
    feilutbetalt_beloep numeric,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av character varying,
    endret_tid timestamp(3) without time zone,
    er_per_maaned boolean DEFAULT false NOT NULL
);


--
-- Name: feilutbetalt_valuta_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.feilutbetalt_valuta_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: foedselshendelse_pre_lansering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.foedselshendelse_pre_lansering (
    id bigint NOT NULL,
    fk_behandling_id bigint NOT NULL,
    ny_behandling_hendelse text NOT NULL,
    filtreringsregler_input text,
    filtreringsregler_output text,
    vilkaarsvurderinger_for_foedselshendelse text,
    opprettet_av character varying DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av character varying,
    endret_tid timestamp(3) without time zone,
    versjon bigint DEFAULT 0 NOT NULL,
    fk_aktoer_id character varying
);


--
-- Name: foedselshendelse_pre_lansering_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.foedselshendelse_pre_lansering_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: foedselshendelsefiltrering_resultat; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.foedselshendelsefiltrering_resultat (
    id bigint NOT NULL,
    fk_behandling_id bigint NOT NULL,
    filtreringsregel character varying NOT NULL,
    resultat character varying NOT NULL,
    begrunnelse text NOT NULL,
    evalueringsaarsaker text NOT NULL,
    regel_input text,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av character varying,
    endret_tid timestamp(3) without time zone
);


--
-- Name: foedselshendelsefiltrering_resultat_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.foedselshendelsefiltrering_resultat_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: gr_periode_overgangsstonad; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.gr_periode_overgangsstonad (
    id bigint NOT NULL,
    fk_behandling_id bigint NOT NULL,
    fom timestamp(3) without time zone NOT NULL,
    tom timestamp(3) without time zone NOT NULL,
    datakilde character varying NOT NULL,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av character varying,
    endret_tid timestamp(3) without time zone,
    fk_aktoer_id character varying
);


--
-- Name: gr_periode_overgangsstonad_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.gr_periode_overgangsstonad_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: gr_personopplysninger; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.gr_personopplysninger (
    id bigint NOT NULL,
    fk_behandling_id bigint NOT NULL,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying(512) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av character varying(512),
    endret_tid timestamp(3) without time zone,
    aktiv boolean DEFAULT true NOT NULL
);


--
-- Name: gr_personopplysninger_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.gr_personopplysninger_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: gr_soknad; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.gr_soknad (
    id bigint NOT NULL,
    opprettet_av character varying DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT LOCALTIMESTAMP NOT NULL,
    fk_behandling_id bigint NOT NULL,
    soknad text NOT NULL,
    aktiv boolean DEFAULT true NOT NULL
);


--
-- Name: gr_soknad_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.gr_soknad_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: institusjon; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.institusjon (
    id bigint NOT NULL,
    org_nummer character varying NOT NULL,
    tss_ekstern_id character varying,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone
);


--
-- Name: institusjon_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.institusjon_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: journalpost; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.journalpost (
    id bigint NOT NULL,
    fk_behandling_id bigint NOT NULL,
    journalpost_id character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT LOCALTIMESTAMP NOT NULL,
    opprettet_av character varying DEFAULT 'VL'::character varying NOT NULL,
    type character varying
);


--
-- Name: journalpost_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.journalpost_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: kompetanse; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.kompetanse (
    id bigint NOT NULL,
    fk_behandling_id bigint NOT NULL,
    fom timestamp(3) without time zone,
    tom timestamp(3) without time zone,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av character varying,
    endret_tid timestamp(3) without time zone,
    soekers_aktivitet character varying,
    annen_forelderes_aktivitet character varying,
    annen_forelderes_aktivitetsland character varying,
    barnets_bostedsland character varying,
    resultat character varying,
    sokers_aktivitetsland text
);


--
-- Name: kompetanse_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.kompetanse_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: korrigert_etterbetaling; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.korrigert_etterbetaling (
    id bigint NOT NULL,
    aarsak character varying NOT NULL,
    begrunnelse character varying,
    belop bigint NOT NULL,
    aktiv boolean NOT NULL,
    fk_behandling_id bigint NOT NULL,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av character varying,
    endret_tid timestamp(3) without time zone
);


--
-- Name: korrigert_etterbetaling_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.korrigert_etterbetaling_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: korrigert_vedtak; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.korrigert_vedtak (
    id bigint NOT NULL,
    begrunnelse character varying,
    vedtaksdato timestamp(3) without time zone DEFAULT NULL::timestamp without time zone,
    aktiv boolean NOT NULL,
    fk_behandling_id bigint NOT NULL,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av character varying,
    endret_tid timestamp(3) without time zone
);


--
-- Name: korrigert_vedtak_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.korrigert_vedtak_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: logg; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.logg (
    id bigint NOT NULL,
    opprettet_av character varying DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT LOCALTIMESTAMP NOT NULL,
    fk_behandling_id bigint NOT NULL,
    type character varying NOT NULL,
    tittel character varying NOT NULL,
    rolle character varying NOT NULL,
    tekst text NOT NULL
);


--
-- Name: logg_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.logg_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: okonomi_simulering_mottaker; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.okonomi_simulering_mottaker (
    id bigint NOT NULL,
    mottaker_nummer character varying(50),
    mottaker_type character varying(50),
    opprettet_av character varying(512) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av character varying(512),
    endret_tid timestamp(3) without time zone,
    versjon bigint DEFAULT 0,
    fk_behandling_id bigint
);


--
-- Name: okonomi_simulering_mottaker_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.okonomi_simulering_mottaker_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: okonomi_simulering_postering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.okonomi_simulering_postering (
    id bigint NOT NULL,
    fk_okonomi_simulering_mottaker_id bigint,
    fag_omraade_kode character varying(50),
    fom timestamp(3) without time zone,
    tom timestamp(3) without time zone,
    betaling_type character varying(50),
    belop bigint,
    postering_type character varying(50),
    forfallsdato timestamp(3) without time zone,
    uten_inntrekk boolean,
    opprettet_av character varying(512) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av character varying(512),
    endret_tid timestamp(3) without time zone,
    versjon bigint DEFAULT 0,
    er_feilkonto boolean
);


--
-- Name: okonomi_simulering_postering_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.okonomi_simulering_postering_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: oppgave; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.oppgave (
    id bigint NOT NULL,
    fk_behandling_id bigint NOT NULL,
    gsak_id character varying NOT NULL,
    type character varying NOT NULL,
    ferdigstilt boolean NOT NULL,
    opprettet_tid timestamp without time zone NOT NULL
);


--
-- Name: oppgave_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.oppgave_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: periode_resultat_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.periode_resultat_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: person_resultat; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.person_resultat (
    id bigint NOT NULL,
    fk_vilkaarsvurdering_id bigint NOT NULL,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av character varying,
    endret_tid timestamp(3) without time zone,
    fk_aktoer_id character varying
);


--
-- Name: personident; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.personident (
    fk_aktoer_id character varying NOT NULL,
    foedselsnummer character varying NOT NULL,
    aktiv boolean DEFAULT false NOT NULL,
    gjelder_til timestamp(3) without time zone,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av character varying,
    endret_tid timestamp(3) without time zone
);


--
-- Name: po_arbeidsforhold; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.po_arbeidsforhold (
    id bigint NOT NULL,
    fk_po_person_id bigint NOT NULL,
    arbeidsgiver_id character varying,
    arbeidsgiver_type character varying,
    fom date,
    tom date,
    opprettet_av character varying DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av character varying,
    endret_tid timestamp(3) without time zone,
    versjon bigint DEFAULT 0 NOT NULL
);


--
-- Name: po_arbeidsforhold_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.po_arbeidsforhold_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: po_bostedsadresse; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.po_bostedsadresse (
    id bigint NOT NULL,
    type character varying(20) NOT NULL,
    bostedskommune character varying,
    husnummer character varying,
    husbokstav character varying,
    bruksenhetsnummer character varying,
    adressenavn character varying,
    kommunenummer character varying,
    tilleggsnavn character varying,
    postnummer character varying,
    opprettet_av character varying DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    endret_av character varying,
    versjon bigint DEFAULT 0 NOT NULL,
    endret_tid timestamp(3) without time zone,
    matrikkel_id bigint,
    fom date,
    tom date,
    fk_po_person_id bigint
);


--
-- Name: po_bostedsadresse_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.po_bostedsadresse_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: po_bostedsadresseperiode; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.po_bostedsadresseperiode (
    id bigint NOT NULL,
    fk_po_person_id bigint NOT NULL,
    fom date,
    tom date,
    opprettet_av character varying DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av character varying,
    endret_tid timestamp(3) without time zone,
    versjon bigint DEFAULT 0 NOT NULL
);


--
-- Name: po_bostedsadresseperiode_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.po_bostedsadresseperiode_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: po_doedsfall; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.po_doedsfall (
    id bigint NOT NULL,
    fk_po_person_id bigint NOT NULL,
    versjon bigint DEFAULT 0 NOT NULL,
    doedsfall_dato timestamp(3) without time zone NOT NULL,
    doedsfall_adresse character varying,
    doedsfall_postnummer character varying,
    doedsfall_poststed character varying,
    opprettet_av character varying DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av character varying,
    endret_tid timestamp(3) without time zone
);


--
-- Name: po_doedsfall_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.po_doedsfall_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: po_opphold; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.po_opphold (
    id bigint NOT NULL,
    fk_po_person_id bigint NOT NULL,
    type character varying NOT NULL,
    fom date,
    tom date,
    opprettet_av character varying DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av character varying,
    endret_tid timestamp(3) without time zone,
    versjon bigint DEFAULT 0 NOT NULL
);


--
-- Name: po_opphold_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.po_opphold_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: po_person; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.po_person (
    id bigint NOT NULL,
    fk_gr_personopplysninger_id bigint NOT NULL,
    type character varying(10) NOT NULL,
    opprettet_av character varying(512) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    endret_av character varying(512),
    versjon bigint DEFAULT 0 NOT NULL,
    endret_tid timestamp(3) without time zone,
    foedselsdato timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP,
    fk_aktoer_id character varying(50),
    navn character varying DEFAULT ''::character varying,
    kjoenn character varying DEFAULT 'UKJENT'::character varying,
    maalform character varying(2) DEFAULT 'NB'::character varying NOT NULL
);


--
-- Name: po_person_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.po_person_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: po_sivilstand; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.po_sivilstand (
    id bigint NOT NULL,
    fk_po_person_id bigint NOT NULL,
    fom date,
    type character varying NOT NULL,
    opprettet_av character varying DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av character varying,
    endret_tid timestamp(3) without time zone,
    versjon bigint DEFAULT 0 NOT NULL
);


--
-- Name: po_sivilstand_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.po_sivilstand_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: po_statsborgerskap; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.po_statsborgerskap (
    id bigint NOT NULL,
    fk_po_person_id bigint NOT NULL,
    landkode character varying(3) DEFAULT 'XUK'::character varying NOT NULL,
    fom date,
    tom date,
    opprettet_av character varying DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av character varying,
    endret_tid timestamp(3) without time zone,
    versjon bigint DEFAULT 0 NOT NULL,
    medlemskap character varying DEFAULT 'UKJENT'::character varying NOT NULL
);


--
-- Name: po_statsborgerskap_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.po_statsborgerskap_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: refusjon_eos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.refusjon_eos (
    id bigint NOT NULL,
    fk_behandling_id bigint NOT NULL,
    fom timestamp(3) without time zone NOT NULL,
    tom timestamp(3) without time zone NOT NULL,
    refusjonsbeloep numeric NOT NULL,
    land character varying NOT NULL,
    refusjon_avklart boolean NOT NULL,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av character varying,
    endret_tid timestamp(3) without time zone
);


--
-- Name: refusjon_eos_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.refusjon_eos_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: saksstatistikk_mellomlagring; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.saksstatistikk_mellomlagring (
    id bigint NOT NULL,
    offset_verdi bigint,
    funksjonell_id character varying NOT NULL,
    type character varying NOT NULL,
    kontrakt_versjon character varying NOT NULL,
    json text NOT NULL,
    konvertert_tid timestamp(3) without time zone DEFAULT LOCALTIMESTAMP,
    opprettet_tid timestamp(3) without time zone DEFAULT LOCALTIMESTAMP NOT NULL,
    sendt_tid timestamp(3) without time zone DEFAULT LOCALTIMESTAMP,
    type_id bigint,
    offset_aiven bigint
);


--
-- Name: saksstatistikk_mellomlagring_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.saksstatistikk_mellomlagring_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sats_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.sats_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: satskjoering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.satskjoering (
    id bigint NOT NULL,
    fk_fagsak_id bigint NOT NULL,
    start_tid timestamp(3) without time zone DEFAULT LOCALTIMESTAMP NOT NULL,
    ferdig_tid timestamp(3) without time zone,
    feiltype character varying,
    sats_tid timestamp(3) without time zone DEFAULT to_timestamp('01-03-2023'::text, 'DD-MM-YYYY SS:MS'::text) NOT NULL
);


--
-- Name: satskjoering_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.satskjoering_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sett_paa_vent; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sett_paa_vent (
    id bigint NOT NULL,
    fk_behandling_id bigint NOT NULL,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT LOCALTIMESTAMP NOT NULL,
    frist timestamp(3) without time zone NOT NULL,
    aktiv boolean DEFAULT false NOT NULL,
    aarsak character varying NOT NULL,
    endret_av character varying,
    endret_tid timestamp(3) without time zone,
    tid_tatt_av_vent timestamp(3) without time zone,
    tid_satt_paa_vent timestamp(3) without time zone DEFAULT now() NOT NULL
);


--
-- Name: sett_paa_vent_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.sett_paa_vent_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: skyggesak; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.skyggesak (
    id bigint NOT NULL,
    fk_fagsak_id bigint NOT NULL,
    sendt_tid timestamp(3) without time zone
);


--
-- Name: skyggesak_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.skyggesak_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: task; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.task (
    id bigint NOT NULL,
    payload text NOT NULL,
    status character varying(50) DEFAULT 'UBEHANDLET'::character varying NOT NULL,
    versjon bigint DEFAULT 0,
    opprettet_tid timestamp(3) without time zone DEFAULT LOCALTIMESTAMP,
    type character varying(100) NOT NULL,
    metadata character varying(4000),
    trigger_tid timestamp without time zone DEFAULT LOCALTIMESTAMP,
    avvikstype character varying(50)
);


--
-- Name: task_logg; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.task_logg (
    id bigint NOT NULL,
    task_id bigint NOT NULL,
    type character varying(50) NOT NULL,
    node character varying(100) NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT LOCALTIMESTAMP,
    melding text,
    endret_av character varying(100) DEFAULT 'VL'::character varying
);


--
-- Name: task_logg_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.task_logg_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: task_logg_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.task_logg_seq OWNED BY public.task_logg.id;


--
-- Name: task_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.task_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: task_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.task_seq OWNED BY public.task.id;


--
-- Name: tilbakekreving; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tilbakekreving (
    id bigint NOT NULL,
    valg character varying NOT NULL,
    varsel text,
    begrunnelse text NOT NULL,
    tilbakekrevingsbehandling_id text,
    opprettet_av character varying DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp without time zone DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av character varying,
    endret_tid timestamp(3) without time zone,
    versjon bigint DEFAULT 0,
    fk_behandling_id bigint
);


--
-- Name: tilbakekreving_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.tilbakekreving_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: tilkjent_ytelse; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tilkjent_ytelse (
    id bigint NOT NULL,
    fk_behandling_id bigint,
    stonad_fom timestamp without time zone,
    stonad_tom timestamp without time zone,
    opprettet_dato timestamp without time zone NOT NULL,
    opphor_fom timestamp without time zone,
    utbetalingsoppdrag text,
    endret_dato timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: tilkjent_ytelse_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.tilkjent_ytelse_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: totrinnskontroll; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.totrinnskontroll (
    id bigint NOT NULL,
    fk_behandling_id bigint NOT NULL,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av character varying,
    endret_tid timestamp(3) without time zone,
    aktiv boolean DEFAULT true NOT NULL,
    saksbehandler character varying NOT NULL,
    beslutter character varying,
    godkjent boolean DEFAULT true,
    saksbehandler_id character varying DEFAULT 'ukjent'::character varying NOT NULL,
    beslutter_id character varying,
    kontrollerte_sider text DEFAULT ''::text
);


--
-- Name: totrinnskontroll_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.totrinnskontroll_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: utenlandsk_periodebeloep; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.utenlandsk_periodebeloep (
    id bigint NOT NULL,
    fk_behandling_id bigint NOT NULL,
    fom timestamp(3) without time zone,
    tom timestamp(3) without time zone,
    intervall character varying,
    valutakode character varying,
    beloep numeric,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av character varying,
    endret_tid timestamp(3) without time zone,
    utbetalingsland character varying,
    kalkulert_maanedlig_beloep numeric
);


--
-- Name: utenlandsk_periodebeloep_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.utenlandsk_periodebeloep_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: valutakurs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.valutakurs (
    id bigint NOT NULL,
    fk_behandling_id bigint NOT NULL,
    fom timestamp(3) without time zone,
    tom timestamp(3) without time zone,
    valutakursdato timestamp(3) without time zone DEFAULT NULL::timestamp without time zone,
    valutakode character varying,
    kurs numeric,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av character varying,
    endret_tid timestamp(3) without time zone
);


--
-- Name: valutakurs_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.valutakurs_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: vedtak; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.vedtak (
    id bigint NOT NULL,
    fk_behandling_id bigint NOT NULL,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying(512) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT LOCALTIMESTAMP NOT NULL,
    vedtaksdato timestamp(3) without time zone DEFAULT LOCALTIMESTAMP,
    endret_av character varying(512),
    endret_tid timestamp(3) without time zone,
    aktiv boolean DEFAULT true,
    stonad_brev_pdf bytea
);


--
-- Name: vedtak_begrunnelse_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.vedtak_begrunnelse_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: vedtak_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.vedtak_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: vedtaksbegrunnelse; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.vedtaksbegrunnelse (
    id bigint NOT NULL,
    fk_vedtaksperiode_id bigint,
    vedtak_begrunnelse_spesifikasjon character varying NOT NULL
);


--
-- Name: vedtaksbegrunnelse_fritekst; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.vedtaksbegrunnelse_fritekst (
    id bigint NOT NULL,
    fk_vedtaksperiode_id bigint,
    fritekst text DEFAULT ''::text NOT NULL
);


--
-- Name: vedtaksbegrunnelse_fritekst_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.vedtaksbegrunnelse_fritekst_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: vedtaksbegrunnelse_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.vedtaksbegrunnelse_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: vedtaksperiode; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.vedtaksperiode (
    id bigint NOT NULL,
    fk_vedtak_id bigint,
    fom timestamp without time zone,
    tom timestamp without time zone,
    type character varying NOT NULL,
    opprettet_av character varying DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp without time zone DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av character varying,
    endret_tid timestamp(3) without time zone,
    versjon bigint DEFAULT 0
);


--
-- Name: vedtaksperiode_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.vedtaksperiode_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: verge; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.verge (
    id bigint NOT NULL,
    ident character varying,
    fk_behandling_id bigint NOT NULL,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone
);


--
-- Name: verge_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.verge_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: vilkaarsvurdering; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.vilkaarsvurdering (
    id bigint NOT NULL,
    fk_behandling_id bigint NOT NULL,
    aktiv boolean DEFAULT true NOT NULL,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av character varying,
    endret_tid timestamp(3) without time zone,
    samlet_resultat character varying,
    ytelse_personer text DEFAULT ''::text
);


--
-- Name: vilkaarsvurdering_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.vilkaarsvurdering_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: vilkar_resultat; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.vilkar_resultat (
    id bigint NOT NULL,
    vilkar character varying(50) NOT NULL,
    resultat character varying(50) NOT NULL,
    regel_input text,
    regel_output text,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying(512) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av character varying(512),
    endret_tid timestamp(3) without time zone,
    fk_person_resultat_id bigint,
    begrunnelse text,
    periode_fom timestamp(3) without time zone DEFAULT NULL::timestamp without time zone,
    periode_tom timestamp(3) without time zone DEFAULT NULL::timestamp without time zone,
    fk_behandling_id bigint NOT NULL,
    evaluering_aarsak text DEFAULT ''::text,
    er_automatisk_vurdert boolean DEFAULT false NOT NULL,
    er_eksplisitt_avslag_paa_soknad boolean,
    vedtak_begrunnelse_spesifikasjoner text DEFAULT ''::text,
    vurderes_etter character varying,
    utdypende_vilkarsvurderinger character varying,
    resultat_begrunnelse character varying
);


--
-- Name: vilkar_resultat_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.vilkar_resultat_seq
    START WITH 1000000
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: task id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.task ALTER COLUMN id SET DEFAULT nextval('public.task_seq'::regclass);


--
-- Name: task_logg id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.task_logg ALTER COLUMN id SET DEFAULT nextval('public.task_logg_seq'::regclass);


--
-- Data for Name: aktoer; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.aktoer (aktoer_id, versjon, opprettet_av, opprettet_tid, endret_av, endret_tid) FROM stdin;
\.


--
-- Data for Name: aktoer_til_kompetanse; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.aktoer_til_kompetanse (fk_kompetanse_id, fk_aktoer_id) FROM stdin;
\.


--
-- Data for Name: aktoer_til_utenlandsk_periodebeloep; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.aktoer_til_utenlandsk_periodebeloep (fk_utenlandsk_periodebeloep_id, fk_aktoer_id) FROM stdin;
\.


--
-- Data for Name: aktoer_til_valutakurs; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.aktoer_til_valutakurs (fk_valutakurs_id, fk_aktoer_id) FROM stdin;
\.


--
-- Data for Name: andel_tilkjent_ytelse; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.andel_tilkjent_ytelse (id, fk_behandling_id, versjon, opprettet_av, opprettet_tid, stonad_fom, stonad_tom, type, kalkulert_utbetalingsbelop, endret_av, endret_tid, tilkjent_ytelse_id, periode_offset, forrige_periode_offset, kilde_behandling_id, prosent, sats, fk_aktoer_id, nasjonalt_periodebelop, differanseberegnet_periodebelop) FROM stdin;
\.


--
-- Data for Name: annen_vurdering; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.annen_vurdering (id, fk_person_resultat_id, resultat, type, begrunnelse, versjon, opprettet_av, opprettet_tid, endret_av, endret_tid) FROM stdin;
\.


--
-- Data for Name: arbeidsfordeling_pa_behandling; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.arbeidsfordeling_pa_behandling (id, fk_behandling_id, behandlende_enhet_id, behandlende_enhet_navn, manuelt_overstyrt) FROM stdin;
\.


--
-- Data for Name: batch; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.batch (id, kjoredato, status) FROM stdin;
1000000	2021-01-06 00:00:00	LEDIG
1000050	2021-01-29 00:00:00	LEDIG
1000100	2021-02-26 00:00:00	LEDIG
1000150	2021-03-31 00:00:00	LEDIG
1000200	2021-04-26 00:00:00	LEDIG
1000250	2021-05-28 00:00:00	LEDIG
1000300	2021-06-29 00:00:00	LEDIG
1000350	2021-07-30 00:00:00	LEDIG
1000400	2021-08-30 00:00:00	LEDIG
1000450	2021-09-27 00:00:00	LEDIG
1000500	2021-10-29 00:00:00	LEDIG
1000550	2021-11-22 00:00:00	LEDIG
1000600	2022-01-05 00:00:00	LEDIG
1000650	2022-01-28 00:00:00	LEDIG
1000700	2022-02-25 00:00:00	LEDIG
1000750	2022-03-25 00:00:00	LEDIG
1000800	2022-04-26 00:00:00	LEDIG
1000850	2022-05-27 00:00:00	LEDIG
1000900	2022-06-29 00:00:00	LEDIG
1000950	2022-07-29 00:00:00	LEDIG
1001000	2022-08-30 00:00:00	LEDIG
1001050	2022-09-29 00:00:00	LEDIG
1001100	2022-10-28 00:00:00	LEDIG
1001150	2022-11-21 00:00:00	LEDIG
1001200	2023-01-05 00:00:00	LEDIG
1001250	2023-01-30 00:00:00	LEDIG
1001300	2023-02-27 00:00:00	LEDIG
1001350	2023-03-28 00:00:00	LEDIG
1001400	2023-04-25 00:00:00	LEDIG
1001450	2023-05-30 00:00:00	LEDIG
1001500	2023-06-29 00:00:00	LEDIG
1001550	2023-07-28 00:00:00	LEDIG
1001600	2023-08-30 00:00:00	LEDIG
1001650	2023-09-29 00:00:00	LEDIG
1001700	2023-10-30 00:00:00	LEDIG
1001750	2023-11-22 00:00:00	LEDIG
\.


--
-- Data for Name: behandling; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.behandling (id, fk_fagsak_id, versjon, opprettet_av, opprettet_tid, endret_av, endret_tid, behandling_type, aktiv, status, kategori, underkategori, opprettet_aarsak, skal_behandles_automatisk, resultat, overstyrt_endringstidspunkt, aktivert_tid) FROM stdin;
\.


--
-- Data for Name: behandling_migreringsinfo; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.behandling_migreringsinfo (id, fk_behandling_id, migreringsdato, versjon, opprettet_av, opprettet_tid, endret_av, endret_tid) FROM stdin;
\.


--
-- Data for Name: behandling_soknadsinfo; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.behandling_soknadsinfo (id, fk_behandling_id, mottatt_dato, versjon, opprettet_av, opprettet_tid, endret_av, endret_tid, er_digital, journalpost_id, brevkode) FROM stdin;
\.


--
-- Data for Name: behandling_steg_tilstand; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.behandling_steg_tilstand (id, fk_behandling_id, behandling_steg, behandling_steg_status, versjon, opprettet_av, opprettet_tid, endret_av, endret_tid) FROM stdin;
\.


--
-- Data for Name: brevmottaker; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.brevmottaker (id, fk_behandling_id, type, navn, adresselinje_1, adresselinje_2, postnummer, poststed, landkode, versjon, opprettet_av, opprettet_tid, endret_av, endret_tid) FROM stdin;
\.


--
-- Data for Name: data_chunk; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.data_chunk (id, fk_batch_id, transaksjons_id, chunk_nr, er_sendt, versjon, opprettet_av, opprettet_tid, endret_av, endret_tid) FROM stdin;
\.


--
-- Data for Name: endret_utbetaling_andel; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.endret_utbetaling_andel (id, fk_behandling_id, fk_po_person_id, fom, tom, prosent, aarsak, begrunnelse, versjon, opprettet_av, opprettet_tid, endret_av, endret_tid, vedtak_begrunnelse_spesifikasjoner, avtaletidspunkt_delt_bosted, soknadstidspunkt) FROM stdin;
\.


--
-- Data for Name: eos_begrunnelse; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.eos_begrunnelse (id, fk_vedtaksperiode_id, begrunnelse) FROM stdin;
\.


--
-- Data for Name: fagsak; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.fagsak (id, versjon, opprettet_av, opprettet_tid, endret_av, endret_tid, status, arkivert, fk_aktoer_id, type, fk_institusjon_id) FROM stdin;
\.


--
-- Data for Name: feilutbetalt_valuta; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.feilutbetalt_valuta (id, fk_behandling_id, fom, tom, feilutbetalt_beloep, versjon, opprettet_av, opprettet_tid, endret_av, endret_tid, er_per_maaned) FROM stdin;
\.


--
-- Data for Name: foedselshendelse_pre_lansering; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.foedselshendelse_pre_lansering (id, fk_behandling_id, ny_behandling_hendelse, filtreringsregler_input, filtreringsregler_output, vilkaarsvurderinger_for_foedselshendelse, opprettet_av, opprettet_tid, endret_av, endret_tid, versjon, fk_aktoer_id) FROM stdin;
\.


--
-- Data for Name: foedselshendelsefiltrering_resultat; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.foedselshendelsefiltrering_resultat (id, fk_behandling_id, filtreringsregel, resultat, begrunnelse, evalueringsaarsaker, regel_input, versjon, opprettet_av, opprettet_tid, endret_av, endret_tid) FROM stdin;
\.


--
-- Data for Name: gr_periode_overgangsstonad; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.gr_periode_overgangsstonad (id, fk_behandling_id, fom, tom, datakilde, versjon, opprettet_av, opprettet_tid, endret_av, endret_tid, fk_aktoer_id) FROM stdin;
\.


--
-- Data for Name: gr_personopplysninger; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.gr_personopplysninger (id, fk_behandling_id, versjon, opprettet_av, opprettet_tid, endret_av, endret_tid, aktiv) FROM stdin;
\.


--
-- Data for Name: gr_soknad; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.gr_soknad (id, opprettet_av, opprettet_tid, fk_behandling_id, soknad, aktiv) FROM stdin;
\.


--
-- Data for Name: institusjon; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.institusjon (id, org_nummer, tss_ekstern_id, versjon, opprettet_av, opprettet_tid, endret_av, endret_tid) FROM stdin;
\.


--
-- Data for Name: journalpost; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.journalpost (id, fk_behandling_id, journalpost_id, opprettet_tid, opprettet_av, type) FROM stdin;
\.


--
-- Data for Name: kompetanse; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.kompetanse (id, fk_behandling_id, fom, tom, versjon, opprettet_av, opprettet_tid, endret_av, endret_tid, soekers_aktivitet, annen_forelderes_aktivitet, annen_forelderes_aktivitetsland, barnets_bostedsland, resultat, sokers_aktivitetsland) FROM stdin;
\.


--
-- Data for Name: korrigert_etterbetaling; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.korrigert_etterbetaling (id, aarsak, begrunnelse, belop, aktiv, fk_behandling_id, versjon, opprettet_av, opprettet_tid, endret_av, endret_tid) FROM stdin;
\.


--
-- Data for Name: korrigert_vedtak; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.korrigert_vedtak (id, begrunnelse, vedtaksdato, aktiv, fk_behandling_id, versjon, opprettet_av, opprettet_tid, endret_av, endret_tid) FROM stdin;
\.


--
-- Data for Name: logg; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.logg (id, opprettet_av, opprettet_tid, fk_behandling_id, type, tittel, rolle, tekst) FROM stdin;
\.


--
-- Data for Name: okonomi_simulering_mottaker; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.okonomi_simulering_mottaker (id, mottaker_nummer, mottaker_type, opprettet_av, opprettet_tid, endret_av, endret_tid, versjon, fk_behandling_id) FROM stdin;
\.


--
-- Data for Name: okonomi_simulering_postering; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.okonomi_simulering_postering (id, fk_okonomi_simulering_mottaker_id, fag_omraade_kode, fom, tom, betaling_type, belop, postering_type, forfallsdato, uten_inntrekk, opprettet_av, opprettet_tid, endret_av, endret_tid, versjon, er_feilkonto) FROM stdin;
\.


--
-- Data for Name: oppgave; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.oppgave (id, fk_behandling_id, gsak_id, type, ferdigstilt, opprettet_tid) FROM stdin;
\.


--
-- Data for Name: person_resultat; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.person_resultat (id, fk_vilkaarsvurdering_id, versjon, opprettet_av, opprettet_tid, endret_av, endret_tid, fk_aktoer_id) FROM stdin;
\.


--
-- Data for Name: personident; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.personident (fk_aktoer_id, foedselsnummer, aktiv, gjelder_til, versjon, opprettet_av, opprettet_tid, endret_av, endret_tid) FROM stdin;
\.


--
-- Data for Name: po_arbeidsforhold; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.po_arbeidsforhold (id, fk_po_person_id, arbeidsgiver_id, arbeidsgiver_type, fom, tom, opprettet_av, opprettet_tid, endret_av, endret_tid, versjon) FROM stdin;
\.


--
-- Data for Name: po_bostedsadresse; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.po_bostedsadresse (id, type, bostedskommune, husnummer, husbokstav, bruksenhetsnummer, adressenavn, kommunenummer, tilleggsnavn, postnummer, opprettet_av, opprettet_tid, endret_av, versjon, endret_tid, matrikkel_id, fom, tom, fk_po_person_id) FROM stdin;
\.


--
-- Data for Name: po_bostedsadresseperiode; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.po_bostedsadresseperiode (id, fk_po_person_id, fom, tom, opprettet_av, opprettet_tid, endret_av, endret_tid, versjon) FROM stdin;
\.


--
-- Data for Name: po_doedsfall; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.po_doedsfall (id, fk_po_person_id, versjon, doedsfall_dato, doedsfall_adresse, doedsfall_postnummer, doedsfall_poststed, opprettet_av, opprettet_tid, endret_av, endret_tid) FROM stdin;
\.


--
-- Data for Name: po_opphold; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.po_opphold (id, fk_po_person_id, type, fom, tom, opprettet_av, opprettet_tid, endret_av, endret_tid, versjon) FROM stdin;
\.


--
-- Data for Name: po_person; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.po_person (id, fk_gr_personopplysninger_id, type, opprettet_av, opprettet_tid, endret_av, versjon, endret_tid, foedselsdato, fk_aktoer_id, navn, kjoenn, maalform) FROM stdin;
\.


--
-- Data for Name: po_sivilstand; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.po_sivilstand (id, fk_po_person_id, fom, type, opprettet_av, opprettet_tid, endret_av, endret_tid, versjon) FROM stdin;
\.


--
-- Data for Name: po_statsborgerskap; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.po_statsborgerskap (id, fk_po_person_id, landkode, fom, tom, opprettet_av, opprettet_tid, endret_av, endret_tid, versjon, medlemskap) FROM stdin;
\.


--
-- Data for Name: refusjon_eos; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.refusjon_eos (id, fk_behandling_id, fom, tom, refusjonsbeloep, land, refusjon_avklart, versjon, opprettet_av, opprettet_tid, endret_av, endret_tid) FROM stdin;
\.


--
-- Data for Name: saksstatistikk_mellomlagring; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.saksstatistikk_mellomlagring (id, offset_verdi, funksjonell_id, type, kontrakt_versjon, json, konvertert_tid, opprettet_tid, sendt_tid, type_id, offset_aiven) FROM stdin;
\.


--
-- Data for Name: satskjoering; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.satskjoering (id, fk_fagsak_id, start_tid, ferdig_tid, feiltype, sats_tid) FROM stdin;
\.


--
-- Data for Name: sett_paa_vent; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.sett_paa_vent (id, fk_behandling_id, versjon, opprettet_av, opprettet_tid, frist, aktiv, aarsak, endret_av, endret_tid, tid_tatt_av_vent, tid_satt_paa_vent) FROM stdin;
\.


--
-- Data for Name: skyggesak; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.skyggesak (id, fk_fagsak_id, sendt_tid) FROM stdin;
\.


--
-- Data for Name: task; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.task (id, payload, status, versjon, opprettet_tid, type, metadata, trigger_tid, avvikstype) FROM stdin;
\.


--
-- Data for Name: task_logg; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.task_logg (id, task_id, type, node, opprettet_tid, melding, endret_av) FROM stdin;
\.


--
-- Data for Name: tilbakekreving; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.tilbakekreving (id, valg, varsel, begrunnelse, tilbakekrevingsbehandling_id, opprettet_av, opprettet_tid, endret_av, endret_tid, versjon, fk_behandling_id) FROM stdin;
\.


--
-- Data for Name: tilkjent_ytelse; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.tilkjent_ytelse (id, fk_behandling_id, stonad_fom, stonad_tom, opprettet_dato, opphor_fom, utbetalingsoppdrag, endret_dato) FROM stdin;
\.


--
-- Data for Name: totrinnskontroll; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.totrinnskontroll (id, fk_behandling_id, versjon, opprettet_av, opprettet_tid, endret_av, endret_tid, aktiv, saksbehandler, beslutter, godkjent, saksbehandler_id, beslutter_id, kontrollerte_sider) FROM stdin;
\.


--
-- Data for Name: utenlandsk_periodebeloep; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.utenlandsk_periodebeloep (id, fk_behandling_id, fom, tom, intervall, valutakode, beloep, versjon, opprettet_av, opprettet_tid, endret_av, endret_tid, utbetalingsland, kalkulert_maanedlig_beloep) FROM stdin;
\.


--
-- Data for Name: valutakurs; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.valutakurs (id, fk_behandling_id, fom, tom, valutakursdato, valutakode, kurs, versjon, opprettet_av, opprettet_tid, endret_av, endret_tid) FROM stdin;
\.


--
-- Data for Name: vedtak; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.vedtak (id, fk_behandling_id, versjon, opprettet_av, opprettet_tid, vedtaksdato, endret_av, endret_tid, aktiv, stonad_brev_pdf) FROM stdin;
\.


--
-- Data for Name: vedtaksbegrunnelse; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.vedtaksbegrunnelse (id, fk_vedtaksperiode_id, vedtak_begrunnelse_spesifikasjon) FROM stdin;
\.


--
-- Data for Name: vedtaksbegrunnelse_fritekst; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.vedtaksbegrunnelse_fritekst (id, fk_vedtaksperiode_id, fritekst) FROM stdin;
\.


--
-- Data for Name: vedtaksperiode; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.vedtaksperiode (id, fk_vedtak_id, fom, tom, type, opprettet_av, opprettet_tid, endret_av, endret_tid, versjon) FROM stdin;
\.


--
-- Data for Name: verge; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.verge (id, ident, fk_behandling_id, versjon, opprettet_av, opprettet_tid, endret_av, endret_tid) FROM stdin;
\.


--
-- Data for Name: vilkaarsvurdering; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.vilkaarsvurdering (id, fk_behandling_id, aktiv, versjon, opprettet_av, opprettet_tid, endret_av, endret_tid, samlet_resultat, ytelse_personer) FROM stdin;
\.


--
-- Data for Name: vilkar_resultat; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.vilkar_resultat (id, vilkar, resultat, regel_input, regel_output, versjon, opprettet_av, opprettet_tid, endret_av, endret_tid, fk_person_resultat_id, begrunnelse, periode_fom, periode_tom, fk_behandling_id, evaluering_aarsak, er_automatisk_vurdert, er_eksplisitt_avslag_paa_soknad, vedtak_begrunnelse_spesifikasjoner, vurderes_etter, utdypende_vilkarsvurderinger, resultat_begrunnelse) FROM stdin;
\.


--
-- Name: andel_tilkjent_ytelse_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.andel_tilkjent_ytelse_seq', 2000000, true);


--
-- Name: annen_vurdering_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.annen_vurdering_seq', 1000000, false);


--
-- Name: arbeidsfordeling_pa_behandling_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.arbeidsfordeling_pa_behandling_seq', 1000000, false);


--
-- Name: batch_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.batch_seq', 1001750, true);


--
-- Name: behandling_migreringsinfo_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.behandling_migreringsinfo_seq', 1000000, false);


--
-- Name: behandling_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.behandling_seq', 1000000, false);


--
-- Name: behandling_soknadsinfo_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.behandling_soknadsinfo_seq', 1000000, false);


--
-- Name: behandling_steg_tilstand_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.behandling_steg_tilstand_seq', 1000000, false);


--
-- Name: brevmottaker_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.brevmottaker_seq', 1000000, false);


--
-- Name: data_chunk_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.data_chunk_seq', 1000000, false);


--
-- Name: endret_utbetaling_andel_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.endret_utbetaling_andel_seq', 1000000, false);


--
-- Name: eos_begrunnelse_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.eos_begrunnelse_seq', 1000000, false);


--
-- Name: fagsak_person_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.fagsak_person_seq', 1000000, false);


--
-- Name: fagsak_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.fagsak_seq', 1000000, false);


--
-- Name: feilutbetalt_valuta_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.feilutbetalt_valuta_seq', 1000000, false);


--
-- Name: foedselshendelse_pre_lansering_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.foedselshendelse_pre_lansering_seq', 1000000, false);


--
-- Name: foedselshendelsefiltrering_resultat_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.foedselshendelsefiltrering_resultat_seq', 1000000, false);


--
-- Name: gr_periode_overgangsstonad_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.gr_periode_overgangsstonad_seq', 1000000, false);


--
-- Name: gr_personopplysninger_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.gr_personopplysninger_seq', 1000000, false);


--
-- Name: gr_soknad_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.gr_soknad_seq', 1000000, false);


--
-- Name: institusjon_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.institusjon_seq', 1000000, false);


--
-- Name: journalpost_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.journalpost_seq', 1000000, false);


--
-- Name: kompetanse_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.kompetanse_seq', 1000000, false);


--
-- Name: korrigert_etterbetaling_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.korrigert_etterbetaling_seq', 1000000, false);


--
-- Name: korrigert_vedtak_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.korrigert_vedtak_seq', 1000000, false);


--
-- Name: logg_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.logg_seq', 1000000, false);


--
-- Name: okonomi_simulering_mottaker_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.okonomi_simulering_mottaker_seq', 1000000, false);


--
-- Name: okonomi_simulering_postering_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.okonomi_simulering_postering_seq', 1000000, false);


--
-- Name: oppgave_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.oppgave_seq', 1000000, false);


--
-- Name: periode_resultat_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.periode_resultat_seq', 1000000, false);


--
-- Name: po_arbeidsforhold_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.po_arbeidsforhold_seq', 1000000, false);


--
-- Name: po_bostedsadresse_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.po_bostedsadresse_seq', 1000000, false);


--
-- Name: po_bostedsadresseperiode_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.po_bostedsadresseperiode_seq', 1000000, false);


--
-- Name: po_doedsfall_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.po_doedsfall_seq', 1000000, false);


--
-- Name: po_opphold_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.po_opphold_seq', 1000000, false);


--
-- Name: po_person_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.po_person_seq', 1000000, false);


--
-- Name: po_sivilstand_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.po_sivilstand_seq', 1000000, false);


--
-- Name: po_statsborgerskap_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.po_statsborgerskap_seq', 1000000, false);


--
-- Name: refusjon_eos_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.refusjon_eos_seq', 1000000, false);


--
-- Name: saksstatistikk_mellomlagring_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.saksstatistikk_mellomlagring_seq', 1000000, false);


--
-- Name: sats_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.sats_seq', 1000000, false);


--
-- Name: satskjoering_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.satskjoering_seq', 1000000, false);


--
-- Name: sett_paa_vent_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.sett_paa_vent_seq', 1000000, false);


--
-- Name: skyggesak_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.skyggesak_seq', 1000000, false);


--
-- Name: task_logg_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.task_logg_seq', 1, false);


--
-- Name: task_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.task_seq', 51, true);


--
-- Name: tilbakekreving_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.tilbakekreving_seq', 1000000, false);


--
-- Name: tilkjent_ytelse_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.tilkjent_ytelse_seq', 1000000, false);


--
-- Name: totrinnskontroll_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.totrinnskontroll_seq', 1000000, false);


--
-- Name: utenlandsk_periodebeloep_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.utenlandsk_periodebeloep_seq', 1000000, false);


--
-- Name: valutakurs_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.valutakurs_seq', 1000000, false);


--
-- Name: vedtak_begrunnelse_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.vedtak_begrunnelse_seq', 1000000, false);


--
-- Name: vedtak_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.vedtak_seq', 1000000, false);


--
-- Name: vedtaksbegrunnelse_fritekst_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.vedtaksbegrunnelse_fritekst_seq', 1000000, false);


--
-- Name: vedtaksbegrunnelse_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.vedtaksbegrunnelse_seq', 1000000, false);


--
-- Name: vedtaksperiode_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.vedtaksperiode_seq', 1000000, false);


--
-- Name: verge_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.verge_seq', 1000000, false);


--
-- Name: vilkaarsvurdering_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.vilkaarsvurdering_seq', 1000000, false);


--
-- Name: vilkar_resultat_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.vilkar_resultat_seq', 1000000, false);


--
-- Name: aktoer aktoer_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.aktoer
    ADD CONSTRAINT aktoer_pkey PRIMARY KEY (aktoer_id);


--
-- Name: aktoer_til_kompetanse aktoer_til_kompetanse_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.aktoer_til_kompetanse
    ADD CONSTRAINT aktoer_til_kompetanse_pkey PRIMARY KEY (fk_kompetanse_id, fk_aktoer_id);


--
-- Name: aktoer_til_utenlandsk_periodebeloep aktoer_til_utenlandsk_periodebeloep_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.aktoer_til_utenlandsk_periodebeloep
    ADD CONSTRAINT aktoer_til_utenlandsk_periodebeloep_pkey PRIMARY KEY (fk_utenlandsk_periodebeloep_id, fk_aktoer_id);


--
-- Name: aktoer_til_valutakurs aktoer_til_valutakurs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.aktoer_til_valutakurs
    ADD CONSTRAINT aktoer_til_valutakurs_pkey PRIMARY KEY (fk_valutakurs_id, fk_aktoer_id);


--
-- Name: andel_tilkjent_ytelse andel_tilkjent_ytelse_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.andel_tilkjent_ytelse
    ADD CONSTRAINT andel_tilkjent_ytelse_pkey PRIMARY KEY (id);


--
-- Name: annen_vurdering annen_vurdering_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.annen_vurdering
    ADD CONSTRAINT annen_vurdering_pkey PRIMARY KEY (id);


--
-- Name: arbeidsfordeling_pa_behandling arbeidsfordeling_pa_behandling_fk_behandling_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arbeidsfordeling_pa_behandling
    ADD CONSTRAINT arbeidsfordeling_pa_behandling_fk_behandling_id_key UNIQUE (fk_behandling_id);


--
-- Name: arbeidsfordeling_pa_behandling arbeidsfordeling_pa_behandling_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arbeidsfordeling_pa_behandling
    ADD CONSTRAINT arbeidsfordeling_pa_behandling_pkey PRIMARY KEY (id);


--
-- Name: batch batch_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.batch
    ADD CONSTRAINT batch_pkey PRIMARY KEY (id);


--
-- Name: behandling_migreringsinfo behandling_migreringsinfo_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.behandling_migreringsinfo
    ADD CONSTRAINT behandling_migreringsinfo_pkey PRIMARY KEY (id);


--
-- Name: behandling behandling_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.behandling
    ADD CONSTRAINT behandling_pkey PRIMARY KEY (id);


--
-- Name: vilkaarsvurdering behandling_resultat_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vilkaarsvurdering
    ADD CONSTRAINT behandling_resultat_pkey PRIMARY KEY (id);


--
-- Name: behandling_soknadsinfo behandling_soknadsinfo_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.behandling_soknadsinfo
    ADD CONSTRAINT behandling_soknadsinfo_pkey PRIMARY KEY (id);


--
-- Name: behandling_steg_tilstand behandling_steg_tilstand_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.behandling_steg_tilstand
    ADD CONSTRAINT behandling_steg_tilstand_pkey PRIMARY KEY (id);


--
-- Name: vedtak behandling_vedtak_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vedtak
    ADD CONSTRAINT behandling_vedtak_pkey PRIMARY KEY (id);


--
-- Name: tilkjent_ytelse beregning_resultat_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tilkjent_ytelse
    ADD CONSTRAINT beregning_resultat_pkey PRIMARY KEY (id);


--
-- Name: brevmottaker brevmottaker_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brevmottaker
    ADD CONSTRAINT brevmottaker_pkey PRIMARY KEY (id);


--
-- Name: data_chunk data_chunk_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.data_chunk
    ADD CONSTRAINT data_chunk_pkey PRIMARY KEY (id);


--
-- Name: endret_utbetaling_andel endret_utbetaling_andel_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.endret_utbetaling_andel
    ADD CONSTRAINT endret_utbetaling_andel_pkey PRIMARY KEY (id);


--
-- Name: eos_begrunnelse eos_begrunnelse_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.eos_begrunnelse
    ADD CONSTRAINT eos_begrunnelse_pkey PRIMARY KEY (id);


--
-- Name: fagsak fagsak_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fagsak
    ADD CONSTRAINT fagsak_pkey PRIMARY KEY (id);


--
-- Name: foedselshendelse_pre_lansering foedselshendelse_pre_lansering_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.foedselshendelse_pre_lansering
    ADD CONSTRAINT foedselshendelse_pre_lansering_pkey PRIMARY KEY (id);


--
-- Name: foedselshendelsefiltrering_resultat foedselshendelsefiltrering_resultat_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.foedselshendelsefiltrering_resultat
    ADD CONSTRAINT foedselshendelsefiltrering_resultat_pkey PRIMARY KEY (id);


--
-- Name: gr_periode_overgangsstonad gr_periode_overgangsstonad_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.gr_periode_overgangsstonad
    ADD CONSTRAINT gr_periode_overgangsstonad_pkey PRIMARY KEY (id);


--
-- Name: gr_personopplysninger gr_personopplysninger_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.gr_personopplysninger
    ADD CONSTRAINT gr_personopplysninger_pkey PRIMARY KEY (id);


--
-- Name: gr_soknad gr_soknad_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.gr_soknad
    ADD CONSTRAINT gr_soknad_pkey PRIMARY KEY (id);


--
-- Name: task_logg henvendelse_logg_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.task_logg
    ADD CONSTRAINT henvendelse_logg_pkey PRIMARY KEY (id);


--
-- Name: task henvendelse_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.task
    ADD CONSTRAINT henvendelse_pkey PRIMARY KEY (id);


--
-- Name: institusjon institusjon_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.institusjon
    ADD CONSTRAINT institusjon_pkey PRIMARY KEY (id);


--
-- Name: journalpost journalpost_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.journalpost
    ADD CONSTRAINT journalpost_pkey PRIMARY KEY (id);


--
-- Name: kompetanse kompetanse_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.kompetanse
    ADD CONSTRAINT kompetanse_pkey PRIMARY KEY (id);


--
-- Name: korrigert_etterbetaling korrigert_etterbetaling_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.korrigert_etterbetaling
    ADD CONSTRAINT korrigert_etterbetaling_pkey PRIMARY KEY (id);


--
-- Name: korrigert_vedtak korrigert_vedtak_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.korrigert_vedtak
    ADD CONSTRAINT korrigert_vedtak_pkey PRIMARY KEY (id);


--
-- Name: logg logg_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.logg
    ADD CONSTRAINT logg_pkey PRIMARY KEY (id);


--
-- Name: oppgave oppgave_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oppgave
    ADD CONSTRAINT oppgave_pkey PRIMARY KEY (id);


--
-- Name: person_resultat periode_resultat_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.person_resultat
    ADD CONSTRAINT periode_resultat_pkey PRIMARY KEY (id);


--
-- Name: personident personident_foedselsnummer_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.personident
    ADD CONSTRAINT personident_foedselsnummer_key UNIQUE (foedselsnummer);


--
-- Name: personident personident_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.personident
    ADD CONSTRAINT personident_pkey PRIMARY KEY (foedselsnummer);


--
-- Name: po_arbeidsforhold po_arbeidsforhold_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.po_arbeidsforhold
    ADD CONSTRAINT po_arbeidsforhold_pkey PRIMARY KEY (id);


--
-- Name: po_bostedsadresse po_bostedsadresse_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.po_bostedsadresse
    ADD CONSTRAINT po_bostedsadresse_pkey PRIMARY KEY (id);


--
-- Name: po_bostedsadresseperiode po_bostedsadresseperiode_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.po_bostedsadresseperiode
    ADD CONSTRAINT po_bostedsadresseperiode_pkey PRIMARY KEY (id);


--
-- Name: po_doedsfall po_doedsfall_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.po_doedsfall
    ADD CONSTRAINT po_doedsfall_pkey PRIMARY KEY (id);


--
-- Name: po_opphold po_opphold_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.po_opphold
    ADD CONSTRAINT po_opphold_pkey PRIMARY KEY (id);


--
-- Name: po_person po_person_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.po_person
    ADD CONSTRAINT po_person_pkey PRIMARY KEY (id);


--
-- Name: po_sivilstand po_sivilstand_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.po_sivilstand
    ADD CONSTRAINT po_sivilstand_pkey PRIMARY KEY (id);


--
-- Name: po_statsborgerskap po_statsborgerskap_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.po_statsborgerskap
    ADD CONSTRAINT po_statsborgerskap_pkey PRIMARY KEY (id);


--
-- Name: refusjon_eos refusjon_eos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refusjon_eos
    ADD CONSTRAINT refusjon_eos_pkey PRIMARY KEY (id);


--
-- Name: saksstatistikk_mellomlagring saksstatistikk_mellomlagring_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.saksstatistikk_mellomlagring
    ADD CONSTRAINT saksstatistikk_mellomlagring_pkey PRIMARY KEY (id);


--
-- Name: satskjoering satskjoering_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.satskjoering
    ADD CONSTRAINT satskjoering_pkey PRIMARY KEY (id);


--
-- Name: sett_paa_vent sett_paa_vent_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sett_paa_vent
    ADD CONSTRAINT sett_paa_vent_pkey PRIMARY KEY (id);


--
-- Name: skyggesak skyggesak_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.skyggesak
    ADD CONSTRAINT skyggesak_pkey PRIMARY KEY (id);


--
-- Name: tilbakekreving tilbakekreving_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tilbakekreving
    ADD CONSTRAINT tilbakekreving_pkey PRIMARY KEY (id);


--
-- Name: totrinnskontroll totrinnskontroll_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.totrinnskontroll
    ADD CONSTRAINT totrinnskontroll_pkey PRIMARY KEY (id);


--
-- Name: feilutbetalt_valuta trekk_i_loepende_utbetaling_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.feilutbetalt_valuta
    ADD CONSTRAINT trekk_i_loepende_utbetaling_pkey PRIMARY KEY (id);


--
-- Name: behandling_migreringsinfo unik_behandling_id; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.behandling_migreringsinfo
    ADD CONSTRAINT unik_behandling_id UNIQUE (fk_behandling_id);


--
-- Name: utenlandsk_periodebeloep utenlandsk_periodebeloep_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.utenlandsk_periodebeloep
    ADD CONSTRAINT utenlandsk_periodebeloep_pkey PRIMARY KEY (id);


--
-- Name: valutakurs valutakurs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.valutakurs
    ADD CONSTRAINT valutakurs_pkey PRIMARY KEY (id);


--
-- Name: okonomi_simulering_mottaker vedtak_simulering_mottaker_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.okonomi_simulering_mottaker
    ADD CONSTRAINT vedtak_simulering_mottaker_pkey PRIMARY KEY (id);


--
-- Name: okonomi_simulering_postering vedtak_simulering_postering_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.okonomi_simulering_postering
    ADD CONSTRAINT vedtak_simulering_postering_pkey PRIMARY KEY (id);


--
-- Name: vedtaksbegrunnelse_fritekst vedtaksbegrunnelse_fritekst_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vedtaksbegrunnelse_fritekst
    ADD CONSTRAINT vedtaksbegrunnelse_fritekst_pkey PRIMARY KEY (id);


--
-- Name: vedtaksbegrunnelse vedtaksbegrunnelse_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vedtaksbegrunnelse
    ADD CONSTRAINT vedtaksbegrunnelse_pkey PRIMARY KEY (id);


--
-- Name: vedtaksperiode vedtaksperiode_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vedtaksperiode
    ADD CONSTRAINT vedtaksperiode_pkey PRIMARY KEY (id);


--
-- Name: verge verge_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.verge
    ADD CONSTRAINT verge_pkey PRIMARY KEY (id);


--
-- Name: vilkar_resultat vilkar_resultat_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vilkar_resultat
    ADD CONSTRAINT vilkar_resultat_pkey PRIMARY KEY (id);


--
-- Name: andel_tilkjent_ytelse_fk_aktoer_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX andel_tilkjent_ytelse_fk_aktoer_idx ON public.andel_tilkjent_ytelse USING btree (fk_aktoer_id);


--
-- Name: andel_tilkjent_ytelse_fk_behandling_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX andel_tilkjent_ytelse_fk_behandling_id_idx ON public.andel_tilkjent_ytelse USING btree (fk_behandling_id);


--
-- Name: andel_tilkjent_ytelse_fk_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX andel_tilkjent_ytelse_fk_idx ON public.andel_tilkjent_ytelse USING btree (kilde_behandling_id);


--
-- Name: andel_tilkjent_ytelse_fk_tilkjent_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX andel_tilkjent_ytelse_fk_tilkjent_idx ON public.andel_tilkjent_ytelse USING btree (tilkjent_ytelse_id);


--
-- Name: annen_vurdering_fk_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX annen_vurdering_fk_idx ON public.annen_vurdering USING btree (fk_person_resultat_id);


--
-- Name: aty_type_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX aty_type_idx ON public.andel_tilkjent_ytelse USING btree (type);


--
-- Name: behandling_fk_fagsak_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX behandling_fk_fagsak_id_idx ON public.behandling USING btree (fk_fagsak_id);


--
-- Name: behandling_migreringsinfo_fk_behandling_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX behandling_migreringsinfo_fk_behandling_id_idx ON public.behandling_migreringsinfo USING btree (fk_behandling_id);


--
-- Name: behandling_opprettet_tid_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX behandling_opprettet_tid_idx ON public.behandling USING btree (opprettet_tid);


--
-- Name: behandling_soknadsinfo_fk_behandling_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX behandling_soknadsinfo_fk_behandling_id_idx ON public.behandling_soknadsinfo USING btree (fk_behandling_id);


--
-- Name: behandling_steg_tilstand_fk_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX behandling_steg_tilstand_fk_idx ON public.behandling_steg_tilstand USING btree (fk_behandling_id);


--
-- Name: behandling_vedtak_fk_behandling_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX behandling_vedtak_fk_behandling_id_idx ON public.vedtak USING btree (fk_behandling_id);


--
-- Name: beregning_resultat_fk_behandling_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX beregning_resultat_fk_behandling_id_idx ON public.tilkjent_ytelse USING btree (fk_behandling_id);


--
-- Name: brevmottaker_fk_behandling_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX brevmottaker_fk_behandling_id_idx ON public.brevmottaker USING btree (fk_behandling_id);


--
-- Name: data_chunk_transaksjons_id_chunk_nr_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX data_chunk_transaksjons_id_chunk_nr_idx ON public.data_chunk USING btree (transaksjons_id, chunk_nr);


--
-- Name: data_chunk_transaksjons_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX data_chunk_transaksjons_id_idx ON public.data_chunk USING btree (transaksjons_id);


--
-- Name: endret_utbetaling_andel_fk_behandling_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX endret_utbetaling_andel_fk_behandling_id_idx ON public.endret_utbetaling_andel USING btree (fk_behandling_id);


--
-- Name: endret_utbetaling_andel_fk_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX endret_utbetaling_andel_fk_idx ON public.endret_utbetaling_andel USING btree (fk_po_person_id);


--
-- Name: eos_begrunnelse_fk_vedtaksperiode_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX eos_begrunnelse_fk_vedtaksperiode_id_idx ON public.eos_begrunnelse USING btree (fk_vedtaksperiode_id);


--
-- Name: fagsak_fk_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX fagsak_fk_idx ON public.fagsak USING btree (fk_aktoer_id);


--
-- Name: fagsak_status_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX fagsak_status_idx ON public.fagsak USING btree (status);


--
-- Name: feilutbetalt_valuta_fk_behandling_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX feilutbetalt_valuta_fk_behandling_id_idx ON public.feilutbetalt_valuta USING btree (fk_behandling_id);


--
-- Name: foedselshendelse_pre_lansering_fk_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX foedselshendelse_pre_lansering_fk_idx ON public.foedselshendelse_pre_lansering USING btree (fk_aktoer_id);


--
-- Name: foedselshendelsefiltrering_resultat_fk_behandling_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX foedselshendelsefiltrering_resultat_fk_behandling_id_idx ON public.foedselshendelsefiltrering_resultat USING btree (fk_behandling_id);


--
-- Name: gr_periode_overgangsstonad_fk_behandling_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX gr_periode_overgangsstonad_fk_behandling_id_idx ON public.gr_periode_overgangsstonad USING btree (fk_behandling_id);


--
-- Name: gr_periode_overgangsstonad_fk_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX gr_periode_overgangsstonad_fk_idx ON public.gr_periode_overgangsstonad USING btree (fk_aktoer_id);


--
-- Name: gr_personopplysninger_fk_behandling_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX gr_personopplysninger_fk_behandling_id_idx ON public.gr_personopplysninger USING btree (fk_behandling_id);


--
-- Name: gr_soknad_fk_behandling_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX gr_soknad_fk_behandling_id_idx ON public.gr_soknad USING btree (fk_behandling_id);


--
-- Name: henvendelse_logg_henvendelse_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX henvendelse_logg_henvendelse_id_idx ON public.task_logg USING btree (task_id);


--
-- Name: henvendelse_status_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX henvendelse_status_idx ON public.task USING btree (status);


--
-- Name: journalpost_fk_behandling_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX journalpost_fk_behandling_id_idx ON public.journalpost USING btree (fk_behandling_id);


--
-- Name: journalpost_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX journalpost_id_idx ON public.behandling_soknadsinfo USING btree (journalpost_id);


--
-- Name: kompetanse_fk_behandling_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX kompetanse_fk_behandling_id_idx ON public.kompetanse USING btree (fk_behandling_id);


--
-- Name: korrigert_etterbetaling_fk_behandling_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX korrigert_etterbetaling_fk_behandling_id_idx ON public.korrigert_etterbetaling USING btree (fk_behandling_id);


--
-- Name: korrigert_vedtak_fk_behandling_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX korrigert_vedtak_fk_behandling_id_idx ON public.korrigert_vedtak USING btree (fk_behandling_id);


--
-- Name: logg_fk_behandling_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX logg_fk_behandling_id_idx ON public.logg USING btree (fk_behandling_id);


--
-- Name: okonomi_simulering_mottaker_fk_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX okonomi_simulering_mottaker_fk_idx ON public.okonomi_simulering_mottaker USING btree (fk_behandling_id);


--
-- Name: oppgave_fk_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX oppgave_fk_idx ON public.oppgave USING btree (fk_behandling_id);


--
-- Name: person_resultat_fk_aktoer_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX person_resultat_fk_aktoer_idx ON public.person_resultat USING btree (fk_aktoer_id);


--
-- Name: person_resultat_fk_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX person_resultat_fk_idx ON public.person_resultat USING btree (fk_vilkaarsvurdering_id);


--
-- Name: personident_aktoer_id_alle_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX personident_aktoer_id_alle_idx ON public.personident USING btree (fk_aktoer_id);


--
-- Name: po_arbeidsforhold_fk_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX po_arbeidsforhold_fk_idx ON public.po_arbeidsforhold USING btree (fk_po_person_id);


--
-- Name: po_bostedsadresse_fk_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX po_bostedsadresse_fk_idx ON public.po_bostedsadresse USING btree (fk_po_person_id);


--
-- Name: po_bostedsadresseperiode_fk_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX po_bostedsadresseperiode_fk_idx ON public.po_bostedsadresseperiode USING btree (fk_po_person_id);


--
-- Name: po_doedsfall_fk_po_person_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX po_doedsfall_fk_po_person_id_idx ON public.po_doedsfall USING btree (fk_po_person_id);


--
-- Name: po_opphold_fk_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX po_opphold_fk_idx ON public.po_opphold USING btree (fk_po_person_id);


--
-- Name: po_person_fk_gr_personopplysninger_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX po_person_fk_gr_personopplysninger_id_idx ON public.po_person USING btree (fk_gr_personopplysninger_id);


--
-- Name: po_person_fk_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX po_person_fk_idx ON public.po_person USING btree (fk_aktoer_id);


--
-- Name: po_sivilstand_fk_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX po_sivilstand_fk_idx ON public.po_sivilstand USING btree (fk_po_person_id);


--
-- Name: po_statsborgerskap_fk_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX po_statsborgerskap_fk_idx ON public.po_statsborgerskap USING btree (fk_po_person_id);


--
-- Name: refusjon_eos_fk_behandling_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX refusjon_eos_fk_behandling_id_idx ON public.refusjon_eos USING btree (fk_behandling_id);


--
-- Name: saksstatistikk_mellomlagring_sendt_tid_null_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX saksstatistikk_mellomlagring_sendt_tid_null_idx ON public.saksstatistikk_mellomlagring USING btree (sendt_tid) WHERE (sendt_tid IS NULL);


--
-- Name: saksstatistikk_mellomlagring_type_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX saksstatistikk_mellomlagring_type_id_idx ON public.saksstatistikk_mellomlagring USING btree (type_id);


--
-- Name: satskjoering_fagsak_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX satskjoering_fagsak_id_idx ON public.satskjoering USING btree (fk_fagsak_id);


--
-- Name: sett_paa_vent_fk_behandling_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX sett_paa_vent_fk_behandling_id_idx ON public.sett_paa_vent USING btree (fk_behandling_id);


--
-- Name: skyggesak_fagsak_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX skyggesak_fagsak_id_idx ON public.skyggesak USING btree (fk_fagsak_id);


--
-- Name: tilbakekreving_fk_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX tilbakekreving_fk_idx ON public.tilbakekreving USING btree (fk_behandling_id);


--
-- Name: tilkjent_ytelse_utbetalingsoppdrag_not_null_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX tilkjent_ytelse_utbetalingsoppdrag_not_null_idx ON public.tilkjent_ytelse USING btree (utbetalingsoppdrag) WHERE (utbetalingsoppdrag IS NOT NULL);


--
-- Name: totrinnskontroll_fk_behandling_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX totrinnskontroll_fk_behandling_id_idx ON public.totrinnskontroll USING btree (fk_behandling_id);


--
-- Name: uidx_behandling_01; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_behandling_01 ON public.behandling USING btree ((
CASE
    WHEN (aktiv = true) THEN fk_fagsak_id
    ELSE NULL::bigint
END), (
CASE
    WHEN (aktiv = true) THEN aktiv
    ELSE NULL::boolean
END));


--
-- Name: uidx_behandling_02; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_behandling_02 ON public.behandling USING btree (fk_fagsak_id) WHERE (((status)::text <> 'AVSLUTTET'::text) AND ((status)::text <> 'SATT_PÅ_MASKINELL_VENT'::text));


--
-- Name: uidx_behandling_03; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_behandling_03 ON public.behandling USING btree (fk_fagsak_id) WHERE ((status)::text = 'SATT_PÅ_MASKINELL_VENT'::text);


--
-- Name: uidx_behandling_vedtak_01; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_behandling_vedtak_01 ON public.vedtak USING btree ((
CASE
    WHEN (aktiv = true) THEN fk_behandling_id
    ELSE NULL::bigint
END), (
CASE
    WHEN (aktiv = true) THEN aktiv
    ELSE NULL::boolean
END));


--
-- Name: uidx_fagsak_type_aktoer_ikke_arkivert; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_fagsak_type_aktoer_ikke_arkivert ON public.fagsak USING btree (type, fk_aktoer_id) WHERE ((fk_institusjon_id IS NULL) AND (arkivert = false));


--
-- Name: uidx_fagsak_type_aktoer_institusjon_ikke_arkivert; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_fagsak_type_aktoer_institusjon_ikke_arkivert ON public.fagsak USING btree (type, fk_aktoer_id, fk_institusjon_id) WHERE ((fk_institusjon_id IS NOT NULL) AND (arkivert = false));


--
-- Name: uidx_gr_personopplysninger_01; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_gr_personopplysninger_01 ON public.gr_personopplysninger USING btree ((
CASE
    WHEN (aktiv = true) THEN fk_behandling_id
    ELSE NULL::bigint
END), (
CASE
    WHEN (aktiv = true) THEN aktiv
    ELSE NULL::boolean
END));


--
-- Name: uidx_gr_soknad_01; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_gr_soknad_01 ON public.gr_soknad USING btree ((
CASE
    WHEN (aktiv = true) THEN fk_behandling_id
    ELSE NULL::bigint
END), (
CASE
    WHEN (aktiv = true) THEN aktiv
    ELSE NULL::boolean
END));


--
-- Name: uidx_institusjon_org_nummer; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_institusjon_org_nummer ON public.institusjon USING btree (org_nummer);


--
-- Name: uidx_institusjon_tss_ekstern_id; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_institusjon_tss_ekstern_id ON public.institusjon USING btree (tss_ekstern_id);


--
-- Name: uidx_korrigert_etterbetaling_fk_behandling_id_aktiv; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_korrigert_etterbetaling_fk_behandling_id_aktiv ON public.korrigert_etterbetaling USING btree (fk_behandling_id) WHERE (aktiv = true);


--
-- Name: uidx_korrigert_vedtak_fk_behandling_id_aktiv; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_korrigert_vedtak_fk_behandling_id_aktiv ON public.korrigert_vedtak USING btree (fk_behandling_id) WHERE (aktiv = true);


--
-- Name: uidx_personident_aktoer_id; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_personident_aktoer_id ON public.personident USING btree (fk_aktoer_id) WHERE (aktiv = true);


--
-- Name: uidx_personident_foedselsnummer_id; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_personident_foedselsnummer_id ON public.personident USING btree (foedselsnummer);


--
-- Name: uidx_sett_paa_vent_aktiv; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_sett_paa_vent_aktiv ON public.sett_paa_vent USING btree (fk_behandling_id, aktiv) WHERE (aktiv = true);


--
-- Name: uidx_totrinnskontroll_01; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_totrinnskontroll_01 ON public.totrinnskontroll USING btree ((
CASE
    WHEN (aktiv = true) THEN fk_behandling_id
    ELSE NULL::bigint
END), (
CASE
    WHEN (aktiv = true) THEN aktiv
    ELSE NULL::boolean
END));


--
-- Name: uidx_verge_behandling_id; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uidx_verge_behandling_id ON public.verge USING btree (fk_behandling_id);


--
-- Name: utenlandsk_periodebeloep_fk_behandling_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX utenlandsk_periodebeloep_fk_behandling_id_idx ON public.utenlandsk_periodebeloep USING btree (fk_behandling_id);


--
-- Name: valutakurs_fk_behandling_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX valutakurs_fk_behandling_id_idx ON public.valutakurs USING btree (fk_behandling_id);


--
-- Name: vedtak_simulering_postering_fk_vedtak_simulering_mottaker_i_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX vedtak_simulering_postering_fk_vedtak_simulering_mottaker_i_idx ON public.okonomi_simulering_postering USING btree (fk_okonomi_simulering_mottaker_id);


--
-- Name: vedtaksbegrunnelse_fk_vedtaksperiode_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX vedtaksbegrunnelse_fk_vedtaksperiode_id_idx ON public.vedtaksbegrunnelse USING btree (fk_vedtaksperiode_id);


--
-- Name: vedtaksbegrunnelse_fritekst_fk_vedtaksperiode_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX vedtaksbegrunnelse_fritekst_fk_vedtaksperiode_id_idx ON public.vedtaksbegrunnelse_fritekst USING btree (fk_vedtaksperiode_id);


--
-- Name: vedtaksperiode_fk_vedtak_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX vedtaksperiode_fk_vedtak_id_idx ON public.vedtaksperiode USING btree (fk_vedtak_id);


--
-- Name: vilkaarsvurdering_fk_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX vilkaarsvurdering_fk_idx ON public.vilkaarsvurdering USING btree (fk_behandling_id);


--
-- Name: vilkar_resultat_fk_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX vilkar_resultat_fk_idx ON public.vilkar_resultat USING btree (fk_behandling_id);


--
-- Name: vilkar_resultat_fk_personr_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX vilkar_resultat_fk_personr_idx ON public.vilkar_resultat USING btree (fk_person_resultat_id);


--
-- Name: aktoer_til_kompetanse aktoer_til_kompetanse_fk_aktoer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.aktoer_til_kompetanse
    ADD CONSTRAINT aktoer_til_kompetanse_fk_aktoer_id_fkey FOREIGN KEY (fk_aktoer_id) REFERENCES public.aktoer(aktoer_id) ON UPDATE CASCADE;


--
-- Name: aktoer_til_kompetanse aktoer_til_kompetanse_fk_kompetanse_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.aktoer_til_kompetanse
    ADD CONSTRAINT aktoer_til_kompetanse_fk_kompetanse_id_fkey FOREIGN KEY (fk_kompetanse_id) REFERENCES public.kompetanse(id);


--
-- Name: aktoer_til_utenlandsk_periodebeloep aktoer_til_utenlandsk_periode_fk_utenlandsk_periodebeloep__fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.aktoer_til_utenlandsk_periodebeloep
    ADD CONSTRAINT aktoer_til_utenlandsk_periode_fk_utenlandsk_periodebeloep__fkey FOREIGN KEY (fk_utenlandsk_periodebeloep_id) REFERENCES public.utenlandsk_periodebeloep(id);


--
-- Name: aktoer_til_utenlandsk_periodebeloep aktoer_til_utenlandsk_periodebeloep_fk_aktoer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.aktoer_til_utenlandsk_periodebeloep
    ADD CONSTRAINT aktoer_til_utenlandsk_periodebeloep_fk_aktoer_id_fkey FOREIGN KEY (fk_aktoer_id) REFERENCES public.aktoer(aktoer_id) ON UPDATE CASCADE;


--
-- Name: aktoer_til_valutakurs aktoer_til_valutakurs_fk_aktoer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.aktoer_til_valutakurs
    ADD CONSTRAINT aktoer_til_valutakurs_fk_aktoer_id_fkey FOREIGN KEY (fk_aktoer_id) REFERENCES public.aktoer(aktoer_id) ON UPDATE CASCADE;


--
-- Name: aktoer_til_valutakurs aktoer_til_valutakurs_fk_valutakurs_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.aktoer_til_valutakurs
    ADD CONSTRAINT aktoer_til_valutakurs_fk_valutakurs_id_fkey FOREIGN KEY (fk_valutakurs_id) REFERENCES public.valutakurs(id);


--
-- Name: andel_tilkjent_ytelse andel_tilkjent_ytelse_fk_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.andel_tilkjent_ytelse
    ADD CONSTRAINT andel_tilkjent_ytelse_fk_behandling_id_fkey FOREIGN KEY (fk_behandling_id) REFERENCES public.behandling(id);


--
-- Name: andel_tilkjent_ytelse andel_tilkjent_ytelse_kilde_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.andel_tilkjent_ytelse
    ADD CONSTRAINT andel_tilkjent_ytelse_kilde_behandling_id_fkey FOREIGN KEY (kilde_behandling_id) REFERENCES public.behandling(id);


--
-- Name: andel_tilkjent_ytelse andel_tilkjent_ytelse_tilkjent_ytelse_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.andel_tilkjent_ytelse
    ADD CONSTRAINT andel_tilkjent_ytelse_tilkjent_ytelse_id_fkey FOREIGN KEY (tilkjent_ytelse_id) REFERENCES public.tilkjent_ytelse(id) ON DELETE CASCADE;


--
-- Name: annen_vurdering annen_vurdering_fk_person_resultat_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.annen_vurdering
    ADD CONSTRAINT annen_vurdering_fk_person_resultat_id_fkey FOREIGN KEY (fk_person_resultat_id) REFERENCES public.person_resultat(id);


--
-- Name: behandling behandling_fk_fagsak_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.behandling
    ADD CONSTRAINT behandling_fk_fagsak_id_fkey FOREIGN KEY (fk_fagsak_id) REFERENCES public.fagsak(id);


--
-- Name: behandling_migreringsinfo behandling_migreringsinfo_fk_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.behandling_migreringsinfo
    ADD CONSTRAINT behandling_migreringsinfo_fk_behandling_id_fkey FOREIGN KEY (fk_behandling_id) REFERENCES public.behandling(id);


--
-- Name: vilkaarsvurdering behandling_resultat_fk_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vilkaarsvurdering
    ADD CONSTRAINT behandling_resultat_fk_behandling_id_fkey FOREIGN KEY (fk_behandling_id) REFERENCES public.behandling(id);


--
-- Name: behandling_soknadsinfo behandling_soknadsinfo_fk_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.behandling_soknadsinfo
    ADD CONSTRAINT behandling_soknadsinfo_fk_behandling_id_fkey FOREIGN KEY (fk_behandling_id) REFERENCES public.behandling(id);


--
-- Name: behandling_steg_tilstand behandling_steg_tilstand_fk_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.behandling_steg_tilstand
    ADD CONSTRAINT behandling_steg_tilstand_fk_behandling_id_fkey FOREIGN KEY (fk_behandling_id) REFERENCES public.behandling(id);


--
-- Name: vedtak behandling_vedtak_fk_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vedtak
    ADD CONSTRAINT behandling_vedtak_fk_behandling_id_fkey FOREIGN KEY (fk_behandling_id) REFERENCES public.behandling(id);


--
-- Name: tilkjent_ytelse beregning_resultat_fk_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tilkjent_ytelse
    ADD CONSTRAINT beregning_resultat_fk_behandling_id_fkey FOREIGN KEY (fk_behandling_id) REFERENCES public.behandling(id);


--
-- Name: brevmottaker brevmottaker_fk_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brevmottaker
    ADD CONSTRAINT brevmottaker_fk_behandling_id_fkey FOREIGN KEY (fk_behandling_id) REFERENCES public.behandling(id) ON DELETE CASCADE;


--
-- Name: data_chunk data_chunk_fk_batch_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.data_chunk
    ADD CONSTRAINT data_chunk_fk_batch_id_fkey FOREIGN KEY (fk_batch_id) REFERENCES public.batch(id);


--
-- Name: endret_utbetaling_andel endret_utbetaling_andel_fk_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.endret_utbetaling_andel
    ADD CONSTRAINT endret_utbetaling_andel_fk_behandling_id_fkey FOREIGN KEY (fk_behandling_id) REFERENCES public.behandling(id);


--
-- Name: endret_utbetaling_andel endret_utbetaling_andel_fk_po_person_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.endret_utbetaling_andel
    ADD CONSTRAINT endret_utbetaling_andel_fk_po_person_id_fkey FOREIGN KEY (fk_po_person_id) REFERENCES public.po_person(id);


--
-- Name: eos_begrunnelse eos_begrunnelse_fk_vedtaksperiode_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.eos_begrunnelse
    ADD CONSTRAINT eos_begrunnelse_fk_vedtaksperiode_id_fkey FOREIGN KEY (fk_vedtaksperiode_id) REFERENCES public.vedtaksperiode(id) ON DELETE CASCADE;


--
-- Name: fagsak fagsak; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fagsak
    ADD CONSTRAINT fagsak FOREIGN KEY (fk_aktoer_id) REFERENCES public.aktoer(aktoer_id) ON UPDATE CASCADE;


--
-- Name: fagsak fagsak_fk_institusjon_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fagsak
    ADD CONSTRAINT fagsak_fk_institusjon_id_fkey FOREIGN KEY (fk_institusjon_id) REFERENCES public.institusjon(id);


--
-- Name: andel_tilkjent_ytelse fk_andel_tilkjent_ytelse; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.andel_tilkjent_ytelse
    ADD CONSTRAINT fk_andel_tilkjent_ytelse FOREIGN KEY (fk_aktoer_id) REFERENCES public.aktoer(aktoer_id) ON UPDATE CASCADE;


--
-- Name: vilkar_resultat fk_behandling_id_vilkar_resultat; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vilkar_resultat
    ADD CONSTRAINT fk_behandling_id_vilkar_resultat FOREIGN KEY (fk_behandling_id) REFERENCES public.behandling(id);


--
-- Name: foedselshendelse_pre_lansering fk_foedselshendelse_pre_lansering; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.foedselshendelse_pre_lansering
    ADD CONSTRAINT fk_foedselshendelse_pre_lansering FOREIGN KEY (fk_aktoer_id) REFERENCES public.aktoer(aktoer_id) ON UPDATE CASCADE;


--
-- Name: gr_periode_overgangsstonad fk_gr_periode_overgangsstonad; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.gr_periode_overgangsstonad
    ADD CONSTRAINT fk_gr_periode_overgangsstonad FOREIGN KEY (fk_aktoer_id) REFERENCES public.aktoer(aktoer_id) ON UPDATE CASCADE;


--
-- Name: person_resultat fk_person_resultat; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.person_resultat
    ADD CONSTRAINT fk_person_resultat FOREIGN KEY (fk_aktoer_id) REFERENCES public.aktoer(aktoer_id) ON UPDATE CASCADE;


--
-- Name: personident fk_personident; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.personident
    ADD CONSTRAINT fk_personident FOREIGN KEY (fk_aktoer_id) REFERENCES public.aktoer(aktoer_id) ON UPDATE CASCADE;


--
-- Name: po_person fk_po_person; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.po_person
    ADD CONSTRAINT fk_po_person FOREIGN KEY (fk_aktoer_id) REFERENCES public.aktoer(aktoer_id) ON UPDATE CASCADE;


--
-- Name: foedselshendelsefiltrering_resultat foedselshendelsefiltrering_resultat_fk_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.foedselshendelsefiltrering_resultat
    ADD CONSTRAINT foedselshendelsefiltrering_resultat_fk_behandling_id_fkey FOREIGN KEY (fk_behandling_id) REFERENCES public.behandling(id);


--
-- Name: gr_periode_overgangsstonad gr_periode_overgangsstonad_fk_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.gr_periode_overgangsstonad
    ADD CONSTRAINT gr_periode_overgangsstonad_fk_behandling_id_fkey FOREIGN KEY (fk_behandling_id) REFERENCES public.behandling(id);


--
-- Name: gr_personopplysninger gr_personopplysninger_fk_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.gr_personopplysninger
    ADD CONSTRAINT gr_personopplysninger_fk_behandling_id_fkey FOREIGN KEY (fk_behandling_id) REFERENCES public.behandling(id);


--
-- Name: gr_soknad gr_soknad_fk_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.gr_soknad
    ADD CONSTRAINT gr_soknad_fk_behandling_id_fkey FOREIGN KEY (fk_behandling_id) REFERENCES public.behandling(id);


--
-- Name: task_logg henvendelse_logg_henvendelse_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.task_logg
    ADD CONSTRAINT henvendelse_logg_henvendelse_id_fkey FOREIGN KEY (task_id) REFERENCES public.task(id);


--
-- Name: journalpost journalpost_fk_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.journalpost
    ADD CONSTRAINT journalpost_fk_behandling_id_fkey FOREIGN KEY (fk_behandling_id) REFERENCES public.behandling(id);


--
-- Name: kompetanse kompetanse_fk_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.kompetanse
    ADD CONSTRAINT kompetanse_fk_behandling_id_fkey FOREIGN KEY (fk_behandling_id) REFERENCES public.behandling(id);


--
-- Name: korrigert_etterbetaling korrigert_etterbetaling_fk_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.korrigert_etterbetaling
    ADD CONSTRAINT korrigert_etterbetaling_fk_behandling_id_fkey FOREIGN KEY (fk_behandling_id) REFERENCES public.behandling(id);


--
-- Name: korrigert_vedtak korrigert_vedtak_fk_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.korrigert_vedtak
    ADD CONSTRAINT korrigert_vedtak_fk_behandling_id_fkey FOREIGN KEY (fk_behandling_id) REFERENCES public.behandling(id);


--
-- Name: logg logg_fk_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.logg
    ADD CONSTRAINT logg_fk_behandling_id_fkey FOREIGN KEY (fk_behandling_id) REFERENCES public.behandling(id);


--
-- Name: okonomi_simulering_mottaker okonomi_simulering_mottaker_fk_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.okonomi_simulering_mottaker
    ADD CONSTRAINT okonomi_simulering_mottaker_fk_behandling_id_fkey FOREIGN KEY (fk_behandling_id) REFERENCES public.behandling(id);


--
-- Name: oppgave oppgave_fk_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.oppgave
    ADD CONSTRAINT oppgave_fk_behandling_id_fkey FOREIGN KEY (fk_behandling_id) REFERENCES public.behandling(id);


--
-- Name: person_resultat periode_resultat_fk_behandling_resultat_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.person_resultat
    ADD CONSTRAINT periode_resultat_fk_behandling_resultat_id_fkey FOREIGN KEY (fk_vilkaarsvurdering_id) REFERENCES public.vilkaarsvurdering(id);


--
-- Name: po_arbeidsforhold po_arbeidsforhold_fk_po_person_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.po_arbeidsforhold
    ADD CONSTRAINT po_arbeidsforhold_fk_po_person_id_fkey FOREIGN KEY (fk_po_person_id) REFERENCES public.po_person(id);


--
-- Name: po_bostedsadresse po_bostedsadresse_fk_po_person_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.po_bostedsadresse
    ADD CONSTRAINT po_bostedsadresse_fk_po_person_id_fkey FOREIGN KEY (fk_po_person_id) REFERENCES public.po_person(id);


--
-- Name: po_bostedsadresseperiode po_bostedsadresseperiode_fk_po_person_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.po_bostedsadresseperiode
    ADD CONSTRAINT po_bostedsadresseperiode_fk_po_person_id_fkey FOREIGN KEY (fk_po_person_id) REFERENCES public.po_person(id);


--
-- Name: po_doedsfall po_doedsfall_fk_po_person_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.po_doedsfall
    ADD CONSTRAINT po_doedsfall_fk_po_person_id_fkey FOREIGN KEY (fk_po_person_id) REFERENCES public.po_person(id);


--
-- Name: po_opphold po_opphold_fk_po_person_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.po_opphold
    ADD CONSTRAINT po_opphold_fk_po_person_id_fkey FOREIGN KEY (fk_po_person_id) REFERENCES public.po_person(id);


--
-- Name: po_person po_person_fk_gr_personopplysninger_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.po_person
    ADD CONSTRAINT po_person_fk_gr_personopplysninger_id_fkey FOREIGN KEY (fk_gr_personopplysninger_id) REFERENCES public.gr_personopplysninger(id);


--
-- Name: po_sivilstand po_sivilstand_fk_po_person_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.po_sivilstand
    ADD CONSTRAINT po_sivilstand_fk_po_person_id_fkey FOREIGN KEY (fk_po_person_id) REFERENCES public.po_person(id);


--
-- Name: po_statsborgerskap po_statsborgerskap_fk_po_person_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.po_statsborgerskap
    ADD CONSTRAINT po_statsborgerskap_fk_po_person_id_fkey FOREIGN KEY (fk_po_person_id) REFERENCES public.po_person(id);


--
-- Name: refusjon_eos refusjon_eos_fk_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refusjon_eos
    ADD CONSTRAINT refusjon_eos_fk_behandling_id_fkey FOREIGN KEY (fk_behandling_id) REFERENCES public.behandling(id);


--
-- Name: satskjoering satskjoering_fk_fagsak_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.satskjoering
    ADD CONSTRAINT satskjoering_fk_fagsak_id_fkey FOREIGN KEY (fk_fagsak_id) REFERENCES public.fagsak(id) ON DELETE CASCADE;


--
-- Name: sett_paa_vent sett_paa_vent_fk_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sett_paa_vent
    ADD CONSTRAINT sett_paa_vent_fk_behandling_id_fkey FOREIGN KEY (fk_behandling_id) REFERENCES public.behandling(id);


--
-- Name: tilbakekreving tilbakekreving_fk_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tilbakekreving
    ADD CONSTRAINT tilbakekreving_fk_behandling_id_fkey FOREIGN KEY (fk_behandling_id) REFERENCES public.behandling(id);


--
-- Name: totrinnskontroll totrinnskontroll_fk_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.totrinnskontroll
    ADD CONSTRAINT totrinnskontroll_fk_behandling_id_fkey FOREIGN KEY (fk_behandling_id) REFERENCES public.behandling(id);


--
-- Name: feilutbetalt_valuta trekk_i_loepende_utbetaling_fk_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.feilutbetalt_valuta
    ADD CONSTRAINT trekk_i_loepende_utbetaling_fk_behandling_id_fkey FOREIGN KEY (fk_behandling_id) REFERENCES public.behandling(id);


--
-- Name: utenlandsk_periodebeloep utenlandsk_periodebeloep_fk_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.utenlandsk_periodebeloep
    ADD CONSTRAINT utenlandsk_periodebeloep_fk_behandling_id_fkey FOREIGN KEY (fk_behandling_id) REFERENCES public.behandling(id);


--
-- Name: valutakurs valutakurs_fk_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.valutakurs
    ADD CONSTRAINT valutakurs_fk_behandling_id_fkey FOREIGN KEY (fk_behandling_id) REFERENCES public.behandling(id);


--
-- Name: okonomi_simulering_postering vedtak_simulering_postering_fk_vedtak_simulering_mottaker__fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.okonomi_simulering_postering
    ADD CONSTRAINT vedtak_simulering_postering_fk_vedtak_simulering_mottaker__fkey FOREIGN KEY (fk_okonomi_simulering_mottaker_id) REFERENCES public.okonomi_simulering_mottaker(id) ON DELETE CASCADE;


--
-- Name: vedtaksbegrunnelse vedtaksbegrunnelse_fk_vedtaksperiode_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vedtaksbegrunnelse
    ADD CONSTRAINT vedtaksbegrunnelse_fk_vedtaksperiode_id_fkey FOREIGN KEY (fk_vedtaksperiode_id) REFERENCES public.vedtaksperiode(id) ON DELETE CASCADE;


--
-- Name: vedtaksbegrunnelse_fritekst vedtaksbegrunnelse_fritekst_fk_vedtaksperiode_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vedtaksbegrunnelse_fritekst
    ADD CONSTRAINT vedtaksbegrunnelse_fritekst_fk_vedtaksperiode_id_fkey FOREIGN KEY (fk_vedtaksperiode_id) REFERENCES public.vedtaksperiode(id) ON DELETE CASCADE;


--
-- Name: vedtaksperiode vedtaksperiode_fk_vedtak_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vedtaksperiode
    ADD CONSTRAINT vedtaksperiode_fk_vedtak_id_fkey FOREIGN KEY (fk_vedtak_id) REFERENCES public.vedtak(id);


--
-- Name: verge verge_fk_behandling_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.verge
    ADD CONSTRAINT verge_fk_behandling_id_fkey FOREIGN KEY (fk_behandling_id) REFERENCES public.behandling(id);


--
-- Name: vilkar_resultat vilkar_resultat_fk_person_resultat_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vilkar_resultat
    ADD CONSTRAINT vilkar_resultat_fk_person_resultat_id_fkey FOREIGN KEY (fk_person_resultat_id) REFERENCES public.person_resultat(id);


--
-- PostgreSQL database dump complete
--

