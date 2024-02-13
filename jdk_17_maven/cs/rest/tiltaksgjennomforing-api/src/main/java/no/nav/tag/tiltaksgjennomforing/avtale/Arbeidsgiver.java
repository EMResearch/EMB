package no.nav.tag.tiltaksgjennomforing.avtale;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import no.nav.arbeidsgiver.altinnrettigheter.proxy.klient.model.AltinnReportee;
import no.nav.tag.tiltaksgjennomforing.autorisasjon.InnloggetArbeidsgiver;
import no.nav.tag.tiltaksgjennomforing.autorisasjon.InnloggetBruker;
import no.nav.tag.tiltaksgjennomforing.enhet.Norg2Client;

import no.nav.tag.tiltaksgjennomforing.exceptions.TilgangskontrollException;
import no.nav.tag.tiltaksgjennomforing.exceptions.VarighetDatoErTilbakeITidException;
import no.nav.tag.tiltaksgjennomforing.persondata.PdlRespons;
import no.nav.tag.tiltaksgjennomforing.persondata.PersondataService;
import no.nav.tag.tiltaksgjennomforing.utils.Now;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import static no.nav.tag.tiltaksgjennomforing.persondata.PersondataService.hentNavnFraPdlRespons;

public class Arbeidsgiver extends Avtalepart<Fnr> {
    private final Map<BedriftNr, Collection<Tiltakstype>> tilganger;
    private final Set<AltinnReportee> altinnOrganisasjoner;
    private final PersondataService persondataService;
    private final Norg2Client norg2Client;

    public Arbeidsgiver(
            Fnr identifikator,
            Set<AltinnReportee> altinnOrganisasjoner,
            Map<BedriftNr, Collection<Tiltakstype>> tilganger,
            PersondataService persondataService,
            Norg2Client norg2Client
    ) {
        super(identifikator);
        this.altinnOrganisasjoner = altinnOrganisasjoner;
        this.tilganger = tilganger;
        this.persondataService = persondataService;
        this.norg2Client = norg2Client;
    }

    private static boolean avbruttForMerEnn12UkerSiden(Avtale avtale) {
        return avtale.isAvbrutt() && avtale.getSistEndret()
                .plus(84, ChronoUnit.DAYS)
                .isBefore(Now.instant());
    }

    private static boolean annullertForMerEnn12UkerSiden(Avtale avtale) {
        return avtale.getAnnullertTidspunkt() != null && avtale.getAnnullertTidspunkt()
                .plus(84, ChronoUnit.DAYS)
                .isBefore(Now.instant());
    }

    private static boolean sluttdatoPassertMedMerEnn12Uker(Avtale avtale) {
        return avtale.erGodkjentAvVeileder() && avtale.getGjeldendeInnhold()
                .getSluttDato().plusWeeks(12)
                .isBefore(Now.localDate());
    }

    private static Avtale fjernAvbruttGrunn(Avtale avtale) {
        avtale.setAvbruttGrunn(null);
        return avtale;
    }

    private static Avtale fjernAnnullertGrunn(Avtale avtale) {
        avtale.setAnnullertGrunn(null);
        return avtale;
    }

    private static Avtale fjernKvalifiseringsgruppe(Avtale avtale) {
        avtale.setKvalifiseringsgruppe(null);
        avtale.setFormidlingsgruppe(null);
        return avtale;
    }

    @Override
    protected void avvisDatoerTilbakeITid(Avtale avtale, LocalDate startDato, LocalDate sluttDato) {
        if (!avtale.erUfordelt()) {
            return;
        }
        if (startDato != null && startDato.isBefore(Now.localDate())) {
            throw new VarighetDatoErTilbakeITidException();
        }
        if (sluttDato != null && sluttDato.isBefore(Now.localDate())) {
            throw new VarighetDatoErTilbakeITidException();
        }
    }

    @Override
    void godkjennForAvtalepart(Avtale avtale) {
        avtale.godkjennForArbeidsgiver(getIdentifikator());
    }

    @Override
    public boolean kanEndreAvtale() {
        return true;
    }

    @Override
    public boolean erGodkjentAvInnloggetBruker(Avtale avtale) {
        return avtale.erGodkjentAvArbeidsgiver();
    }

    @Override
    boolean kanOppheveGodkjenninger(Avtale avtale) {
        return !avtale.erGodkjentAvVeileder();
    }

    @Override
    void opphevGodkjenningerSomAvtalepart(Avtale avtale) {
        avtale.opphevGodkjenningerSomArbeidsgiver();
    }

    @Override
    protected Avtalerolle rolle() {
        return Avtalerolle.ARBEIDSGIVER;
    }

    @Override
    public InnloggetBruker innloggetBruker() {
        return new InnloggetArbeidsgiver(getIdentifikator(), altinnOrganisasjoner, tilganger);
    }

    @Override
    public Collection<BedriftNr> identifikatorer() {
        return tilganger.keySet();
    }

    @Override
    public boolean harTilgangTilAvtale(Avtale avtale) {
        if (sluttdatoPassertMedMerEnn12Uker(avtale)) {
            return false;
        }
        if (avbruttForMerEnn12UkerSiden(avtale)) {
            return false;
        }
        if (annullertForMerEnn12UkerSiden(avtale)) {
            return false;
        }
        return harTilgangPåTiltakIBedrift(avtale.getBedriftNr(), avtale.getTiltakstype());
    }

    private boolean harTilgangPåTiltakIBedrift(BedriftNr bedriftNr, Tiltakstype tiltakstype) {
        if (!tilganger.containsKey(bedriftNr)) {
            return false;
        }
        Collection<Tiltakstype> gyldigeTilgangerPåBedriftNr = tilganger.get(bedriftNr);
        return gyldigeTilgangerPåBedriftNr.contains(tiltakstype);
    }

    @Override
    Page<Avtale> hentAlleAvtalerMedMuligTilgang(AvtaleRepository avtaleRepository, AvtalePredicate queryParametre, Pageable pageable) {
        if (tilganger.isEmpty()) {
            return Page.empty();
        }
        Page<Avtale> avtaler;
        if (queryParametre.getTiltakstype() != null) {
            if (harTilgangPåTiltakIBedrift(queryParametre.getBedriftNr(), queryParametre.getTiltakstype()))
                avtaler = avtaleRepository.findAllByBedriftNrInAndTiltakstype(Set.of(queryParametre.getBedriftNr()), queryParametre.getTiltakstype(), pageable);
            else if (queryParametre.getBedriftNr() == null) {
                avtaler = avtaleRepository.findAllByBedriftNrInAndTiltakstype(tilganger.keySet(), queryParametre.getTiltakstype(), pageable);
            } else { // Bruker ba om informasjon på en bedrift hen ikke har tilgang til, og får dermed tom liste
                avtaler = Page.empty();
            }
        } else {
            if (queryParametre.getBedriftNr() != null && tilganger.containsKey(queryParametre.getBedriftNr()))
                avtaler = avtaleRepository.findAllByBedriftNrIn(Set.of(queryParametre.getBedriftNr()), pageable);
            else if (queryParametre.getBedriftNr() == null) {
                avtaler = avtaleRepository.findAllByBedriftNrIn(tilganger.keySet(), pageable);
            } else { // Bruker ba om informasjon på en bedrift hen ikke har tilgang til, og får dermed tom liste
                avtaler = Page.empty();
            }
        }
        return avtaler
                .map(Arbeidsgiver::fjernAvbruttGrunn)
                .map(Arbeidsgiver::fjernAnnullertGrunn);
    }

    public List<Avtale> hentAvtalerForMinsideArbeidsgiver(AvtaleRepository avtaleRepository, BedriftNr bedriftNr) {
        return avtaleRepository.findAllByBedriftNr(bedriftNr).stream()
                .filter(this::harTilgang)
                .map(Arbeidsgiver::fjernAvbruttGrunn)
                .map(Arbeidsgiver::fjernAnnullertGrunn)
                .collect(Collectors.toList());
    }

    private void tilgangTilBedriftVedOpprettelseAvAvtale(BedriftNr bedriftNr, Tiltakstype tiltakstype) {
        if (!harTilgangPåTiltakIBedrift(bedriftNr, tiltakstype)) {
            throw new TilgangskontrollException("Har ikke tilgang på tiltak i valgt bedrift");
        }
    }

    public Avtale opprettAvtale(OpprettAvtale opprettAvtale) {
        this.tilgangTilBedriftVedOpprettelseAvAvtale(opprettAvtale.getBedriftNr(), opprettAvtale.getTiltakstype());
        Avtale avtale = Avtale.arbeidsgiverOppretterAvtale(opprettAvtale);
        leggEnheterVedOpprettelseAvAvtale(avtale);

        return avtale;
    }

    public Avtale opprettMentorAvtale(OpprettMentorAvtale opprettMentorAvtale) {
        this.tilgangTilBedriftVedOpprettelseAvAvtale(
                opprettMentorAvtale.getBedriftNr(),
                opprettMentorAvtale.getTiltakstype()
        );
        Avtale avtale = Avtale.arbeidsgiverOppretterAvtale(opprettMentorAvtale);
        leggEnheterVedOpprettelseAvAvtale(avtale);

        return avtale;
    }

    protected void leggEnheterVedOpprettelseAvAvtale(Avtale avtale) {
        final PdlRespons persondata = this.hentPersonDataForOpprettelseAvAvtale(avtale);
        super.hentGeoEnhetFraNorg2(avtale, persondata, norg2Client);
        super.hentOppfølingenhetNavnFraNorg2(avtale, norg2Client);
    }

    private PdlRespons hentPersonDataForOpprettelseAvAvtale(Avtale avtale) {
        final PdlRespons persondata = persondataService.hentPersondata(avtale.getDeltakerFnr());
        avtale.leggTilDeltakerNavn(hentNavnFraPdlRespons(persondata));
        return persondata;
    }

    @Override
    public Avtale hentAvtale(AvtaleRepository avtaleRepository, UUID avtaleId) {
        Avtale avtale = super.hentAvtale(avtaleRepository, avtaleId);
        fjernAvbruttGrunn(avtale);
        fjernAnnullertGrunn(avtale);
        fjernKvalifiseringsgruppe(avtale);
        return avtale;
    }


}
