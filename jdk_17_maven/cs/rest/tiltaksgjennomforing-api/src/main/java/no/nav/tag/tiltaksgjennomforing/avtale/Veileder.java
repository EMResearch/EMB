package no.nav.tag.tiltaksgjennomforing.avtale;

import lombok.extern.slf4j.Slf4j;
import no.nav.tag.tiltaksgjennomforing.autorisasjon.InnloggetBruker;
import no.nav.tag.tiltaksgjennomforing.autorisasjon.InnloggetVeileder;
import no.nav.tag.tiltaksgjennomforing.autorisasjon.SlettemerkeProperties;
import no.nav.tag.tiltaksgjennomforing.autorisasjon.abac.TilgangskontrollService;
import no.nav.tag.tiltaksgjennomforing.enhet.*;
import no.nav.tag.tiltaksgjennomforing.exceptions.*;
import no.nav.tag.tiltaksgjennomforing.featuretoggles.enhet.NavEnhet;
import no.nav.tag.tiltaksgjennomforing.persondata.PdlRespons;
import no.nav.tag.tiltaksgjennomforing.persondata.PersondataService;
import org.springframework.data.domain.*;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static no.nav.tag.tiltaksgjennomforing.persondata.PersondataService.hentNavnFraPdlRespons;

@Slf4j
public class Veileder extends Avtalepart<NavIdent> implements InternBruker {
    private final TilgangskontrollService tilgangskontrollService;

    private final PersondataService persondataService;
    private final SlettemerkeProperties slettemerkeProperties;
    private final boolean harAdGruppeForBeslutter;
    private final Norg2Client norg2Client;
    private final Set<NavEnhet> navEnheter;
    private final VeilarbArenaClient veilarbArenaClient;
    private final UUID azureOid;

    public Veileder(
            NavIdent identifikator,
            UUID azureOid,
            TilgangskontrollService tilgangskontrollService,
            PersondataService persondataService,
            Norg2Client norg2Client,
            Set<NavEnhet> navEnheter,
            SlettemerkeProperties slettemerkeProperties,
            boolean harAdGruppeForBeslutter,
            VeilarbArenaClient veilarbArenaClient
    ) {

        super(identifikator);
        this.azureOid = azureOid;
        this.tilgangskontrollService = tilgangskontrollService;
        this.persondataService = persondataService;
        this.norg2Client = norg2Client;
        this.navEnheter = navEnheter;
        this.slettemerkeProperties = slettemerkeProperties;
        this.harAdGruppeForBeslutter = harAdGruppeForBeslutter;
        this.veilarbArenaClient = veilarbArenaClient;
    }

    @Deprecated
    public Veileder(
            NavIdent identifikator,
            TilgangskontrollService tilgangskontrollService,
            PersondataService persondataService,
            Norg2Client norg2Client,
            Set<NavEnhet> navEnheter,
            SlettemerkeProperties slettemerkeProperties,
            boolean harAdGruppeForBeslutter,
            VeilarbArenaClient veilarbArenaClient
    ) {
        this(identifikator, null, tilgangskontrollService, persondataService, norg2Client, navEnheter, slettemerkeProperties, harAdGruppeForBeslutter, veilarbArenaClient);
    }

    @Override
    public boolean harTilgangTilAvtale(Avtale avtale) {
        boolean harTilgang = tilgangskontrollService.harSkrivetilgangTilKandidat(this, avtale.getDeltakerFnr());
        if(!harTilgang) {
            log.info("Har ikke tilgang til avtale {}", avtale.getAvtaleNr());
        }
        return harTilgang;
    }

    @Override
    Page<Avtale> hentAlleAvtalerMedMuligTilgang(AvtaleRepository avtaleRepository, AvtalePredicate queryParametre, Pageable pageable) {
        NavIdent veilederNavIdent = queryParametre.getVeilederNavIdent() != null ? queryParametre.getVeilederNavIdent() : getIdentifikator();

        if(queryParametre.getStatus() != null) {
            // Har filtrert på status. Da går siste filtrering på status i java-kode
            Pageable allPages = PageRequest.of(0, Integer.MAX_VALUE, pageable.getSort());
            Page<Avtale> avtalerUtenStatusFiltrering = null;
            if(queryParametre.getTiltakstype() != null) {
                // Filtrer på tiltakstype
                if (queryParametre.getVeilederNavIdent() != null) {
                    avtalerUtenStatusFiltrering = avtaleRepository.findAllByVeilederNavIdentAndTiltakstype(queryParametre.getVeilederNavIdent(), queryParametre.getTiltakstype(), allPages);
                } else if (queryParametre.getDeltakerFnr() != null) {
                    avtalerUtenStatusFiltrering = avtaleRepository.findAllByDeltakerFnrAndTiltakstype(queryParametre.getDeltakerFnr(), queryParametre.getTiltakstype(), allPages);
                } else if (queryParametre.getBedriftNr() != null) {
                    avtalerUtenStatusFiltrering = avtaleRepository.findAllByBedriftNrAndTiltakstype(queryParametre.getBedriftNr(), queryParametre.getTiltakstype(), allPages);
                } else if (queryParametre.getNavEnhet() != null && queryParametre.getErUfordelt() != null && queryParametre.getErUfordelt()) {
                    avtalerUtenStatusFiltrering = avtaleRepository.findAllByVeilederNavIdentIsNullAndEnhetGeografiskAndTiltakstypeOrVeilederNavIdentIsNullAndEnhetOppfolgingAndTiltakstype(queryParametre.getNavEnhet(), queryParametre.getTiltakstype(), queryParametre.getNavEnhet(), queryParametre.getTiltakstype(), allPages);
                } else if (queryParametre.getNavEnhet() != null) {
                    avtalerUtenStatusFiltrering = avtaleRepository.findAllByEnhetGeografiskAndTiltakstypeOrEnhetOppfolgingAndTiltakstype(queryParametre.getNavEnhet(), queryParametre.getTiltakstype(), queryParametre.getNavEnhet(), queryParametre.getTiltakstype(), allPages);
                } else if (queryParametre.getAvtaleNr() != null) {
                    avtalerUtenStatusFiltrering = avtaleRepository.findAllByAvtaleNrAndTiltakstype(queryParametre.getAvtaleNr(), queryParametre.getTiltakstype(), allPages);
                } else {
                    avtalerUtenStatusFiltrering = avtaleRepository.findAllByVeilederNavIdentAndTiltakstype(veilederNavIdent, queryParametre.getTiltakstype(), allPages);
                }
            } else {
                if (queryParametre.getVeilederNavIdent() != null) {
                    avtalerUtenStatusFiltrering = avtaleRepository.findAllByVeilederNavIdent(queryParametre.getVeilederNavIdent(), allPages);
                } else if (queryParametre.getDeltakerFnr() != null) {
                    avtalerUtenStatusFiltrering = avtaleRepository.findAllByDeltakerFnr(queryParametre.getDeltakerFnr(), allPages);
                } else if (queryParametre.getBedriftNr() != null) {
                    avtalerUtenStatusFiltrering = avtaleRepository.findAllByBedriftNr(queryParametre.getBedriftNr(), allPages);
                } else if (queryParametre.getNavEnhet() != null && queryParametre.getErUfordelt() != null && queryParametre.getErUfordelt()) {
                    avtalerUtenStatusFiltrering = avtaleRepository.findAllByVeilederNavIdentIsNullAndEnhetGeografiskOrVeilederNavIdentIsNullAndEnhetOppfolging(queryParametre.getNavEnhet(), queryParametre.getNavEnhet(), allPages);
                } else if (queryParametre.getNavEnhet() != null) {
                    avtalerUtenStatusFiltrering = avtaleRepository.findAllByEnhetGeografiskOrEnhetOppfolging(queryParametre.getNavEnhet(), queryParametre.getNavEnhet(), allPages);
                } else if (queryParametre.getAvtaleNr() != null) {
                    avtalerUtenStatusFiltrering = avtaleRepository.findAllByAvtaleNr(queryParametre.getAvtaleNr(), allPages);
                } else {
                    avtalerUtenStatusFiltrering = avtaleRepository.findAllByVeilederNavIdent(veilederNavIdent, allPages);
                }
            }
            int skip = pageable.getPageNumber() > 0 ? (pageable.getPageNumber())*pageable.getPageSize() : 0;
            List<Avtale> totaltFørPaging = avtalerUtenStatusFiltrering.getContent().stream()
                    .filter(avtale -> avtale.statusSomEnum() == queryParametre.getStatus()).toList();
            List<Avtale> avtaler = avtalerUtenStatusFiltrering.getContent().stream()
                    .filter(avtale -> avtale.statusSomEnum() == queryParametre.getStatus())
                    .skip(skip)
                    .limit(pageable.getPageSize()).toList();
            return new PageImpl<>(avtaler, pageable, totaltFørPaging.size());
        }
        else {
            // Har ikke filtrert på status. Da går all filtrering i JPA/database
            // Om det er filter med tiltaksType
            if(queryParametre.getTiltakstype() != null) {
                if (queryParametre.getVeilederNavIdent() != null) {
                    return avtaleRepository.findAllByVeilederNavIdentAndTiltakstype(queryParametre.getVeilederNavIdent(), queryParametre.getTiltakstype(), pageable);
                } else if (queryParametre.getDeltakerFnr() != null) {
                    return avtaleRepository.findAllByDeltakerFnrAndTiltakstype(queryParametre.getDeltakerFnr(), queryParametre.getTiltakstype(), pageable);
                } else if (queryParametre.getBedriftNr() != null) {
                    return avtaleRepository.findAllByBedriftNrAndTiltakstype(queryParametre.getBedriftNr(), queryParametre.getTiltakstype(), pageable);
                } else if (queryParametre.getNavEnhet() != null && queryParametre.getErUfordelt() != null && queryParametre.getErUfordelt()) {
                    return avtaleRepository.findAllByVeilederNavIdentIsNullAndEnhetGeografiskAndTiltakstypeOrVeilederNavIdentIsNullAndEnhetOppfolgingAndTiltakstype(queryParametre.getNavEnhet(), queryParametre.getTiltakstype(), queryParametre.getNavEnhet(), queryParametre.getTiltakstype(), pageable);
                } else if (queryParametre.getNavEnhet() != null) {
                    return avtaleRepository.findAllByEnhetGeografiskAndTiltakstypeOrEnhetOppfolgingAndTiltakstype(queryParametre.getNavEnhet(), queryParametre.getTiltakstype(), queryParametre.getNavEnhet(), queryParametre.getTiltakstype(), pageable);
                } else if (queryParametre.getAvtaleNr() != null) {
                    return avtaleRepository.findAllByAvtaleNrAndTiltakstype(queryParametre.getAvtaleNr(), queryParametre.getTiltakstype(), pageable);
                } else {
                    return avtaleRepository.findAllByVeilederNavIdentAndTiltakstype(veilederNavIdent, queryParametre.getTiltakstype(), pageable);
                }
            } else {
                // Ingen tiltakstyper i query
                if (queryParametre.getVeilederNavIdent() != null) {
                    return avtaleRepository.findAllByVeilederNavIdent(queryParametre.getVeilederNavIdent(), pageable);
                } else if (queryParametre.getDeltakerFnr() != null) {
                    return avtaleRepository.findAllByDeltakerFnr(queryParametre.getDeltakerFnr(), pageable);
                } else if (queryParametre.getBedriftNr() != null) {
                    return avtaleRepository.findAllByBedriftNr(queryParametre.getBedriftNr(), pageable);
                } else if (queryParametre.getNavEnhet() != null && queryParametre.getErUfordelt() != null && queryParametre.getErUfordelt()) {
                    return avtaleRepository.findAllByVeilederNavIdentIsNullAndEnhetGeografiskOrVeilederNavIdentIsNullAndEnhetOppfolging(queryParametre.getNavEnhet(), queryParametre.getNavEnhet(), pageable);
                } else if (queryParametre.getNavEnhet() != null) {
                    return avtaleRepository.findAllByEnhetGeografiskOrEnhetOppfolging(queryParametre.getNavEnhet(), queryParametre.getNavEnhet(), pageable);
                } else if (queryParametre.getAvtaleNr() != null) {
                    return avtaleRepository.findAllByAvtaleNr(queryParametre.getAvtaleNr(), pageable);
                } else {
                    return avtaleRepository.findAllByVeilederNavIdent(veilederNavIdent, pageable);
                }
            }
        }
    }

    public void annullerAvtale(Instant sistEndret, String annullerGrunn, Avtale avtale) {
        avtale.sjekkSistEndret(sistEndret);
        avtale.annuller(this, annullerGrunn);
    }

    @Override
    public boolean kanEndreAvtale() {
        return true;
    }

    @Override
    void godkjennForAvtalepart(Avtale avtale) {
        if (persondataService.erKode6(avtale.getDeltakerFnr())) {
            throw new KanIkkeGodkjenneAvtalePåKode6Exception();
        }
        if (avtale.getTiltakstype() != Tiltakstype.SOMMERJOBB) {
            veilarbArenaClient.sjekkOppfølingStatus(avtale);
        }
        avtale.godkjennForVeileder(getIdentifikator());
    }

    @Override
    public boolean erGodkjentAvInnloggetBruker(Avtale avtale) {
        return avtale.erGodkjentAvVeileder();
    }

    @Override
    boolean kanOppheveGodkjenninger(Avtale avtale) {
        return true;
    }

    public void godkjennForVeilederOgDeltaker(GodkjentPaVegneGrunn paVegneAvGrunn, Avtale avtale) {
        sjekkTilgang(avtale);
        if (persondataService.erKode6(avtale.getDeltakerFnr())) {
            throw new KanIkkeGodkjenneAvtalePåKode6Exception();
        }
        if (avtale.getTiltakstype() != Tiltakstype.SOMMERJOBB) {
            veilarbArenaClient.sjekkOppfølingStatus(avtale);
        }
        avtale.godkjennForVeilederOgDeltaker(getIdentifikator(), paVegneAvGrunn);
    }

    private void blokkereKode6Prosessering(Fnr deltakerFnr) {
        if (persondataService.erKode6(deltakerFnr)) {
            throw new KanIkkeGodkjenneAvtalePåKode6Exception();
        }
    }

    private void sjekkOppfølgingStatusForTiltak(Avtale avtale) {
        if (avtale.getTiltakstype() != Tiltakstype.SOMMERJOBB) {
            veilarbArenaClient.sjekkOppfølingStatus(avtale);
        }
    }

    public void godkjennForVeilederOgArbeidsgiver(
            GodkjentPaVegneAvArbeidsgiverGrunn paVegneAvArbeidsgiverGrunn,
            Avtale avtale
    ) {
        super.sjekkTilgang(avtale);
        this.blokkereKode6Prosessering(avtale.getDeltakerFnr());
        this.sjekkOppfølgingStatusForTiltak(avtale);
        avtale.godkjennForVeilederOgArbeidsgiver(getIdentifikator(), paVegneAvArbeidsgiverGrunn);
    }

    public void godkjennForVeilederOgDeltakerOgArbeidsgiver(
            GodkjentPaVegneAvDeltakerOgArbeidsgiverGrunn paVegneAvDeltakerOgArbeidsgiverGrunn,
            Avtale avtale
    ) {
        super.sjekkTilgang(avtale);
        this.blokkereKode6Prosessering(avtale.getDeltakerFnr());
        this.sjekkOppfølgingStatusForTiltak(avtale);
        avtale.godkjennForVeilederOgDeltakerOgArbeidsgiver(getIdentifikator(), paVegneAvDeltakerOgArbeidsgiverGrunn);
    }

    @Override
    void opphevGodkjenningerSomAvtalepart(Avtale avtale) {
        avtale.opphevGodkjenningerSomVeileder();
    }

    @Override
    protected Avtalerolle rolle() {
        return Avtalerolle.VEILEDER;
    }

    @Override
    public InnloggetBruker innloggetBruker() {
        return new InnloggetVeileder(getIdentifikator(), navEnheter, harAdGruppeForBeslutter);
    }

    public void delAvtaleMedAvtalepart(Avtalerolle avtalerolle, Avtale avtale) {
        avtale.delMedAvtalepart(avtalerolle);
    }

    public void overtaAvtale(Avtale avtale) {
        super.sjekkTilgang(avtale);
        if (this.getIdentifikator().equals(avtale.getVeilederNavIdent())) {
            throw new ErAlleredeVeilederException();
        }
        avtale.overtaAvtale(this.getIdentifikator());
    }

    @Override
    public void endreAvtale(
            Instant sistEndret,
            EndreAvtale endreAvtale,
            Avtale avtale,
            EnumSet<Tiltakstype> tiltakstyperMedTilskuddsperioder
    ) {
        super.sjekkTilgangOgEndreAvtale(
                sistEndret,
                endreAvtale,
                avtale,
                tiltakstyperMedTilskuddsperioder
        );
        this.oppdatereEnheterVedEndreAvtale(avtale);
    }

    protected void oppdatereEnheterVedEndreAvtale(Avtale avtale) {
        PdlRespons pdlRespons = this.oppdaterePersondataFraPdlVedEndreAvtale(avtale.getDeltakerFnr());
        this.oppdatereOppfølgingStatusVedEndreAvtale(avtale);
        this.oppdatereGeoEnhetVedEndreAvtale(avtale, pdlRespons);
        this.oppdatereOppfølgingEnhetsnavnVedEndreAvtale(avtale);

    }

    public PdlRespons oppdaterePersondataFraPdlVedEndreAvtale(Fnr deltakerFnr) {
        final PdlRespons persondata = persondataService.hentPersondataFraPdl(deltakerFnr);
        this.sjekkKode6(persondata);
        return persondata;
    }

    private void oppdatereGeoEnhetVedEndreAvtale(Avtale avtale, PdlRespons pdlRespons) {
        Norg2GeoResponse norg2GeoResponse = PersondataService.hentGeoLokasjonFraPdlRespons(pdlRespons)
                .map(norg2Client::hentGeoEnhetFraCacheEllerNorg2)
                .orElse(null);
        if (norg2GeoResponse == null) return;
        avtale.setEnhetGeografisk(norg2GeoResponse.getEnhetNr());
        avtale.setEnhetsnavnGeografisk(norg2GeoResponse.getNavn());
    }

    private void oppdatereOppfølgingEnhetsnavnVedEndreAvtale(Avtale avtale) {
        final Norg2OppfølgingResponse response = norg2Client.hentOppfølgingsEnhetsnavnFraCacheNorg2(
                avtale.getEnhetOppfolging()
        );
        if (response == null) return;
        avtale.setEnhetsnavnOppfolging(response.getNavn());
    }

    public void oppdatereOppfølgingStatusVedEndreAvtale(Avtale avtale) {
        Oppfølgingsstatus oppfølgingsstatus = veilarbArenaClient.HentOppfølgingsenhetFraCacheEllerArena(
                avtale.getDeltakerFnr().asString()
        );
        if (oppfølgingsstatus == null) return;
        this.settOppfølgingsStatus(avtale, oppfølgingsstatus);
    }

    private void sjekkKode6(PdlRespons persondata) {
        if (persondataService.erKode6(persondata)) {
            throw new KanIkkeOppretteAvtalePåKode6Exception();
        }
    }

    public Avtale opprettAvtale(OpprettAvtale opprettAvtale) {
        this.sjekkTilgangskontroll(opprettAvtale.getDeltakerFnr());
        Avtale avtale = Avtale.veilederOppretterAvtale(opprettAvtale, getIdentifikator());
        leggTilEnheter(avtale);
        return avtale;
    }

    public Avtale opprettMentorAvtale(OpprettMentorAvtale opprettMentorAvtale) {
        this.sjekkTilgangskontroll(opprettMentorAvtale.getDeltakerFnr());
        Avtale avtale = Avtale.veilederOppretterAvtale(opprettMentorAvtale, getIdentifikator());
        leggTilEnheter(avtale);
        return avtale;
    }

    private void sjekkTilgangskontroll(Fnr deltakerFnr) {
        if(!tilgangskontrollService.harSkrivetilgangTilKandidat(this, deltakerFnr)) {
            throw new IkkeTilgangTilDeltakerException();
        }
    }

    protected void leggTilEnheter(Avtale avtale){
        final PdlRespons persondata = this.hentPersonDataForOpprettelseAvAvtale(avtale);
        this.hentOppfølgingFraArena(avtale, veilarbArenaClient);
        super.hentGeoEnhetFraNorg2(avtale, persondata, norg2Client);
        this.hentOppfolgingEnhetsnavnFraNorg2(avtale, norg2Client);
    }

    private PdlRespons hentPersonDataForOpprettelseAvAvtale(Avtale avtale) {
        final PdlRespons persondata = hentPersondata(avtale.getDeltakerFnr());
        avtale.leggTilDeltakerNavn(hentNavnFraPdlRespons(persondata));
        return persondata;
    }

    public void hentOppfolgingEnhetsnavnFraNorg2(Avtale avtale, Norg2Client norg2Client) {
        final Norg2OppfølgingResponse response = norg2Client.hentOppfølgingsEnhetsnavn(avtale.getEnhetOppfolging());
        if (response == null) return;
        avtale.setEnhetsnavnOppfolging(response.getNavn());
    }

    public void hentOppfølgingFraArena(
            Avtale avtale,
            VeilarbArenaClient veilarbArenaClient
    ) {
        if(avtale.harOppfølgingsStatus()) return;
        Oppfølgingsstatus oppfølgingsstatus = veilarbArenaClient.sjekkOgHentOppfølgingStatus(avtale);
        if (oppfølgingsstatus == null) return;
        this.settOppfølgingsStatus(avtale, oppfølgingsstatus);
        this.settLonntilskuddProsentsats(avtale);
    }

    private PdlRespons hentPersondata(Fnr deltakerFnr) {
        final PdlRespons persondata = persondataService.hentPersondata(deltakerFnr);
        this.sjekkKode6(persondata);
        return persondata;
    }

    public void sjekkOgHentOppfølgingStatus(Avtale avtale, VeilarbArenaClient veilarbArenaClient) {
        Oppfølgingsstatus oppfølgingsstatus = veilarbArenaClient.sjekkOgHentOppfølgingStatus(avtale);
        this.settOppfølgingsStatus(avtale, oppfølgingsstatus);
    }

    protected void settOppfølgingsStatus(Avtale avtale, Oppfølgingsstatus oppfølgingsstatus) {
        avtale.setEnhetOppfolging(oppfølgingsstatus.getOppfolgingsenhet());
        avtale.setKvalifiseringsgruppe(oppfølgingsstatus.getKvalifiseringsgruppe());
        avtale.setFormidlingsgruppe(oppfølgingsstatus.getFormidlingsgruppe());
    }

    public void slettemerk(Avtale avtale) {
        super.sjekkTilgang(avtale);
        List<NavIdent> identer = slettemerkeProperties.getIdent();
        if (!identer.contains(this.getIdentifikator())) {
            throw new IkkeAdminTilgangException();
        }
        avtale.slettemerk(this.getIdentifikator());
    }

    public void endreMål(EndreMål endreMål, Avtale avtale) {
        super.sjekkTilgang(avtale);
        avtale.endreMål(endreMål, getIdentifikator());
    }

    public void endreInkluderingstilskudd(EndreInkluderingstilskudd endreInkluderingstilskudd, Avtale avtale) {
        super.sjekkTilgang(avtale);
        avtale.endreInkluderingstilskudd(endreInkluderingstilskudd, getIdentifikator());
    }

    public void forkortAvtale(Avtale avtale, LocalDate sluttDato, String grunn, String annetGrunn) {
        super.sjekkTilgang(avtale);
        avtale.forkortAvtale(sluttDato, grunn, annetGrunn, getIdentifikator());
    }

    public void forlengAvtale(LocalDate sluttDato, Avtale avtale) {
        super.sjekkTilgang(avtale);
        sjekkOgHentOppfølgingStatus(avtale, veilarbArenaClient);
        avtale.forlengAvtale(sluttDato, getIdentifikator());
    }

    protected void oppdatereEnheterEtterForespørsel(Avtale avtale) {
        final PdlRespons persondata = this.hentPersonDataForOpprettelseAvAvtale(avtale);
        this.sjekkOgHentOppfølgingStatus(avtale, veilarbArenaClient);
        super.hentGeoEnhetFraNorg2(avtale, persondata, norg2Client);
        this.hentOppfolgingEnhetsnavnFraNorg2(avtale, norg2Client);
    }

    public void endreStillingbeskrivelse(EndreStillingsbeskrivelse endreStillingsbeskrivelse, Avtale avtale) {
        super.sjekkTilgang(avtale);
        avtale.endreStillingsbeskrivelse(endreStillingsbeskrivelse, getIdentifikator());
    }

    public void endreOppfølgingOgTilrettelegging(
            EndreOppfølgingOgTilrettelegging endreOppfølgingOgTilrettelegging,
            Avtale avtale
    ) {
        super.sjekkTilgang(avtale);
        avtale.endreOppfølgingOgTilrettelegging(endreOppfølgingOgTilrettelegging, getIdentifikator());
    }

    public void endreOmMentor(EndreOmMentor endreOmMentor, Avtale avtale) {
        super.sjekkTilgang(avtale);
        avtale.endreOmMentor(endreOmMentor, getIdentifikator());
    }

    public void endreKontaktinfo(EndreKontaktInformasjon endreKontaktInformasjon, Avtale avtale) {
        super.sjekkTilgang(avtale);
        avtale.endreKontaktInformasjon(endreKontaktInformasjon, getIdentifikator());
    }

    public void endreTilskuddsberegning(EndreTilskuddsberegning endreTilskuddsberegning, Avtale avtale) {
        super.sjekkTilgang(avtale);
        avtale.endreTilskuddsberegning(endreTilskuddsberegning, getIdentifikator());
    }

    public void sendTilbakeTilBeslutter(Avtale avtale) {
        super.sjekkTilgang(avtale);
        avtale.sendTilbakeTilBeslutter();
    }

    protected void oppdatereKostnadssted(Avtale avtale, Norg2Client norg2Client, String enhet) {
        final Norg2OppfølgingResponse response = norg2Client.hentOppfølgingsEnhetsnavn(enhet);

        if (response == null) {
            throw new FeilkodeException(Feilkode.ENHET_FINNES_IKKE);
        }
        NyttKostnadssted nyttKostnadssted = new NyttKostnadssted(enhet, response.getNavn());
        TreeSet<TilskuddPeriode> tilskuddPerioder = avtale.finnTilskuddsperioderIkkeLukketForEndring();

        if (tilskuddPerioder == null) {
            throw new FeilkodeException(Feilkode.TILSKUDDSPERIODE_ER_IKKE_SATT);
        }
        avtale.oppdatereKostnadsstedForTilskuddsperioder(nyttKostnadssted);
    }

    private LocalDate settStartDato(LocalDate startdato) {
        return startdato != null ? startdato : LocalDate.now();
    }

    protected List<AlleredeRegistrertAvtale> hentAvtaleDeltakerAlleredeErRegistrertPaa(
            Fnr deltakerFnr,
            Tiltakstype tiltakstype,
            UUID avtaleId,
            LocalDate startDato,
            LocalDate sluttDato,
            AvtaleRepository avtaleRepository
    ) {
        if(avtaleId != null && startDato != null && sluttDato != null) {
            return AlleredeRegistrertAvtale.filtrerAvtaleDeltakerAlleredeErRegistrertPaa(
                    avtaleRepository.finnAvtalerSomOverlapperForDeltakerVedGodkjenningAvAvtale(
                            deltakerFnr.asString(),
                            avtaleId.toString(),
                            Date.valueOf(settStartDato(startDato)),
                            Date.valueOf(sluttDato)
                    ),
                    tiltakstype
            );
        }
        return AlleredeRegistrertAvtale.filtrerAvtaleDeltakerAlleredeErRegistrertPaa(
                avtaleRepository.finnAvtalerSomOverlapperForDeltakerVedOpprettelseAvAvtale(
                        deltakerFnr.asString(),
                        Date.valueOf(settStartDato(startDato))

                ),
                tiltakstype
        );
    }

    @Override public UUID getAzureOid() {
        return azureOid;
    }

    @Override public NavIdent getNavIdent() {
        return getIdentifikator();
    }
}
