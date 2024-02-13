package no.nav.tag.tiltaksgjennomforing.avtale;

import lombok.extern.slf4j.Slf4j;
import no.nav.tag.tiltaksgjennomforing.autorisasjon.InnloggetBeslutter;
import no.nav.tag.tiltaksgjennomforing.autorisasjon.InnloggetBruker;
import no.nav.tag.tiltaksgjennomforing.autorisasjon.abac.TilgangskontrollService;
import no.nav.tag.tiltaksgjennomforing.enhet.Norg2Client;
import no.nav.tag.tiltaksgjennomforing.enhet.Norg2OppfølgingResponse;
import no.nav.tag.tiltaksgjennomforing.exceptions.Feilkode;
import no.nav.tag.tiltaksgjennomforing.exceptions.FeilkodeException;
import no.nav.tag.tiltaksgjennomforing.exceptions.NavEnhetIkkeFunnetException;
import no.nav.tag.tiltaksgjennomforing.exceptions.TilgangskontrollException;
import no.nav.tag.tiltaksgjennomforing.featuretoggles.enhet.NavEnhet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
public class Beslutter extends Avtalepart<NavIdent> implements InternBruker {
    private Norg2Client norg2Client;
    private TilgangskontrollService tilgangskontrollService;

    private UUID azureOid;

    private Set<NavEnhet> navEnheter;

    public Beslutter(NavIdent identifikator, UUID azureOid, Set<NavEnhet> navEnheter, TilgangskontrollService tilgangskontrollService, Norg2Client norg2Client) {
        super(identifikator);
        this.azureOid = azureOid;
        this.navEnheter = navEnheter;
        this.tilgangskontrollService = tilgangskontrollService;
        this.norg2Client = norg2Client;
    }

    public void godkjennTilskuddsperiode(Avtale avtale, String enhet) {
        sjekkTilgang(avtale);
        final Norg2OppfølgingResponse response = norg2Client.hentOppfølgingsEnhetsnavn(enhet);

        if (response == null) {
            throw new FeilkodeException(Feilkode.ENHET_FINNES_IKKE);
        }
        avtale.godkjennTilskuddsperiode(getIdentifikator(), enhet);
    }

    public void avslåTilskuddsperiode(Avtale avtale, EnumSet<Avslagsårsak> avslagsårsaker, String avslagsforklaring) {
        sjekkTilgang(avtale);
        avtale.avslåTilskuddsperiode(getIdentifikator(), avslagsårsaker, avslagsforklaring);
    }

    public void setOmAvtalenKanEtterregistreres(Avtale avtale) {
        sjekkTilgang(avtale);
        avtale.togglegodkjennEtterregistrering(getIdentifikator());
    }

    @Override
    public boolean harTilgangTilAvtale(Avtale avtale) {
        return tilgangskontrollService.harSkrivetilgangTilKandidat(this, avtale.getDeltakerFnr());
    }

    public boolean harTilgangTilFnr(Fnr fnr) {
        return tilgangskontrollService.harSkrivetilgangTilKandidat(this, fnr);
    }

    @Override
    Page<Avtale> hentAlleAvtalerMedMuligTilgang(AvtaleRepository avtaleRepository, AvtalePredicate queryParametre, Pageable pageable) {
        return avtaleRepository.findAllByAvtaleNr(queryParametre.getAvtaleNr(), pageable);
    }

    private Integer getPlussdato() {
        return ((int) ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.now().plusMonths(3)));
    }

    Page<BeslutterOversiktDTO> finnGodkjenteAvtalerMedTilskuddsperiodestatusOgNavEnheterListe(
            AvtaleRepository avtaleRepository,
            AvtalePredicate queryParametre,
            String sorteringskolonne,
            Integer page,
            Integer size,
            String sorteringOrder
    ) {
        Sort by = Sort.by(AvtaleSorterer.getSortingOrderForPageable(sorteringskolonne, sorteringOrder));
        Pageable paging = PageRequest.of(page, size, by);

        Set<String> navEnheter = hentNavEnheter();

        if (navEnheter.isEmpty()) {
            throw new NavEnhetIkkeFunnetException();
        }

        TilskuddPeriodeStatus status = queryParametre.getTilskuddPeriodeStatus();
        Tiltakstype tiltakstype = queryParametre.getTiltakstype();
        BedriftNr bedriftNr = queryParametre.getBedriftNr();
        Integer avtaleNr = queryParametre.getAvtaleNr();
        String filtrertNavEnhet = queryParametre.getNavEnhet();
        Integer plussDato = getPlussdato();
        LocalDate decisiondate = LocalDate.now().plusDays(plussDato);

        if (status == null) {
            status = TilskuddPeriodeStatus.UBEHANDLET;
        }

        Set<Tiltakstype> tiltakstyper = new HashSet<>();
        if (tiltakstype != null) {
            tiltakstyper.add(tiltakstype);
        } else {
            tiltakstyper.add(Tiltakstype.SOMMERJOBB);
            tiltakstyper.add(Tiltakstype.VARIG_LONNSTILSKUDD);
            tiltakstyper.add(Tiltakstype.MIDLERTIDIG_LONNSTILSKUDD);
        }

        return avtaleRepository.finnGodkjenteAvtalerMedTilskuddsperiodestatusOgNavEnheter(
                status,
                decisiondate,
                tiltakstyper,
                filtrertNavEnhet != null ? Set.of(filtrertNavEnhet) : navEnheter,
                bedriftNr != null ? bedriftNr.asString() : null,
                avtaleNr,
                paging
        );
    }

    private Set<String> hentNavEnheter() {
        return this.navEnheter.stream().map(NavEnhet::getVerdi).collect(Collectors.toSet());
    }

    @Override
    void godkjennForAvtalepart(Avtale avtale) {
        throw new TilgangskontrollException("Beslutter kan ikke godkjenne avtaler");
    }

    @Override
    public boolean kanEndreAvtale() {
        return false;
    }

    @Override
    public boolean erGodkjentAvInnloggetBruker(Avtale avtale) {
        return false;
    }

    @Override
    boolean kanOppheveGodkjenninger(Avtale avtale) {
        return false;
    }

    @Override
    void opphevGodkjenningerSomAvtalepart(Avtale avtale) {
        throw new TilgangskontrollException("Beslutter kan ikke oppheve godkjenninger av avtaler");
    }

    @Override
    protected Avtalerolle rolle() {
        return Avtalerolle.BESLUTTER;
    }

    @Override
    public InnloggetBruker innloggetBruker() {
        return new InnloggetBeslutter(getIdentifikator(), navEnheter);
    }

    @Override
    public UUID getAzureOid() {
        return azureOid;
    }

    @Override
    public NavIdent getNavIdent() {
        return getIdentifikator();
    }
}
