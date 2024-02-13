package no.nav.tag.tiltaksgjennomforing.avtale;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.security.token.support.core.api.ProtectedWithClaims;
import no.nav.tag.tiltaksgjennomforing.autorisasjon.TokenUtils;
import no.nav.tag.tiltaksgjennomforing.autorisasjon.UtviklerTilgangProperties;
import no.nav.tag.tiltaksgjennomforing.exceptions.RessursFinnesIkkeException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@ProtectedWithClaims(issuer = "aad")
@RestController
@RequestMapping("/utvikler-admin/")
@Slf4j
@RequiredArgsConstructor
public class AdminController {
    private final AvtaleRepository avtaleRepository;
    private final TilskuddPeriodeRepository tilskuddPeriodeRepository;
    private final UtviklerTilgangProperties utviklerTilgangProperties;
    private final TokenUtils tokenUtils;

    private void sjekkTilgang() {
        if (!tokenUtils.harAdGruppe(utviklerTilgangProperties.getGruppeTilgang())) {
            throw new HttpClientErrorException(HttpStatus.FORBIDDEN);
        }
    }

    @PostMapping("reberegn")
    public void reberegnLønnstilskudd(@RequestBody List<UUID> avtaleIder) {
        sjekkTilgang();
        for (UUID avtaleId : avtaleIder) {
            Avtale avtale = avtaleRepository.findById(avtaleId).orElseThrow();
            avtale.reberegnLønnstilskudd();
            avtaleRepository.save(avtale);
        }
    }

    @PostMapping("/reberegn-mangler-dato-for-redusert-prosent/{migreringsDato}")
    @Transactional
    public void reberegnVarigLønnstilskuddSomIkkeHarRedusertDato(@PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate migreringsDato) {
        sjekkTilgang();
        log.info("Starter jobb for å fikse manglende redusert prosent og redusert sum");
        // 1. Generer dato for redusert prosent og sumRedusert
        List<Avtale> varigeLønnstilskudd = avtaleRepository.findAllByTiltakstypeAndGjeldendeInnhold_DatoForRedusertProsentNullAndGjeldendeInnhold_AvtaleInngåttNotNull(Tiltakstype.VARIG_LONNSTILSKUDD);
        log.info("Fant {} varige lønnstilskudd avtaler som mangler redusert prosent til fiksing.", varigeLønnstilskudd.size());
        AtomicInteger antallUnder67 = new AtomicInteger();
        varigeLønnstilskudd.forEach(avtale -> {
            LocalDate startDato = avtale.getGjeldendeInnhold().getStartDato();
            LocalDate sluttDato = avtale.getGjeldendeInnhold().getSluttDato();
            if (avtale.getGjeldendeInnhold().getLonnstilskuddProsent() > 67
                    && startDato.isBefore(sluttDato.minusMonths(12))
                    && avtale.getAnnullertTidspunkt() == null
                    && avtale.getAvbruttGrunn() == null
                    && avtale.getGjeldendeInnhold().getSumLonnstilskudd() != null) {

                avtale.reUtregnRedusert();
                avtale.nyeTilskuddsperioderEtterMigreringFraArena(migreringsDato, false);
                avtaleRepository.save(avtale);
                antallUnder67.getAndIncrement();
            }
        });
        log.info("Ferdig kjørt reberegning av fiks for manglende redusert prosent og redusert sum på {} avtaler", antallUnder67);
    }

    @PostMapping("/reberegn-mangler-dato-for-redusert-prosent-dry-run/{migreringsDato}")
    public void reberegnVarigLønnstilskuddSomIkkeHarRedusertDatoDryRun(@PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate migreringsDato) {
        sjekkTilgang();
        log.info("DRY-RUN: Starter DRY-RUN jobb for å fikse manglende redusert prosent og redusert sum");
        // 1. Generer dato for redusert prosent og sumRedusert
        List<Avtale> varigeLønnstilskudd = avtaleRepository.findAllByTiltakstypeAndGjeldendeInnhold_DatoForRedusertProsentNullAndGjeldendeInnhold_AvtaleInngåttNotNull(Tiltakstype.VARIG_LONNSTILSKUDD);
        log.info("DRY-RUN: Fant {} varige lønnstilskudd avtaler som mangler redusert prosent til fiksing.", varigeLønnstilskudd.size());
        AtomicInteger antallUnder67 = new AtomicInteger();
        varigeLønnstilskudd.forEach(avtale -> {
            LocalDate startDato = avtale.getGjeldendeInnhold().getStartDato();
            LocalDate sluttDato = avtale.getGjeldendeInnhold().getSluttDato();

            if (avtale.getGjeldendeInnhold().getLonnstilskuddProsent() > 67
                    && startDato.isBefore(sluttDato.minusMonths(12))
                    && avtale.getAnnullertTidspunkt() == null
                    && avtale.getAvbruttGrunn() == null
                    && avtale.getGjeldendeInnhold().getSumLonnstilskudd() != null) {
                antallUnder67.getAndIncrement();
            }
        });
        log.info("DRY-RUN: Fant {} avtaler som vil bli kjørt fiksing av redusert sum og sats på", antallUnder67.get());
    }

    @PostMapping("/annuller-tilskuddsperiode/{tilskuddsperiodeId}")
    @Transactional
    public void annullerTilskuddsperiode(@PathVariable("tilskuddsperiodeId") UUID id) {
        sjekkTilgang();
        log.info("Annullerer tilskuddsperiode {}", id);
        TilskuddPeriode tilskuddPeriode = tilskuddPeriodeRepository.findById(id).orElseThrow(RessursFinnesIkkeException::new);
        Avtale avtale = tilskuddPeriode.getAvtale();
        avtale.annullerTilskuddsperiode(tilskuddPeriode);
        tilskuddPeriodeRepository.save(tilskuddPeriode);
        avtaleRepository.save(avtale);
    }

    @PostMapping("/annuller-og-resend-tilskuddsperiode/{tilskuddsperiodeId}")
    @Transactional
    public void annullerOgResendTilskuddsperiode(@PathVariable("tilskuddsperiodeId") UUID id) {
        sjekkTilgang();
        log.info("Annullerer tilskuddsperiode {} og resender som godkjent", id);
        TilskuddPeriode tilskuddPeriode = tilskuddPeriodeRepository.findById(id).orElseThrow(RessursFinnesIkkeException::new);
        Avtale avtale = tilskuddPeriode.getAvtale();
        avtale.annullerTilskuddsperiode(tilskuddPeriode);
        avtale.lagNyGodkjentTilskuddsperiodeFraAnnullertPeriode(tilskuddPeriode);
        avtaleRepository.save(avtale);
    }

    @PostMapping("/annuller-og-generer-tilskuddsperiode/{tilskuddsperiodeId}")
    @Transactional
    public void annullerOgGenererTilskuddsperiode(@PathVariable("tilskuddsperiodeId") UUID id) {
        sjekkTilgang();
        log.info("Annullerer tilskuddsperiode {} og genererer ny ubehandlet", id);
        TilskuddPeriode tilskuddPeriode = tilskuddPeriodeRepository.findById(id).orElseThrow(RessursFinnesIkkeException::new);
        Avtale avtale = tilskuddPeriode.getAvtale();
        avtale.annullerTilskuddsperiode(tilskuddPeriode);
        avtale.lagNyTilskuddsperiodeFraAnnullertPeriode(tilskuddPeriode);
        avtaleRepository.save(avtale);
    }

    @PostMapping("/annuller-og-generer-behandlet-i-arena-perioder/{avtaleId}/{dato}")
    @Transactional
    public void annullerOgGenererBehandletIArenaPerioder(@PathVariable("avtaleId") UUID avtaleId, @PathVariable("dato") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate dato) {
        sjekkTilgang();
        log.info("Annullerer tilskuddsperioder med sluttdato før {} på avtale {} og lager nye med status behandlet i arena", dato, avtaleId);
        Avtale avtale = avtaleRepository.findById(avtaleId).orElseThrow(RessursFinnesIkkeException::new);
        List<TilskuddPeriode> tilskuddsperioder = tilskuddPeriodeRepository.findAllByAvtaleAndSluttDatoBefore(avtale, dato);
        log.info("Fant {} tilskuddsperioder som skal annulleres og genereres på nytt med behandlet i arena status", tilskuddsperioder.size());

        tilskuddsperioder.stream().toList().forEach(tp -> {
            avtale.annullerTilskuddsperiode(tp);
            avtale.lagNyBehandletIArenaTilskuddsperiodeFraAnnullertPeriode(tp);
        });

        log.info("Avtale {} har nå {} perioder med status behandlet i arena", avtaleId, avtale.getTilskuddPeriode().stream().filter(tp -> tp.getStatus() == TilskuddPeriodeStatus.BEHANDLET_I_ARENA).count());
        avtaleRepository.save(avtale);
    }

    @PostMapping("/lag-tilskuddsperioder-for-en-avtale/{avtaleId}/{migreringsDato}")
    @Transactional
    public void lagTilskuddsperioderPåEnAvtale(@PathVariable("avtaleId") UUID id, @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate migreringsDato) {
        sjekkTilgang();
        log.info("Lager tilskuddsperioder på en enkelt avtale {} fra dato {}", id, migreringsDato);
        Avtale avtale = avtaleRepository.findById(id)
                .orElseThrow(RessursFinnesIkkeException::new);
        avtale.nyeTilskuddsperioderEtterMigreringFraArena(migreringsDato, false);
        avtaleRepository.save(avtale);
    }

    @PostMapping("/reberegn-ubehandlede-tilskuddsperioder/{avtaleId}")
    @Transactional
    public void reberegnUbehandledeTilskuddsperioder(@PathVariable("avtaleId") UUID avtaleId) {
        sjekkTilgang();
        log.info("Reberegner ubehandlede tilskuddsperioder for avtale: {}", avtaleId);
        Avtale avtale = avtaleRepository.findById(avtaleId).orElseThrow(RessursFinnesIkkeException::new);
        avtale.reberegnUbehandledeTilskuddsperioder();
        avtaleRepository.save(avtale);
    }

    @PostMapping("/finn-avtaler-med-tilskuddsperioder-feil-datoer")
    public void finnTilskuddsperioderMedFeilDatoer() {
        sjekkTilgang();
        log.info("Finner avtaler som har tilskuddsperioder med mindre startdato enn en periode med lavere løpenummer");
        List<Avtale> midlertidige = avtaleRepository.findAllByTiltakstype(Tiltakstype.MIDLERTIDIG_LONNSTILSKUDD);
        midlertidige.removeIf(a -> a.getGjeldendeInnhold().getAvtaleInngått() == null);
        midlertidige.removeIf(a -> a.getTilskuddPeriode().size() == 0);

        midlertidige.forEach(avtale -> {
            avtale.getTilskuddPeriode().forEach(tp -> {
                if (tp.getLøpenummer() > 1) {
                    TilskuddPeriode forrigePeriode = avtale.getTilskuddPeriode().stream().filter(t -> t.getLøpenummer() == tp.getLøpenummer() - 1).collect(Collectors.toList()).stream().findFirst().orElseThrow();
                    if (tp.getStartDato().isBefore(forrigePeriode.getStartDato())) {
                        log.warn("Tilskuddsperiode med id {} har startDato før startDatoen til forrige løpenummer!", tp.getId());
                    }
                }
            });
        });
    }

}
