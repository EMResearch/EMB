CREATE TABLE fagsak (
    id                UUID PRIMARY KEY,
    versjon           BIGINT                              NOT NULL,
    fagsystem         VARCHAR                             NOT NULL,
    ekstern_fagsak_id VARCHAR,
    status            VARCHAR                             NOT NULL,
    bruker_ident      VARCHAR,
    bruker_sprakkode  VARCHAR      DEFAULT 'NB'           NOT NULL,
    ytelsestype       VARCHAR      DEFAULT 'BA'           NOT NULL,
    opprettet_av      VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid     TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    endret_av         VARCHAR,
    endret_tid        TIMESTAMP(3)
);

COMMENT ON TABLE fagsak
    IS 'Fagsak for tilbakekreving. Alle behandling er koblet mot en fagsak.';

COMMENT ON COLUMN fagsak.id
    IS 'Primary key';

COMMENT ON COLUMN fagsak.fagsystem
    IS 'Fagsystemet som er eier av tilbakekrevingsbehandling';

COMMENT ON COLUMN fagsak.ekstern_fagsak_id
    IS 'Saksnummer (som gsak har mottatt)';

COMMENT ON COLUMN fagsak.status
    IS 'Fk:status fagsak';

COMMENT ON COLUMN fagsak.bruker_ident
    IS 'Fk:Ident på bruker';

COMMENT ON COLUMN fagsak.ytelsestype
    IS 'Fremmednøkkel til kodeverkstabellen som inneholder oversikt over ytelser';

CREATE UNIQUE INDEX ON fagsak (ekstern_fagsak_id);

CREATE INDEX ON fagsak (status);

CREATE INDEX ON fagsak (bruker_ident);

CREATE INDEX ON fagsak (ytelsestype);

CREATE TABLE behandling (
    id                      UUID PRIMARY KEY,
    versjon                 BIGINT                              NOT NULL,
    fagsak_id               UUID                                NOT NULL REFERENCES fagsak,
    status                  VARCHAR                             NOT NULL,
    type                    VARCHAR                             NOT NULL,
    opprettet_dato          DATE         DEFAULT current_date   NOT NULL,
    avsluttet_dato          DATE,
    ansvarlig_saksbehandler VARCHAR,
    ansvarlig_beslutter     VARCHAR,
    behandlende_enhet       VARCHAR,
    behandlende_enhets_navn VARCHAR,
    opprettet_av            VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid           TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    endret_av               VARCHAR,
    endret_tid              TIMESTAMP(3),
    manuelt_opprettet       BOOLEAN                             NOT NULL,
    ekstern_bruk_id         UUID                                NOT NULL,
    saksbehandlingstype     VARCHAR                             NOT NULL
        CONSTRAINT chk_saksbehandlingstype
            CHECK (saksbehandlingstype IN ('ORDINÆR', 'AUTOMATISK_IKKE_INNKREVING_LAVT_BELØP'))
);

COMMENT ON TABLE behandling
    IS 'Behandling av fagsak';

COMMENT ON COLUMN behandling.id
    IS 'Primary key';

COMMENT ON COLUMN behandling.fagsak_id
    IS 'Fk: fagsak fremmednøkkel for kobling til fagsak';

COMMENT ON COLUMN behandling.status
    IS 'Fk: behandlingsstatus fremmednøkkel til tabellen som viser status på behandlinger';

COMMENT ON COLUMN behandling.type
    IS 'Fk: type behandling ';

COMMENT ON COLUMN behandling.opprettet_dato
    IS 'Dato når behandlingen ble opprettet.';

COMMENT ON COLUMN behandling.avsluttet_dato
    IS 'Dato når behandlingen ble avsluttet.';

COMMENT ON COLUMN behandling.ansvarlig_saksbehandler
    IS 'Id til saksbehandler som oppretter forslag til vedtak ved totrinnsbehandling.';

COMMENT ON COLUMN behandling.ansvarlig_beslutter
    IS 'Beslutter som har fattet vedtaket';

COMMENT ON COLUMN behandling.behandlende_enhet
    IS 'Nav-enhet som behandler behandlingen';

COMMENT ON COLUMN behandling.behandlende_enhets_navn
    IS 'Navn på behandlende enhet';

COMMENT ON COLUMN behandling.manuelt_opprettet
    IS 'Angir om behandlingen ble opprettet manuelt. ';

COMMENT ON COLUMN behandling.ekstern_bruk_id
    IS 'Unik uuid for behandling til utvortes bruk';

COMMENT ON COLUMN behandling.saksbehandlingstype
    IS 'Angir hvordan behandlingen saksbehandles ';

CREATE INDEX ON behandling (fagsak_id);

CREATE INDEX ON behandling (status);

CREATE INDEX ON behandling (type);

CREATE UNIQUE INDEX ON behandling (ekstern_bruk_id);

CREATE TABLE ekstern_behandling (
    id            UUID PRIMARY KEY,
    versjon       BIGINT                              NOT NULL,
    behandling_id UUID                                NOT NULL REFERENCES behandling,
    aktiv         BOOLEAN      DEFAULT TRUE           NOT NULL,
    ekstern_id    VARCHAR                             NOT NULL,
    opprettet_av  VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    endret_av     VARCHAR,
    endret_tid    TIMESTAMP(3),
    UNIQUE (behandling_id, ekstern_id)
);

COMMENT ON TABLE ekstern_behandling
    IS 'Referanse til ekstern behandling';

COMMENT ON COLUMN ekstern_behandling.id
    IS 'Primary key';

COMMENT ON COLUMN ekstern_behandling.behandling_id
    IS 'Fk: behandling fremmednøkkel for kobling til intern behandling';

COMMENT ON COLUMN ekstern_behandling.aktiv
    IS 'Angir om ekstern behandling data er gjeldende';

COMMENT ON COLUMN ekstern_behandling.ekstern_id
    IS 'ekstern_id;referanse. Peker på referanse-feltet i kravgrunnlaget, og kommer opprinnelig fra fagsystemet.';

CREATE INDEX ON ekstern_behandling (behandling_id);

CREATE INDEX ON ekstern_behandling (ekstern_id);

CREATE TABLE varsel (
    id                      UUID PRIMARY KEY,
    versjon                 BIGINT                              NOT NULL,
    behandling_id           UUID                                NOT NULL REFERENCES behandling,
    aktiv                   BOOLEAN                             NOT NULL,
    varseltekst             VARCHAR                             NOT NULL,
    varselbelop             BIGINT,
    revurderingsvedtaksdato DATE                                NOT NULL,
    opprettet_av            VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid           TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    endret_av               VARCHAR,
    endret_tid              TIMESTAMP(3)
);

COMMENT ON TABLE varsel
    IS 'Tabell for å lagre varsel info';

COMMENT ON COLUMN varsel.id
    IS 'Primary key';

COMMENT ON COLUMN varsel.behandling_id
    IS 'Fk: behandling fremmednøkkel for tilknyttet behandling';

COMMENT ON COLUMN varsel.aktiv
    IS 'Angir status av varsel';

COMMENT ON COLUMN varsel.varseltekst
    IS 'Fritekst som brukes i varselbrev';

COMMENT ON COLUMN varsel.varselbelop
    IS 'Beløp som brukes i varselbrev';

COMMENT ON COLUMN varsel.revurderingsvedtaksdato
    IS 'vedtaksdato av fagsystemsrevurdering. Brukes av varselbrev';

CREATE INDEX ON varsel (behandling_id);

CREATE TABLE varselsperiode (
    id            UUID PRIMARY KEY,
    versjon       BIGINT                              NOT NULL,
    varsel_id     UUID                                NOT NULL REFERENCES varsel,
    fom           DATE                                NOT NULL,
    tom           DATE                                NOT NULL,
    opprettet_av  VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    endret_av     VARCHAR,
    endret_tid    TIMESTAMP(3)
);

COMMENT ON TABLE varselsperiode
    IS 'Feilutbetalingsperiode som brukes i varselbrev';

COMMENT ON COLUMN varselsperiode.id
    IS 'Primary key';

COMMENT ON COLUMN varselsperiode.varsel_id
    IS 'FK: varsel fremmednøkel for kobling til varsel';

COMMENT ON COLUMN varselsperiode.fom
    IS 'Første dag av feilutbetalingsperiode';

COMMENT ON COLUMN varselsperiode.fom
    IS 'Siste dag av feilutbetalingsperiode';

COMMENT ON COLUMN varselsperiode.versjon
    IS 'Bruker for optimistisk låsing';

CREATE INDEX ON varselsperiode (varsel_id);

CREATE TABLE verge (
    id            UUID PRIMARY KEY,
    behandling_id UUID                                NOT NULL REFERENCES behandling,
    versjon       BIGINT                              NOT NULL,
    ident         VARCHAR,
    gyldig_fom    DATE                                NOT NULL,
    gyldig_tom    DATE                                NOT NULL,
    aktiv         BOOLEAN                             NOT NULL,
    type          VARCHAR                             NOT NULL,
    org_nr        VARCHAR,
    navn          VARCHAR                             NOT NULL,
    kilde         VARCHAR                             NOT NULL,
    opprettet_av  VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    endret_av     VARCHAR,
    endret_tid    TIMESTAMP(3),
    begrunnelse   VARCHAR
);

COMMENT ON TABLE verge
    IS 'Informasjon om verge';

COMMENT ON COLUMN verge.id
    IS 'Primary key';

COMMENT ON COLUMN verge.ident
    IS 'Aktørid av verge person';

COMMENT ON COLUMN verge.gyldig_fom
    IS 'Hvis fullmakt er begrenset i periode, dato for når fullmakten er gyldig fra';

COMMENT ON COLUMN verge.gyldig_tom
    IS 'Hvis fullmakt er begrenset i periode, dato for når fullmakten er gyldig til';

COMMENT ON COLUMN verge.type
    IS 'Type verge';

COMMENT ON COLUMN verge.org_nr
    IS 'Vergens organisasjonsnummer';

COMMENT ON COLUMN verge.navn
    IS 'Navn på vergen, som tastet inn av saksbehandler';

COMMENT ON COLUMN verge.kilde
    IS 'Opprinnelsen av verge.enten fpsak hvis det kopierte fra fpsak eller fptilbake';

COMMENT ON COLUMN verge.begrunnelse
    IS 'Begrunnelse for verge';

CREATE TABLE behandlingsresultat (
    id            UUID PRIMARY KEY,
    versjon       BIGINT                               NOT NULL,
    behandling_id UUID                                 NOT NULL REFERENCES behandling,
    type          VARCHAR      DEFAULT 'IKKE_FASTSATT' NOT NULL,
    opprettet_av  VARCHAR      DEFAULT 'VL'            NOT NULL,
    opprettet_tid TIMESTAMP(3) DEFAULT localtimestamp  NOT NULL,
    endret_av     VARCHAR,
    endret_tid    TIMESTAMP(3)
);

COMMENT ON TABLE behandlingsresultat
    IS 'Beregningsresultat. Knytter sammen beregning og behandling.';

COMMENT ON COLUMN behandlingsresultat.id
    IS 'Primary key';

COMMENT ON COLUMN behandlingsresultat.versjon
    IS 'Bruker for optimistisk låsing';

COMMENT ON COLUMN behandlingsresultat.behandling_id
    IS 'Fk: behandling fremmednøkkel for kobling til behandling';

COMMENT ON COLUMN behandlingsresultat.type
    IS 'Resultat av behandlingen';

CREATE INDEX ON behandlingsresultat (behandling_id);

CREATE INDEX ON behandlingsresultat (type);

CREATE TABLE behandlingsarsak (
    id                     UUID PRIMARY KEY,
    versjon                BIGINT                              NOT NULL,
    behandling_id          UUID                                NOT NULL REFERENCES behandling,
    type                   VARCHAR                             NOT NULL,
    opprettet_av           VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid          TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    endret_av              VARCHAR,
    endret_tid             TIMESTAMP(3),
    original_behandling_id UUID REFERENCES behandling
);

COMMENT ON TABLE behandlingsarsak
    IS 'Årsak for rebehandling';

COMMENT ON COLUMN behandlingsarsak.id
    IS 'Primary key';

COMMENT ON COLUMN behandlingsarsak.behandling_id
    IS 'Fk: behandling fremmednøkkel for kobling til behandling';

COMMENT ON COLUMN behandlingsarsak.type
    IS 'Fk: behandlingsårsakstype fremmednøkkel til oversikten over hvilke årsaker en behandling kan begrunnes med';

COMMENT ON COLUMN behandlingsarsak.original_behandling_id
    IS 'Fk: behandling fremmednøkkel for kobling til behandlingen denne raden i tabellen hører til';

CREATE INDEX ON behandlingsarsak (behandling_id);

CREATE INDEX ON behandlingsarsak (original_behandling_id);

CREATE TABLE aksjonspunkt (
    id                      UUID PRIMARY KEY,
    versjon                 BIGINT                              NOT NULL,
    totrinnsbehandling      BOOLEAN                             NOT NULL,
    behandlingsstegstype    VARCHAR,
    aksjonspunktsdefinisjon VARCHAR                             NOT NULL,
    status                  VARCHAR                             NOT NULL,
    tidsfrist               TIMESTAMP(3),
    ventearsak              VARCHAR      DEFAULT '-'            NOT NULL,
    behandling_id           UUID                                NOT NULL REFERENCES behandling,
    reaktiveringsstatus     VARCHAR      DEFAULT 'AKTIV'        NOT NULL,
    manuelt_opprettet       BOOLEAN      DEFAULT FALSE          NOT NULL,
    revurdering             BOOLEAN      DEFAULT FALSE          NOT NULL,
    opprettet_av            VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid           TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    endret_av               VARCHAR,
    endret_tid              TIMESTAMP(3)
);

COMMENT ON TABLE aksjonspunkt
    IS 'Aksjoner som en saksbehandler må utføre manuelt.';

COMMENT ON COLUMN aksjonspunkt.id
    IS 'Primary key';

COMMENT ON COLUMN aksjonspunkt.totrinnsbehandling
    IS 'Indikerer at aksjonspunkter krever en totrinnsbehandling';

COMMENT ON COLUMN aksjonspunkt.behandlingsstegstype
    IS 'Hvilket steg ble dette aksjonspunktet funnet i?';

COMMENT ON COLUMN aksjonspunkt.status
    IS 'Fk:Status på aksjonspunktet';

COMMENT ON COLUMN aksjonspunkt.aksjonspunktsdefinisjon
    IS 'aksjonspunktsdefinisjon enum';

COMMENT ON COLUMN aksjonspunkt.tidsfrist
    IS 'Behandling blir automatisk gjenopptatt etter dette tidspunktet';

COMMENT ON COLUMN aksjonspunkt.ventearsak
    IS 'Årsak for at behandling er satt på vent';

COMMENT ON COLUMN aksjonspunkt.behandling_id
    IS 'Fremmednøkkel for kobling til behandling';

COMMENT ON COLUMN aksjonspunkt.status
    IS 'Angir om aksjonspunktet er aktivt. Inaktive aksjonspunkter er historiske som ble kopiert når en revurdering ble opprettet. De eksisterer for å kunne vise den opprinnelige begrunnelsen, uten at saksbehandler må ta stilling til det på nytt.';

COMMENT ON COLUMN aksjonspunkt.manuelt_opprettet
    IS 'Angir om aksjonspunktet ble opprettet manuelt. Typisk skjer dette ved overstyring, og når saksbehandler manuelt reaktiverer et historisk aksjonspunkt i en revurdering. Brukes når behandlingskontroll skal rydde ved hopp.';

COMMENT ON COLUMN aksjonspunkt.revurdering
    IS 'Flagget settes på aksjonspunkter som kopieres i det en revurdering opprettes. Trengs for å kunne vurdere om aksjonspunktet er kandidat for totrinnskontroll dersom det har blitt en endring i aksjonspunktet under revurderingen.';

CREATE UNIQUE INDEX ON aksjonspunkt (behandling_id, aksjonspunktsdefinisjon);

CREATE INDEX ON aksjonspunkt (behandlingsstegstype);

CREATE INDEX ON aksjonspunkt (aksjonspunktsdefinisjon);

CREATE INDEX ON aksjonspunkt (ventearsak);

CREATE INDEX ON aksjonspunkt (status);

CREATE INDEX ON aksjonspunkt (reaktiveringsstatus);

ALTER TABLE aksjonspunkt
    ADD CONSTRAINT chk_unique_beh_ad
        UNIQUE (behandling_id, aksjonspunktsdefinisjon);

CREATE TABLE revurderingsarsak (
    id              UUID PRIMARY KEY,
    versjon         BIGINT                              NOT NULL,
    aksjonspunkt_id UUID                                NOT NULL REFERENCES aksjonspunkt,
    arsakstype      VARCHAR                             NOT NULL,
    opprettet_av    VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid   TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    endret_av       VARCHAR,
    endret_tid      TIMESTAMP(3)
);

COMMENT ON TABLE revurderingsarsak
    IS 'Årsaken til at aksjonspunkt må vurderes på nytt';

COMMENT ON COLUMN revurderingsarsak.id
    IS 'Primary key';

COMMENT ON COLUMN revurderingsarsak.aksjonspunkt_id
    IS 'Fk:Aksjonspunkt fremmednøkkel til aksjonspunktet som må vurderes på nytt';

COMMENT ON COLUMN revurderingsarsak.arsakstype
    IS 'Årsak for at aksjonspunkt må vurderes på nytt';

CREATE INDEX ON revurderingsarsak (aksjonspunkt_id);

CREATE INDEX ON revurderingsarsak (arsakstype);

CREATE TABLE behandlingsstegstilstand (
    id                     UUID PRIMARY KEY,
    versjon                BIGINT                              NOT NULL,
    behandling_id          UUID                                NOT NULL REFERENCES behandling,
    behandlingsstegstype   VARCHAR                             NOT NULL,
    behandlingsstegsstatus VARCHAR                             NOT NULL,
    opprettet_av           VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid          TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    endret_av              VARCHAR,
    endret_tid             TIMESTAMP(3)
);

COMMENT ON TABLE behandlingsstegstilstand
    IS 'Angir tilstand for behandlingsteg som kjøres';

COMMENT ON COLUMN behandlingsstegstilstand.id
    IS 'Primary key';

COMMENT ON COLUMN behandlingsstegstilstand.behandling_id
    IS 'Fk: behandling fremmednøkkel for kobling til behandlingen dette steget er tilknyttet';

COMMENT ON COLUMN behandlingsstegstilstand.behandlingsstegstype
    IS 'Hvilket behandlingsteg som kjøres';

COMMENT ON COLUMN behandlingsstegstilstand.behandlingsstegsstatus
    IS 'Status på steg: (ved) inngang, startet, venter, (ved) utgang, utført';

CREATE INDEX ON behandlingsstegstilstand (behandling_id);

CREATE INDEX ON behandlingsstegstilstand (behandlingsstegsstatus);

CREATE INDEX ON behandlingsstegstilstand (behandlingsstegstype);

CREATE TABLE behandlingsvedtak (
    id                      UUID PRIMARY KEY,
    versjon                 BIGINT                                NOT NULL,
    vedtaksdato             DATE                                  NOT NULL,
    ansvarlig_saksbehandler VARCHAR                               NOT NULL,
    behandlingsresultat_id  UUID                                  NOT NULL REFERENCES behandlingsresultat,
    iverksettingsstatus     VARCHAR      DEFAULT 'IKKE_IVERKSATT' NOT NULL,
    opprettet_av            VARCHAR      DEFAULT 'VL'             NOT NULL,
    opprettet_tid           TIMESTAMP(3) DEFAULT localtimestamp   NOT NULL,
    endret_av               VARCHAR,
    endret_tid              TIMESTAMP(3)
);

COMMENT ON TABLE behandlingsvedtak
    IS 'Vedtak koblet til en behandling via et behandlingsresultat.';

COMMENT ON COLUMN behandlingsvedtak.id
    IS 'Primary key';

COMMENT ON COLUMN behandlingsvedtak.vedtaksdato
    IS 'Vedtaksdato.';

COMMENT ON COLUMN behandlingsvedtak.ansvarlig_saksbehandler
    IS 'Ansvarlig saksbehandler som godkjente vedtaket.';

COMMENT ON COLUMN behandlingsvedtak.behandlingsresultat_id
    IS 'Fk:Behandling_resultat fremmednøkkel til tabellen som viser behandlingsresultatet';

COMMENT ON COLUMN behandlingsvedtak.iverksettingsstatus
    IS 'Status for iverksettingssteget';

CREATE UNIQUE INDEX ON behandlingsvedtak (behandlingsresultat_id);

CREATE INDEX ON behandlingsvedtak (ansvarlig_saksbehandler);

CREATE INDEX ON behandlingsvedtak (vedtaksdato);

CREATE INDEX ON behandlingsvedtak (iverksettingsstatus);

CREATE TABLE totrinnsvurdering (
    id                      UUID PRIMARY KEY,
    versjon                 BIGINT                              NOT NULL,
    behandling_id           UUID                                NOT NULL REFERENCES behandling,
    aksjonspunktsdefinisjon VARCHAR                             NOT NULL,
    aktiv                   BOOLEAN      DEFAULT TRUE           NOT NULL,
    godkjent                BOOLEAN                             NOT NULL,
    begrunnelse             VARCHAR,
    opprettet_av            VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid           TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    endret_av               VARCHAR,
    endret_tid              TIMESTAMP(3)
);

COMMENT ON TABLE totrinnsvurdering
    IS 'Statisk read only totrinnsvurdering som brukes til å vise vurderinger til aksjonspunkter uavhengig av status';

COMMENT ON COLUMN totrinnsvurdering.godkjent
    IS 'Beslutters godkjenning';

COMMENT ON COLUMN totrinnsvurdering.begrunnelse
    IS 'Beslutters begrunnelse';

CREATE INDEX ON totrinnsvurdering (aksjonspunktsdefinisjon);

CREATE INDEX ON totrinnsvurdering (behandling_id);

CREATE TABLE arsak_totrinnsvurdering (
    id                   UUID PRIMARY KEY,
    versjon              BIGINT                              NOT NULL,
    arsakstype           VARCHAR                             NOT NULL,
    totrinnsvurdering_id UUID                                NOT NULL REFERENCES totrinnsvurdering,
    opprettet_av         VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid        TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    endret_av            VARCHAR,
    endret_tid           TIMESTAMP(3)
);

COMMENT ON TABLE arsak_totrinnsvurdering
    IS 'Årsaken til at aksjonspunkt må vurderes på nytt';

COMMENT ON COLUMN arsak_totrinnsvurdering.arsakstype
    IS 'Årsak til at løsning på aksjonspunkt er underkjent';

CREATE INDEX ON arsak_totrinnsvurdering (totrinnsvurdering_id);

CREATE INDEX ON arsak_totrinnsvurdering (arsakstype);

CREATE TABLE mottakers_varselrespons (
    id                      UUID PRIMARY KEY,
    versjon                 BIGINT                    NOT NULL,
    behandling_id           UUID                      NOT NULL REFERENCES behandling,
    akseptert_faktagrunnlag BOOLEAN,
    opprettet_av            VARCHAR      DEFAULT 'VL' NOT NULL,
    opprettet_tid           TIMESTAMP(3) DEFAULT localtimestamp,
    endret_av               VARCHAR,
    endret_tid              TIMESTAMP(3),
    kilde                   VARCHAR                   NOT NULL
);

COMMENT ON TABLE mottakers_varselrespons
    IS 'Respons fra mottakere av tbk. Varsel';

COMMENT ON COLUMN mottakers_varselrespons.id
    IS 'Primary key';

COMMENT ON COLUMN mottakers_varselrespons.behandling_id
    IS 'Behandlingen responsen hører til';

COMMENT ON COLUMN mottakers_varselrespons.akseptert_faktagrunnlag
    IS 'Angir om faktagrunnlag har blitt akseptert av bruker';

COMMENT ON COLUMN mottakers_varselrespons.kilde
    IS 'Angir hvor responsen ble registrert';

CREATE UNIQUE INDEX ON mottakers_varselrespons (behandling_id);

CREATE TABLE vurdert_foreldelse (
    id            UUID PRIMARY KEY,
    versjon       BIGINT                              NOT NULL,
    behandling_id UUID                                NOT NULL,
    aktiv         BOOLEAN      DEFAULT TRUE           NOT NULL,
    opprettet_av  VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    endret_av     VARCHAR,
    endret_tid    TIMESTAMP(3)
);

COMMENT ON TABLE vurdert_foreldelse
    IS 'Aggregate tabell for å lagre vurdert foreldelse';

COMMENT ON COLUMN vurdert_foreldelse.id
    IS 'Primary key';

COMMENT ON COLUMN vurdert_foreldelse.behandling_id
    IS 'Fk: behandling fremmednøkkel for tilknyttet behandling';

COMMENT ON COLUMN vurdert_foreldelse.aktiv
    IS 'Angir status av vurdert foreldelse';

CREATE TABLE gruppering_vurdert_foreldelse (
    id                    UUID                                NOT NULL PRIMARY KEY,
    versjon               BIGINT                              NOT NULL,
    vurdert_foreldelse_id UUID                                NOT NULL
        REFERENCES vurdert_foreldelse,
    behandling_id         UUID                                NOT NULL
        REFERENCES behandling,
    aktiv                 BOOLEAN      DEFAULT TRUE           NOT NULL,
    opprettet_av          VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid         TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    endret_av             VARCHAR,
    endret_tid            TIMESTAMP(3)
)
;

COMMENT ON TABLE gruppering_vurdert_foreldelse IS 'Aggregate tabell for å lagre vurdert foreldelse'
;

COMMENT ON COLUMN gruppering_vurdert_foreldelse.id IS 'Primary Key'
;

COMMENT ON COLUMN gruppering_vurdert_foreldelse.vurdert_foreldelse_id IS 'FK:VURDERT_FORELDELSE'
;

COMMENT ON COLUMN gruppering_vurdert_foreldelse.behandling_id IS 'FK: BEHANDLING fremmednøkkel for tilknyttet behandling'
;

COMMENT ON COLUMN gruppering_vurdert_foreldelse.aktiv IS 'Angir status av vurdert foreldelse'
;

CREATE INDEX idx_gr_vurdert_foreldelse_1
    ON gruppering_vurdert_foreldelse (vurdert_foreldelse_id)
;


CREATE TABLE foreldelsesperiode (
    id                        UUID PRIMARY KEY,
    versjon                   BIGINT                              NOT NULL,
    vurdert_foreldelse_id     UUID                                NOT NULL REFERENCES vurdert_foreldelse,
    fom                       DATE                                NOT NULL,
    tom                       DATE                                NOT NULL,
    foreldelsesvurderingstype VARCHAR                             NOT NULL,
    opprettet_av              VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid             TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    endret_av                 VARCHAR,
    endret_tid                TIMESTAMP(3),
    begrunnelse               VARCHAR                             NOT NULL,
    foreldelsesfrist          DATE,
    oppdagelsesdato           DATE
);

COMMENT ON TABLE foreldelsesperiode
    IS 'Tabell for å lagre ny utbetaling periode opprettet av saksbehandler';

COMMENT ON COLUMN foreldelsesperiode.id
    IS 'Primary key';

COMMENT ON COLUMN foreldelsesperiode.vurdert_foreldelse_id
    IS 'Fk:Vurdert_foreldelse';

COMMENT ON COLUMN foreldelsesperiode.fom
    IS 'Første dag av ny utbetaling periode';

COMMENT ON COLUMN foreldelsesperiode.tom
    IS 'Siste dag av ny utbetaling periode';

COMMENT ON COLUMN foreldelsesperiode.foreldelsesvurderingstype
    IS 'Foreldelse vurdering type av en periode';

COMMENT ON COLUMN foreldelsesperiode.begrunnelse
    IS 'Begrunnelse for endre periode';

COMMENT ON COLUMN foreldelsesperiode.foreldelsesfrist
    IS 'Foreldelsesfrist for når feilutbetalingen kan innkreves';

COMMENT ON COLUMN foreldelsesperiode.oppdagelsesdato
    IS 'Dato for når feilutbetalingen ble oppdaget';

CREATE INDEX ON foreldelsesperiode (vurdert_foreldelse_id);

CREATE INDEX ON foreldelsesperiode (foreldelsesvurderingstype);

CREATE TABLE kravgrunnlag431 (
    id                      UUID PRIMARY KEY,
    versjon                 BIGINT                              NOT NULL,
    vedtak_id               VARCHAR                             NOT NULL,
    kravstatuskode          VARCHAR                             NOT NULL,
    fagomradekode           VARCHAR                             NOT NULL,
    fagsystem               VARCHAR                             NOT NULL,
    fagsystem_vedtaksdato   DATE,
    omgjort_vedtak_id       VARCHAR,
    gjelder_vedtak_id       VARCHAR                             NOT NULL,
    gjelder_type            VARCHAR                             NOT NULL,
    utbetales_til_id        VARCHAR                             NOT NULL,
    hjemmelkode             VARCHAR,
    beregnes_renter         BOOLEAN,
    ansvarlig_enhet         VARCHAR                             NOT NULL,
    bostedsenhet            VARCHAR                             NOT NULL,
    behandlingsenhet        VARCHAR                             NOT NULL,
    kontrollfelt            VARCHAR                             NOT NULL,
    saksbehandler_id        VARCHAR                             NOT NULL,
    referanse               VARCHAR,
    opprettet_av            VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid           TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    endret_av               VARCHAR,
    endret_tid              TIMESTAMP(3),
    ekstern_kravgrunnlag_id VARCHAR
);

COMMENT ON TABLE kravgrunnlag431
    IS 'Tabell for tilbakekrevingsvedtak fra økonomi';

COMMENT ON COLUMN kravgrunnlag431.vedtak_id
    IS 'Identifikasjon av tilbakekrevingsvedtaket opprettet av tilbakekrevingskomponenten';

COMMENT ON COLUMN kravgrunnlag431.kravstatuskode
    IS 'Status på kravgrunnlaget';

COMMENT ON COLUMN kravgrunnlag431.fagomradekode
    IS 'Fagområdet på feilutbetalingen';

COMMENT ON COLUMN kravgrunnlag431.fagsystem
    IS 'Fagsystemets identifikasjon av vedtaket som har feilutbetaling';

COMMENT ON COLUMN kravgrunnlag431.fagsystem_vedtaksdato
    IS 'Fagsystemets vedtaksdato for vedtaket';

COMMENT ON COLUMN kravgrunnlag431.omgjort_vedtak_id
    IS 'Henvisning til forrige gyldige vedtak';

COMMENT ON COLUMN kravgrunnlag431.gjelder_vedtak_id
    IS 'Vanligvis stønadsmottaker (fnr;org.nr.) i feilutbetalingen';

COMMENT ON COLUMN kravgrunnlag431.gjelder_type
    IS 'Angir om vedtak-gjelder-id er fnr, org.nr., tss-nr etc';

COMMENT ON COLUMN kravgrunnlag431.utbetales_til_id
    IS 'Mottaker av pengene i feilutbetalingen';

COMMENT ON COLUMN kravgrunnlag431.hjemmelkode
    IS 'Lovhjemmel for tilbakekrevingsvedtaket';

COMMENT ON COLUMN kravgrunnlag431.beregnes_renter
    IS 'J dersom det skal beregnes renter på kravet';

COMMENT ON COLUMN kravgrunnlag431.ansvarlig_enhet
    IS 'Enhet ansvarlig';

COMMENT ON COLUMN kravgrunnlag431.bostedsenhet
    IS 'Bostedsenhet, hentet fra feilutbetalingen';

COMMENT ON COLUMN kravgrunnlag431.behandlingsenhet
    IS 'Behandlende enhet, hentet fra feilutbetalingen';

COMMENT ON COLUMN kravgrunnlag431.kontrollfelt
    IS 'Brukes ved innsending av tilbakekrevingsvedtak for å kontrollere at kravgrunnlaget ikke er blitt endret i mellomtiden';

COMMENT ON COLUMN kravgrunnlag431.saksbehandler_id
    IS 'Saksbehandler';

COMMENT ON COLUMN kravgrunnlag431.referanse
    IS 'Henvisning fra nyeste oppdragslinje';

COMMENT ON COLUMN kravgrunnlag431.ekstern_kravgrunnlag_id
    IS 'Referanse til kravgrunnlag fra ostbk. Brukes ved omgjøring for å hente nytt grunnlag.';

CREATE INDEX ON kravgrunnlag431 (kravstatuskode);

CREATE INDEX ON kravgrunnlag431 (fagomradekode);

CREATE INDEX ON kravgrunnlag431 (gjelder_type);

CREATE INDEX ON kravgrunnlag431 (utbetales_til_id);

CREATE INDEX ON kravgrunnlag431 (vedtak_id);

CREATE TABLE kravgrunnlagsperiode432 (
    id                   UUID PRIMARY KEY,
    versjon              BIGINT                              NOT NULL,
    kravgrunnlag431_id   UUID                                NOT NULL REFERENCES kravgrunnlag431,
    fom                  DATE                                NOT NULL,
    tom                  DATE                                NOT NULL,
    manedlig_skattebelop NUMERIC(12, 2)                      NOT NULL,
    opprettet_av         VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid        TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    endret_av            VARCHAR,
    endret_tid           TIMESTAMP(3)
);

COMMENT ON TABLE kravgrunnlagsperiode432
    IS 'Perioder av tilbakekrevingsvedtak fra økonomi';

COMMENT ON COLUMN kravgrunnlagsperiode432.fom
    IS 'Første dag i periode';

COMMENT ON COLUMN kravgrunnlagsperiode432.tom
    IS 'Siste dag i periode';

COMMENT ON COLUMN kravgrunnlagsperiode432.kravgrunnlag431_id
    IS 'Fk:Krav_grunnlag431';

COMMENT ON COLUMN kravgrunnlagsperiode432.manedlig_skattebelop
    IS 'Angir totalt skattebeløp per måned';

CREATE INDEX ON kravgrunnlagsperiode432 (kravgrunnlag431_id);

CREATE TABLE kravgrunnlagsbelop433 (
    id                           UUID PRIMARY KEY,
    versjon                      BIGINT                              NOT NULL,
    klassekode                   VARCHAR                             NOT NULL,
    klassetype                   VARCHAR                             NOT NULL,
    opprinnelig_utbetalingsbelop NUMERIC(12, 2),
    nytt_belop                   NUMERIC(12, 2)                      NOT NULL,
    tilbakekreves_belop          NUMERIC(12, 2),
    uinnkrevd_belop              NUMERIC(12, 2),
    resultatkode                 VARCHAR,
    arsakskode                   VARCHAR,
    skyldkode                    VARCHAR,
    kravgrunnlagsperiode432_id   UUID                                NOT NULL REFERENCES kravgrunnlagsperiode432,
    skatteprosent                NUMERIC(7, 4)                       NOT NULL,
    opprettet_av                 VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid                TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    endret_av                    VARCHAR,
    endret_tid                   TIMESTAMP(3)
);

COMMENT ON TABLE kravgrunnlagsbelop433
    IS 'Tabell for tilbakekrevingsbeløp fra økonomi';

COMMENT ON COLUMN kravgrunnlagsbelop433.klassekode
    IS 'Klassifisering av stønad, skatt, trekk etc.';

COMMENT ON COLUMN kravgrunnlagsbelop433.klassetype
    IS 'Angir type av klassekoden';

COMMENT ON COLUMN kravgrunnlagsbelop433.opprinnelig_utbetalingsbelop
    IS 'Opprinnelig beregnet beløp, dvs utbetalingen som førte til feilutbetaling';

COMMENT ON COLUMN kravgrunnlagsbelop433.nytt_belop
    IS 'Beløpet som ble beregnet ved korrigeringen';

COMMENT ON COLUMN kravgrunnlagsbelop433.tilbakekreves_belop
    IS 'Beløpet som skal tilbakekreves';

COMMENT ON COLUMN kravgrunnlagsbelop433.uinnkrevd_belop
    IS 'Beløp som ikke skal tilbakekreves';

COMMENT ON COLUMN kravgrunnlagsbelop433.resultatkode
    IS 'Hvilket vedtak som er fattet ang tilbakekreving';

COMMENT ON COLUMN kravgrunnlagsbelop433.arsakskode
    IS 'Årsak til feilutbetalingen';

COMMENT ON COLUMN kravgrunnlagsbelop433.skyldkode
    IS 'Hvem som har skyld i at det ble feilutbetalt';

COMMENT ON COLUMN kravgrunnlagsbelop433.kravgrunnlagsperiode432_id
    IS 'Fk:Krav_grunnlag_periode432';

COMMENT ON COLUMN kravgrunnlagsbelop433.skatteprosent
    IS 'Angir gjeldende skatt prosent som skal trekke fra brutto tilbakekrevingsbeløp for netto tilbakekreving';

CREATE INDEX ON kravgrunnlagsbelop433 (klassekode);

CREATE INDEX ON kravgrunnlagsbelop433 (klassetype);

CREATE INDEX ON kravgrunnlagsbelop433 (kravgrunnlagsperiode432_id);

CREATE TABLE kravvedtaksstatus437 (
    id                UUID PRIMARY KEY,
    versjon           BIGINT                              NOT NULL,
    vedtak_id         VARCHAR                             NOT NULL,
    kravstatuskode    VARCHAR                             NOT NULL,
    fagomradekode     VARCHAR                             NOT NULL,
    fagsystem_id      VARCHAR                             NOT NULL,
    gjelder_vedtak_id VARCHAR                             NOT NULL,
    gjelder_type      VARCHAR                             NOT NULL,
    referanse         VARCHAR,
    opprettet_av      VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid     TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    endret_av         VARCHAR,
    endret_tid        TIMESTAMP(3)
);

COMMENT ON TABLE kravvedtaksstatus437
    IS 'Tabell for krav og vedtak status endringer fra økonomi';

COMMENT ON COLUMN kravvedtaksstatus437.vedtak_id
    IS 'Identifikasjon av tilbakekrevingsvedtaket opprettet av tilbakekrevingskomponenten';

COMMENT ON COLUMN kravvedtaksstatus437.kravstatuskode
    IS 'Status på kravgrunnlaget';

COMMENT ON COLUMN kravvedtaksstatus437.fagomradekode
    IS 'Fagområdet på feilutbetalingen';

COMMENT ON COLUMN kravvedtaksstatus437.fagsystem_id
    IS 'Fagsystemets identifikasjon av vedtaket som har feilutbetaling';

COMMENT ON COLUMN kravvedtaksstatus437.gjelder_vedtak_id
    IS 'Vanligvis stønadsmottaker (fnr;org.nr.) i feilutbetalingen';

COMMENT ON COLUMN kravvedtaksstatus437.gjelder_type
    IS 'Angir om vedtak-gjelder-id er fnr, org.nr., tss-nr etc';

COMMENT ON COLUMN kravvedtaksstatus437.referanse
    IS 'Henvisning fra nyeste oppdragslinje';

CREATE INDEX ON kravvedtaksstatus437 (kravstatuskode);

CREATE INDEX ON kravvedtaksstatus437 (fagomradekode);

CREATE INDEX ON kravvedtaksstatus437 (gjelder_type);

CREATE TABLE gruppering_krav_grunnlag (
    id                 UUID PRIMARY KEY,
    versjon            BIGINT                              NOT NULL,
    kravgrunnlag431_id UUID                                NOT NULL REFERENCES kravgrunnlag431,
    behandling_id      UUID                                NOT NULL REFERENCES behandling,
    aktiv              BOOLEAN      DEFAULT TRUE           NOT NULL,
    opprettet_av       VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid      TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    endret_av          VARCHAR,
    endret_tid         TIMESTAMP(3),
    sperret            BOOLEAN
);

COMMENT ON TABLE gruppering_krav_grunnlag
    IS 'Aggregate tabell for å lagre grunnlag';

COMMENT ON COLUMN gruppering_krav_grunnlag.id
    IS 'Primary key';

COMMENT ON COLUMN gruppering_krav_grunnlag.kravgrunnlag431_id
    IS 'Fk:Krav_grunnlag431.angir grunnlag kommer fra økonomi';

COMMENT ON COLUMN gruppering_krav_grunnlag.behandling_id
    IS 'Fk: behandling fremmednøkkel for tilknyttet behandling';

COMMENT ON COLUMN gruppering_krav_grunnlag.aktiv
    IS 'Angir status av grunnlag';

COMMENT ON COLUMN gruppering_krav_grunnlag.sperret
    IS 'Angir om grunnlaget har fått sper melding fra økonomi';

CREATE INDEX ON gruppering_krav_grunnlag (kravgrunnlag431_id);

CREATE INDEX ON gruppering_krav_grunnlag (behandling_id);

CREATE TABLE vilkarsvurdering (
    id            UUID PRIMARY KEY,
    versjon       BIGINT                              NOT NULL,
    behandling_id UUID                                NOT NULL,
    aktiv         BOOLEAN                             NOT NULL,
    opprettet_av  VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    endret_av     VARCHAR,
    endret_tid    TIMESTAMP(3)
);

COMMENT ON TABLE vilkarsvurdering
    IS 'Kobler flere perioder av vilkårsvurdering for tilbakekreving';

COMMENT ON COLUMN vilkarsvurdering.behandling_id
    IS 'Referanse til behandling';

COMMENT ON COLUMN vilkarsvurdering.aktiv
    IS 'Angir status av manuell vilkårsvurdering';

CREATE INDEX ON vilkarsvurdering (behandling_id);

CREATE TABLE vilkarsvurderingsperiode (
    id                        UUID PRIMARY KEY,
    versjon                   BIGINT                              NOT NULL,
    vilkarsvurdering_id       UUID                                NOT NULL REFERENCES vilkarsvurdering,
    fom                       DATE                                NOT NULL,
    tom                       DATE                                NOT NULL,
    navoppfulgt               VARCHAR                             NOT NULL,
    vilkarsvurderingsresultat VARCHAR                             NOT NULL,
    begrunnelse               VARCHAR                             NOT NULL,
    opprettet_av              VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid             TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    endret_av                 VARCHAR,
    endret_tid                TIMESTAMP(3)
);

COMMENT ON TABLE vilkarsvurderingsperiode
    IS 'Periode med vilkårsvurdering for tilbakekreving';

COMMENT ON COLUMN vilkarsvurderingsperiode.vilkarsvurdering_id
    IS 'Fk:vilkår';

COMMENT ON COLUMN vilkarsvurderingsperiode.fom
    IS 'Fra-og-med-dato';

COMMENT ON COLUMN vilkarsvurderingsperiode.tom
    IS 'Til-og-med-dato';

COMMENT ON COLUMN vilkarsvurderingsperiode.navoppfulgt
    IS 'Vurdering av hvordan nav har fulgt opp';

COMMENT ON COLUMN vilkarsvurderingsperiode.vilkarsvurderingsresultat
    IS 'Hovedresultat av vilkårsvurdering (kodeverk)';

COMMENT ON COLUMN vilkarsvurderingsperiode.begrunnelse
    IS 'Saksbehandlers begrunnelse';

CREATE INDEX ON vilkarsvurderingsperiode (vilkarsvurdering_id);

CREATE INDEX ON vilkarsvurderingsperiode (navoppfulgt);

CREATE INDEX ON vilkarsvurderingsperiode (vilkarsvurderingsresultat);

CREATE TABLE vilkarsvurdering_aktsomhet (
    id                            UUID PRIMARY KEY,
    versjon                       BIGINT                              NOT NULL,
    vilkarsvurderingsperiode_id   UUID                                NOT NULL REFERENCES vilkarsvurderingsperiode,
    aktsomhet                     VARCHAR                             NOT NULL,
    ilegg_renter                  BOOLEAN,
    andel_tilbakekreves           NUMERIC(5, 2),
    manuelt_satt_belop            BIGINT,
    begrunnelse                   VARCHAR                             NOT NULL,
    opprettet_av                  VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid                 TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    endret_av                     VARCHAR,
    endret_tid                    TIMESTAMP(3),
    serlige_grunner_til_reduksjon BOOLEAN,
    tilbakekrev_smabelop          BOOLEAN,
    serlige_grunner_begrunnelse   VARCHAR,
    CHECK ("andel_tilbakekreves" IS NULL OR manuelt_satt_belop IS NULL)
);

COMMENT ON TABLE vilkarsvurdering_aktsomhet
    IS 'Videre vurderinger når det er vurdert at bruker ikke mottok beløp i god tro';

COMMENT ON COLUMN vilkarsvurdering_aktsomhet.vilkarsvurderingsperiode_id
    IS 'Fk:vilkårsperiode';

COMMENT ON COLUMN vilkarsvurdering_aktsomhet.aktsomhet
    IS 'Resultat av aktsomhetsvurdering (kodeverk)';

COMMENT ON COLUMN vilkarsvurdering_aktsomhet.ilegg_renter
    IS 'Hvorvidt renter skal ilegges';

COMMENT ON COLUMN vilkarsvurdering_aktsomhet.andel_tilbakekreves
    IS 'Hvor stor del av feilutbetalt beløp som skal tilbakekreves';

COMMENT ON COLUMN vilkarsvurdering_aktsomhet.manuelt_satt_belop
    IS 'Feilutbetalt beløp som skal tilbakekreves som bestemt ved saksbehandler';

COMMENT ON COLUMN vilkarsvurdering_aktsomhet.begrunnelse
    IS 'Beskrivelse av aktsomhet';

COMMENT ON COLUMN vilkarsvurdering_aktsomhet.serlige_grunner_til_reduksjon
    IS 'Angir om særlig grunner gi reduksjon av beløpet';

COMMENT ON COLUMN vilkarsvurdering_aktsomhet.tilbakekrev_smabelop
    IS 'Angir om skal tilbakekreves når totalbeløpet er under 4 rettsgebyr';

COMMENT ON COLUMN vilkarsvurdering_aktsomhet.serlige_grunner_begrunnelse
    IS 'Beskrivelse av særlig grunner';

CREATE INDEX ON vilkarsvurdering_aktsomhet (vilkarsvurderingsperiode_id);

CREATE INDEX ON vilkarsvurdering_aktsomhet (aktsomhet);

CREATE TABLE vilkarsvurdering_serlig_grunn (
    id                            UUID PRIMARY KEY,
    versjon                       BIGINT                              NOT NULL,
    vilkarsvurdering_aktsomhet_id UUID                                NOT NULL REFERENCES vilkarsvurdering_aktsomhet,
    serlig_grunn                  VARCHAR                             NOT NULL,
    opprettet_av                  VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid                 TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    endret_av                     VARCHAR,
    endret_tid                    TIMESTAMP(3),
    begrunnelse                   VARCHAR
);

COMMENT ON TABLE vilkarsvurdering_serlig_grunn
    IS 'Særlige grunner ved vurdering';

COMMENT ON COLUMN vilkarsvurdering_serlig_grunn.vilkarsvurdering_aktsomhet_id
    IS 'Fk:vilkarsvurdering_aktsomhet';

COMMENT ON COLUMN vilkarsvurdering_serlig_grunn.serlig_grunn
    IS 'Særlig grunn (kodeverk)';

COMMENT ON COLUMN vilkarsvurdering_serlig_grunn.begrunnelse
    IS 'Beskrivelse av særlig grunn hvis grunn er annet';

CREATE INDEX ON vilkarsvurdering_serlig_grunn (vilkarsvurdering_aktsomhet_id);

CREATE INDEX ON vilkarsvurdering_serlig_grunn (serlig_grunn);

CREATE TABLE vilkarsvurdering_god_tro (
    id                          UUID PRIMARY KEY,
    versjon                     BIGINT                              NOT NULL,
    vilkarsvurderingsperiode_id UUID                                NOT NULL REFERENCES vilkarsvurderingsperiode,
    belop_er_i_behold           BOOLEAN                             NOT NULL,
    belop_tilbakekreves         BIGINT,
    begrunnelse                 VARCHAR                             NOT NULL,
    opprettet_av                VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid               TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    endret_av                   VARCHAR,
    endret_tid                  TIMESTAMP(3)
);

COMMENT ON TABLE vilkarsvurdering_god_tro
    IS 'Videre vurderinger når det er vurdert at bruker mottok feilutbetaling i god tro';

COMMENT ON COLUMN vilkarsvurdering_god_tro.vilkarsvurderingsperiode_id
    IS 'Fk:vilkarsvurderingsperiode';

COMMENT ON COLUMN vilkarsvurdering_god_tro.belop_er_i_behold
    IS 'Indikerer at beløp er i behold';

COMMENT ON COLUMN vilkarsvurdering_god_tro.belop_tilbakekreves
    IS 'Hvor mye av feilutbetalt beløp som skal tilbakekreves';

COMMENT ON COLUMN vilkarsvurdering_god_tro.begrunnelse
    IS 'Beskrivelse av god tro vilkår';

CREATE INDEX ON vilkarsvurdering_god_tro (vilkarsvurderingsperiode_id);

CREATE TABLE fakta_feilutbetaling (
    id            UUID PRIMARY KEY,
    versjon       BIGINT                              NOT NULL,
    opprettet_av  VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    endret_av     VARCHAR,
    endret_tid    TIMESTAMP(3),
    begrunnelse   VARCHAR
);

COMMENT ON TABLE fakta_feilutbetaling
    IS 'Kobler flere perioder av fakta om feilutbetaling for tilbakekreving';

COMMENT ON COLUMN fakta_feilutbetaling.begrunnelse
    IS 'Begrunnelse for endringer gjort i fakta om feilutbetaling';

CREATE TABLE fakta_feilutbetalingsperiode (
    id                      UUID PRIMARY KEY,
    versjon                 BIGINT                              NOT NULL,
    fom                     DATE                                NOT NULL,
    tom                     DATE                                NOT NULL,
    hendelsestype           VARCHAR                             NOT NULL,
    hendelsesundertype      VARCHAR                             NOT NULL,
    opprettet_av            VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid           TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    endret_av               VARCHAR,
    endret_tid              TIMESTAMP(3),
    fakta_feilutbetaling_id UUID                                NOT NULL REFERENCES fakta_feilutbetaling
);

COMMENT ON TABLE fakta_feilutbetalingsperiode
    IS 'Tabell for å lagre feilutbetaling årsak og underårsak for hver perioder';

COMMENT ON COLUMN fakta_feilutbetalingsperiode.id
    IS 'Primary key';

COMMENT ON COLUMN fakta_feilutbetalingsperiode.fom
    IS 'Første dag av feilutbetaling periode';

COMMENT ON COLUMN fakta_feilutbetalingsperiode.tom
    IS 'Siste dag av feilutbetaling periode';

COMMENT ON COLUMN fakta_feilutbetalingsperiode.hendelsestype
    IS 'Hendelse som er årsak til feilutbetalingen';

COMMENT ON COLUMN fakta_feilutbetalingsperiode.hendelsesundertype
    IS 'Hendelse som er årsak til feilutbetalingen (underårsak)';

COMMENT ON COLUMN fakta_feilutbetalingsperiode.fakta_feilutbetaling_id
    IS 'Fk:Feilutbetaling';

CREATE INDEX ON fakta_feilutbetalingsperiode (fakta_feilutbetaling_id);

CREATE INDEX ON fakta_feilutbetalingsperiode (hendelsestype);

CREATE INDEX ON fakta_feilutbetalingsperiode (hendelsesundertype);

CREATE TABLE gruppering_fakta_feilutbetaling (
    id                      UUID PRIMARY KEY,
    versjon                 BIGINT                              NOT NULL,
    behandling_id           UUID                                NOT NULL REFERENCES behandling,
    fakta_feilutbetaling_id UUID                                NOT NULL REFERENCES fakta_feilutbetaling,
    aktiv                   BOOLEAN                             NOT NULL,
    opprettet_av            VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid           TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    endret_av               VARCHAR,
    endret_tid              TIMESTAMP(3)
);

COMMENT ON TABLE gruppering_fakta_feilutbetaling
    IS 'Versjonering av fakta om feilutbetaling for tilbakekreving';

COMMENT ON COLUMN gruppering_fakta_feilutbetaling.behandling_id
    IS 'Referanse til behandling';

COMMENT ON COLUMN gruppering_fakta_feilutbetaling.fakta_feilutbetaling_id
    IS 'Fk:Feilutbetaling';

COMMENT ON COLUMN gruppering_fakta_feilutbetaling.aktiv
    IS 'Angir status av fakta om feilutbetaling';

CREATE INDEX ON gruppering_fakta_feilutbetaling (behandling_id);

CREATE INDEX ON gruppering_fakta_feilutbetaling (fakta_feilutbetaling_id);

CREATE TABLE okonomi_xml_mottatt (
    id                UUID PRIMARY KEY,
    versjon           BIGINT                              NOT NULL,
    melding           TEXT                                NOT NULL,
    sekvens           INTEGER,
    opprettet_av      VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid     TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    endret_av         VARCHAR,
    endret_tid        TIMESTAMP(3),
    tilkoblet         BOOLEAN,
    ekstern_fagsak_id VARCHAR,
    henvisning        VARCHAR
);

COMMENT ON TABLE okonomi_xml_mottatt
    IS 'Lagrer mottatt kravgrunnlag-xml i påvente av at den skal prosesseres. Brukes for at mottak skal være mer robust';

COMMENT ON COLUMN okonomi_xml_mottatt.id
    IS 'Primærnøkkel';

COMMENT ON COLUMN okonomi_xml_mottatt.melding
    IS 'Kravgrunnlag-xml';

COMMENT ON COLUMN okonomi_xml_mottatt.sekvens
    IS 'Teller innenfor en behandling';

COMMENT ON COLUMN okonomi_xml_mottatt.tilkoblet
    IS 'Angir om mottatt xml er tilkoblet med en behandling';

COMMENT ON COLUMN okonomi_xml_mottatt.ekstern_fagsak_id
    IS 'Saksnummer(som økonomi har sendt)';

COMMENT ON COLUMN okonomi_xml_mottatt.henvisning
    IS 'Henvisning;referanse. Peker på referanse-feltet i kravgrunnlaget, og kommer opprinnelig fra fagsystemet. For fptilbake er den lik fpsak.behandlingid. For k9-tilbake er den lik base64(bytes(behandlinguuid))';

CREATE INDEX ON okonomi_xml_mottatt (henvisning);

CREATE INDEX ON okonomi_xml_mottatt (ekstern_fagsak_id);

CREATE INDEX ON okonomi_xml_mottatt (opprettet_tid);

CREATE INDEX ON okonomi_xml_mottatt (tilkoblet);

CREATE TABLE totrinnsresultatsgrunnlag (
    id                                 UUID PRIMARY KEY,
    versjon                            BIGINT                              NOT NULL,
    behandling_id                      UUID                                NOT NULL REFERENCES behandling,
    gruppering_fakta_feilutbetaling_id UUID                                NOT NULL REFERENCES gruppering_fakta_feilutbetaling,
    gruppering_vurdert_foreldelse_id   UUID REFERENCES gruppering_vurdert_foreldelse,
    vilkarsvurdering_id                UUID REFERENCES vilkarsvurdering,
    aktiv                              BOOLEAN                             NOT NULL,
    opprettet_av                       VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid                      TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    endret_av                          VARCHAR,
    endret_tid                         TIMESTAMP(3)
);

COMMENT ON TABLE totrinnsresultatsgrunnlag
    IS 'Tabell som held grunnlagsid for data vist i panelet fra beslutter.';

COMMENT ON COLUMN totrinnsresultatsgrunnlag.id
    IS 'Pk';

COMMENT ON COLUMN totrinnsresultatsgrunnlag.behandling_id
    IS 'Fk til behandling som hører til totrinnsresultatet';

COMMENT ON COLUMN totrinnsresultatsgrunnlag.gruppering_fakta_feilutbetaling_id
    IS 'Fk til aktivt feilutbetalingaggregate ved totrinnsbehandlingen';

COMMENT ON COLUMN totrinnsresultatsgrunnlag.gruppering_vurdert_foreldelse_id
    IS 'Fk til aktivt vurdertforeldelseaggregate ved totrinnsbehandlingen';

COMMENT ON COLUMN totrinnsresultatsgrunnlag.vilkarsvurdering_id
    IS 'Fk til aktivt vilkårvurderingaggregate ved totrinnsbehandlingen';

CREATE INDEX ON totrinnsresultatsgrunnlag (behandling_id);

CREATE INDEX ON totrinnsresultatsgrunnlag (gruppering_fakta_feilutbetaling_id);

CREATE INDEX ON totrinnsresultatsgrunnlag (gruppering_vurdert_foreldelse_id);

CREATE INDEX ON totrinnsresultatsgrunnlag (vilkarsvurdering_id);

CREATE TABLE vedtaksbrevsoppsummering (
    id                    UUID PRIMARY KEY,
    versjon               BIGINT                              NOT NULL,
    behandling_id         UUID                                NOT NULL REFERENCES behandling,
    oppsummering_fritekst VARCHAR,
    opprettet_tid         TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    opprettet_av          VARCHAR      DEFAULT 'VL'           NOT NULL,
    endret_av             VARCHAR,
    endret_tid            TIMESTAMP(3),
    fritekst              TEXT
);

COMMENT ON TABLE vedtaksbrevsoppsummering
    IS 'Inneholder friteksten til vedtaksoppsummeringen som er skrevet inn av saksbehandler.';

COMMENT ON COLUMN vedtaksbrevsoppsummering.id
    IS 'Primary key';

COMMENT ON COLUMN vedtaksbrevsoppsummering.behandling_id
    IS 'Fk: behandling fremmednøkkel for kobling til behandling i fptilbake';

COMMENT ON COLUMN vedtaksbrevsoppsummering.oppsummering_fritekst
    IS 'Fritekst fra saksbehandler til oppsummering av vedtaket';

COMMENT ON COLUMN vedtaksbrevsoppsummering.fritekst
    IS 'Fritekst fra saksbehandler til oppsummering av vedtaket';

CREATE INDEX ON vedtaksbrevsoppsummering (behandling_id);

CREATE TABLE vedtaksbrevsperiode (
    id            UUID PRIMARY KEY,
    versjon       BIGINT                              NOT NULL,
    behandling_id UUID                                NOT NULL REFERENCES behandling,
    fom           DATE                                NOT NULL,
    tom           DATE                                NOT NULL,
    fritekst      VARCHAR                             NOT NULL,
    fritekststype VARCHAR                             NOT NULL,
    opprettet_tid TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    opprettet_av  VARCHAR      DEFAULT 'VL'           NOT NULL,
    endret_av     VARCHAR,
    endret_tid    TIMESTAMP(3)
);

COMMENT ON TABLE vedtaksbrevsperiode
    IS 'Inneholder en periode i et vedtaksbrev, samt fritekst';

COMMENT ON COLUMN vedtaksbrevsperiode.id
    IS 'Primary key';

COMMENT ON COLUMN vedtaksbrevsperiode.behandling_id
    IS 'Fk: behandling fremmednøkkel for kobling til behandling i fptilbake';

COMMENT ON COLUMN vedtaksbrevsperiode.fom
    IS 'Fom-dato for perioden';

COMMENT ON COLUMN vedtaksbrevsperiode.tom
    IS 'Tom-dato for perioden';

COMMENT ON COLUMN vedtaksbrevsperiode.fritekst
    IS 'Fritekst skrevet til et av avsnittene i vedtaksbrevet';

COMMENT ON COLUMN vedtaksbrevsperiode.fritekststype
    IS 'Hvilket avsnitt friteksten gjelder';

CREATE INDEX ON vedtaksbrevsperiode (behandling_id);

CREATE TABLE okonomi_xml_sendt (
    id            UUID PRIMARY KEY NOT NULL,
    versjon       BIGINT           NOT NULL,
    behandling_id UUID             NOT NULL REFERENCES behandling,
    melding       TEXT             NOT NULL,
    kvittering    TEXT,
    opprettet_av  VARCHAR      DEFAULT 'VL',
    opprettet_tid TIMESTAMP(3) DEFAULT localtimestamp,
    endret_av     VARCHAR,
    endret_tid    TIMESTAMP(3),
    meldingstype  VARCHAR          NOT NULL
);

COMMENT ON TABLE okonomi_xml_sendt
    IS 'Tabell som tar vare på xml sendt til os, brukes for feilsøking';

COMMENT ON COLUMN okonomi_xml_sendt.id
    IS 'Primary key';

COMMENT ON COLUMN okonomi_xml_sendt.behandling_id
    IS 'Behandlingen det gjelder';

COMMENT ON COLUMN okonomi_xml_sendt.melding
    IS 'Xml sendt til os';

COMMENT ON COLUMN okonomi_xml_sendt.kvittering
    IS 'Respons fra os';

COMMENT ON COLUMN okonomi_xml_sendt.meldingstype
    IS 'Meldingstype';

CREATE UNIQUE INDEX ON okonomi_xml_sendt (id);

CREATE INDEX ON okonomi_xml_sendt (behandling_id);

CREATE INDEX ON okonomi_xml_sendt (meldingstype);


CREATE TABLE gruppering_kravvedtaksstatus (
    id                      UUID PRIMARY KEY,
    versjon                 BIGINT                              NOT NULL,
    kravvedtaksstatus437_id UUID                                NOT NULL REFERENCES kravvedtaksstatus437,
    behandling_id           UUID                                NOT NULL REFERENCES behandling,
    aktiv                   BOOLEAN                             NOT NULL,
    opprettet_av            VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid           TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    endret_av               VARCHAR,
    endret_tid              TIMESTAMP(3)
);

COMMENT ON TABLE gruppering_kravvedtaksstatus
    IS 'Aggregate tabell for å lagre krav- og vedtakstatus';

COMMENT ON COLUMN gruppering_kravvedtaksstatus.id
    IS 'Primary key';

COMMENT ON COLUMN gruppering_kravvedtaksstatus.kravvedtaksstatus437_id
    IS 'Fk:Krav_vedtak_status437.angir krav- og vedtakstatus kommer fra økonomi';

COMMENT ON COLUMN gruppering_kravvedtaksstatus.behandling_id
    IS 'Fk: behandling fremmednøkkel for tilknyttet behandling';

COMMENT ON COLUMN gruppering_kravvedtaksstatus.aktiv
    IS 'Angir status av grunnlag';

CREATE INDEX ON gruppering_kravvedtaksstatus (kravvedtaksstatus437_id);

CREATE INDEX ON gruppering_kravvedtaksstatus (behandling_id);

CREATE TABLE brevsporing (
    id             UUID PRIMARY KEY,
    versjon        BIGINT                              NOT NULL,
    behandling_id  UUID                                NOT NULL REFERENCES behandling,
    journalpost_id VARCHAR                             NOT NULL,
    dokument_id    VARCHAR                             NOT NULL,
    brevtype       VARCHAR                             NOT NULL,
    opprettet_av   VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid  TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    endret_av      VARCHAR,
    endret_tid     TIMESTAMP(3)
);

COMMENT ON TABLE brevsporing
    IS 'Brevsporing inneholder informasjon om forskjellige brev som er bestilt.';

COMMENT ON COLUMN brevsporing.id
    IS 'Primary key';

COMMENT ON COLUMN brevsporing.behandling_id
    IS 'Fk: behandling fremmednøkkel for kobling til behandling i fptilbake';

COMMENT ON COLUMN brevsporing.journalpost_id
    IS 'Journalpostid i doksys';

COMMENT ON COLUMN brevsporing.dokument_id
    IS 'Dokumentid i doksys';

COMMENT ON COLUMN brevsporing.brevtype
    IS 'Bestilt brevtype';

CREATE INDEX ON brevsporing (behandling_id);

CREATE INDEX ON brevsporing (brevtype);

CREATE TABLE okonomi_xml_mottatt_arkiv (
    id            UUID PRIMARY KEY,
    versjon       BIGINT                              NOT NULL,
    melding       TEXT                                NOT NULL,
    opprettet_av  VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    endret_av     VARCHAR,
    endret_tid    TIMESTAMP(3)
);

COMMENT ON TABLE okonomi_xml_mottatt_arkiv
    IS 'Tabell for å arkivere gamle kravgrunnlag som ikke finnes i økonomi.';

COMMENT ON COLUMN okonomi_xml_mottatt_arkiv.id
    IS 'Primary key';

COMMENT ON COLUMN okonomi_xml_mottatt_arkiv.melding
    IS 'Gammel kravgrunnlag xml';
