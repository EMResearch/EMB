package no.nav.tag.tiltaksgjennomforing.avtale;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import no.nav.tag.tiltaksgjennomforing.autorisasjon.InnloggetBruker;
import no.nav.tag.tiltaksgjennomforing.enhet.*;
import no.nav.tag.tiltaksgjennomforing.exceptions.*;
import no.nav.tag.tiltaksgjennomforing.persondata.PdlRespons;
import no.nav.tag.tiltaksgjennomforing.persondata.PersondataService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static java.util.Map.entry;

@AllArgsConstructor
@Slf4j
@Data
public abstract class Avtalepart<T extends Identifikator> {
    private final T identifikator;
    static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd. MMMM yyyy");

    public boolean harTilgang(Avtale avtale) {
        if (avtale.isSlettemerket()) {
            return false;
        }
        return harTilgangTilAvtale(avtale);
    }

    abstract boolean harTilgangTilAvtale(Avtale avtale);

    abstract Page<Avtale> hentAlleAvtalerMedMuligTilgang(AvtaleRepository avtaleRepository, AvtalePredicate queryParametre, Pageable pageable);

    public Map<String, Object> hentAlleAvtalerMedLesetilgang(AvtaleRepository avtaleRepository, AvtalePredicate queryParametre, String sorteringskolonne, Pageable pageable) {
        Page<Avtale> avtaler = hentAlleAvtalerMedMuligTilgang(avtaleRepository, queryParametre, pageable);

        List<Avtale> avtalerMedTilgang = avtaler.getContent().stream()
                .filter(avtale -> !avtale.isFeilregistrert())
                .filter(this::harTilgang)
                .toList();

        List<AvtaleMinimalListevisning> listMinimal = avtalerMedTilgang.stream().map(AvtaleMinimalListevisning::fromAvtale).toList();

        return Map.ofEntries(
                entry("avtaler", listMinimal),
                entry("size", avtaler.getSize()),
                entry("currentPage", avtaler.getNumber()),
                entry("totalItems", avtaler.getTotalElements()),
                entry("totalPages", avtaler.getTotalPages())
        );
    }

    public Avtale hentAvtale(AvtaleRepository avtaleRepository, UUID avtaleId) {
        Avtale avtale = avtaleRepository.findById(avtaleId)
                .orElseThrow(RessursFinnesIkkeException::new);
        sjekkTilgang(avtale);
        return avtale;
    }

    public Avtale hentAvtaleFraAvtaleNr(AvtaleRepository avtaleRepository, int avtaleNr) {
        Avtale avtale = avtaleRepository.findByAvtaleNr(avtaleNr).orElseThrow(RessursFinnesIkkeException::new);
        sjekkTilgang(avtale);
        return avtale;
    }

    public List<AvtaleInnhold> hentAvtaleVersjoner(AvtaleRepository avtaleRepository, AvtaleInnholdRepository avtaleInnholdRepository, UUID avtaleId) {
        Avtale avtale = avtaleRepository.findById(avtaleId)
                .orElseThrow(RessursFinnesIkkeException::new);
        sjekkTilgang(avtale);
        return avtaleInnholdRepository.findAllByAvtale(avtale);
    }

    abstract void godkjennForAvtalepart(Avtale avtale);

    abstract boolean kanEndreAvtale();

    public abstract boolean erGodkjentAvInnloggetBruker(Avtale avtale);

    abstract boolean kanOppheveGodkjenninger(Avtale avtale);

    abstract void opphevGodkjenningerSomAvtalepart(Avtale avtale);

    public void godkjennAvtale(Instant sistEndret, Avtale avtale) {
        sjekkTilgang(avtale);
        avtale.sjekkSistEndret(sistEndret);
        godkjennForAvtalepart(avtale);
    }

    public void sjekkTilgang(Avtale avtale) {
        if (!harTilgang(avtale)) {
            throw new TilgangskontrollException("Ikke tilgang til avtale");
        }
    }

    protected void avtalePartKanEndreAvtale() {
        if (!kanEndreAvtale()) {
            throw new KanIkkeEndreException();
        }
    }

    public void endreAvtale(
            Instant sistEndret,
            EndreAvtale endreAvtale,
            Avtale avtale,
            EnumSet<Tiltakstype> tiltakstyperMedTilskuddsperioder
    ) {
        sjekkTilgang(avtale);
        if (!kanEndreAvtale()) {
            throw new KanIkkeEndreException();
        }
        avvisDatoerTilbakeITid(avtale, endreAvtale.getStartDato(), endreAvtale.getSluttDato());
        avtale.endreAvtale(sistEndret, endreAvtale, rolle(), tiltakstyperMedTilskuddsperioder, identifikator);
    }

    protected void sjekkTilgangOgEndreAvtale(
            Instant sistEndret,
            EndreAvtale endreAvtale,
            Avtale avtale,
            EnumSet<Tiltakstype> tiltakstyperMedTilskuddsperioder
    ) {
        sjekkTilgang(avtale);
        avtalePartKanEndreAvtale();
        avvisDatoerTilbakeITid(avtale, endreAvtale.getStartDato(), endreAvtale.getSluttDato());
        avtale.endreAvtale(
                sistEndret,
                endreAvtale,
                rolle(),
                tiltakstyperMedTilskuddsperioder,
                identifikator
        );
    }

    protected void avvisDatoerTilbakeITid(Avtale avtale, LocalDate startDato, LocalDate sluttDato) {
    }

    protected abstract Avtalerolle rolle();

    public void opphevGodkjenninger(Avtale avtale) {
        if (!kanOppheveGodkjenninger(avtale)) {
            throw new KanIkkeOppheveException();
        }
        boolean AlleParterHarIkkeGodkjentAvtale = !avtale.erGodkjentAvVeileder() &&
                !avtale.erGodkjentAvArbeidsgiver() &&
                !avtale.erGodkjentAvDeltaker();

        if (AlleParterHarIkkeGodkjentAvtale) {
            throw new KanIkkeOppheveException();
        }
        if (avtale.erAvtaleInngått()) {
            throw new FeilkodeException(Feilkode.KAN_IKKE_OPPHEVE_GODKJENNINGER_VED_INNGAATT_AVTALE);
        }
        opphevGodkjenningerSomAvtalepart(avtale);
    }

    public abstract InnloggetBruker innloggetBruker();

    public Collection<? extends Identifikator> identifikatorer() {
        return List.of(getIdentifikator());
    }

    protected void hentGeoEnhetFraNorg2(Avtale avtale, PdlRespons pdlRespons, Norg2Client norg2Client) {
        Norg2GeoResponse enhet = PersondataService.hentGeoLokasjonFraPdlRespons(pdlRespons)
                .map(norg2Client::hentGeografiskEnhet).orElse(null);
        if (enhet == null) return;
        avtale.setEnhetGeografisk(enhet.getEnhetNr());
        avtale.setEnhetsnavnGeografisk(enhet.getNavn());
    }

    protected void hentOppfølingenhetNavnFraNorg2(Avtale avtale, Norg2Client norg2Client) {
        if (avtale.getEnhetOppfolging() == null) return;
        if (avtale.getEnhetOppfolging().equals(avtale.getEnhetGeografisk())) {
            avtale.setEnhetsnavnOppfolging(avtale.getEnhetsnavnGeografisk());
        } else {
            final Norg2OppfølgingResponse response = norg2Client.hentOppfølgingsEnhetsnavn(avtale.getEnhetOppfolging());
            if (response == null) return;
            avtale.setEnhetsnavnOppfolging(response.getNavn());
        }
    }

    public void settLonntilskuddProsentsats(Avtale avtale) {
        if (avtale.getTiltakstype() == Tiltakstype.MIDLERTIDIG_LONNSTILSKUDD) {
            avtale.getGjeldendeInnhold().setLonnstilskuddProsent(
                    avtale.getKvalifiseringsgruppe().finnLonntilskuddProsentsatsUtifraKvalifiseringsgruppe(
                            40,
                            60
                    )
            );
        }
    }
}
