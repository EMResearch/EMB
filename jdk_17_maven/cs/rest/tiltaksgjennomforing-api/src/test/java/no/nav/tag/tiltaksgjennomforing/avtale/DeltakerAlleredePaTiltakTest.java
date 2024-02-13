package no.nav.tag.tiltaksgjennomforing.avtale;

import no.nav.tag.tiltaksgjennomforing.Miljø;
import no.nav.tag.tiltaksgjennomforing.datadeling.AvtaleMeldingEntitetRepository;
import no.nav.tag.tiltaksgjennomforing.datavarehus.DvhMeldingEntitetRepository;
import no.nav.tag.tiltaksgjennomforing.varsel.SmsRepository;
import no.nav.tag.tiltaksgjennomforing.varsel.VarselRepository;
import no.nav.tag.tiltaksgjennomforing.varsel.notifikasjon.ArbeidsgiverNotifikasjonRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@SpringBootTest
@ActiveProfiles({Miljø.LOCAL})
@DirtiesContext
public class DeltakerAlleredePaTiltakTest {

    @Autowired
    private AvtaleRepository avtaleRepository;

    @Autowired
    VarselRepository varselRepository;

    @Autowired
    SmsRepository smsRepository;

    @Autowired
    AvtaleInnholdRepository avtaleInnholdRepository;

    @Autowired
    ArbeidsgiverNotifikasjonRepository arbeidsgiverNotifikasjonRepository;

    @Autowired
    DvhMeldingEntitetRepository dvhMeldingEntitetRepository;
    @Autowired
    AvtaleMeldingEntitetRepository avtaleMeldingEntitetRepository;

    @BeforeEach
    public void init() {
        slettInnholdDatabase();
    }

    private void slettInnholdDatabase() {
        varselRepository.deleteAll();
        smsRepository.deleteAll();
        avtaleInnholdRepository.deleteAll();
        arbeidsgiverNotifikasjonRepository.deleteAll();
        dvhMeldingEntitetRepository.deleteAll();
        avtaleMeldingEntitetRepository.deleteAll();
        avtaleRepository.deleteAll();
    }

    private void initAvtalerTilDBTest() {
        settAvtaleInformasjon(
                TestData.enArbeidstreningAvtale(),
                new Fnr("00000000000"),
                LocalDate.now(),
                LocalDate.now().plusMonths(2),
                LocalDateTime.now()
        );
        settAvtaleInformasjon(
                TestData.enMentorAvtaleUsignert(),
                new Fnr("00000000000"),
                LocalDate.now(),
                LocalDate.now().plusMonths(2),
                LocalDateTime.now()
        );
        settAvtaleInformasjon(
                TestData.enInkluderingstilskuddAvtale(),
                new Fnr("00000000000"),
                LocalDate.now(),
                LocalDate.now().plusMonths(2),
                LocalDateTime.now()
        );
    }

    private void settAvtaleInformasjon(Avtale avtale, Fnr deltakerFnr, LocalDate startDato, LocalDate sluttDato, LocalDateTime godkjentAvVeileder) {
        avtale.setDeltakerFnr(deltakerFnr);
        avtale.getGjeldendeInnhold().setStartDato(startDato);
        avtale.getGjeldendeInnhold().setSluttDato(sluttDato);
        if(godkjentAvVeileder != null) {
            avtale.getGjeldendeInnhold().setGodkjentAvVeileder(godkjentAvVeileder);
        }
        avtaleRepository.save(avtale);
    }

    @Test
    public void skal_returnere_avtaler_deltaker_allerede_er_registrert_paa() {
        initAvtalerTilDBTest();
        List<Avtale> avtaleAlleredeRegistrertPaDeltaker = avtaleRepository.finnAvtalerSomOverlapperForDeltakerVedGodkjenningAvAvtale(
                "00000000000",
                UUID.randomUUID().toString(),
                Date.valueOf(LocalDate.now()),
                Date.valueOf(LocalDate.now().plusMonths(1))
        );
        Assertions.assertEquals(3, avtaleAlleredeRegistrertPaDeltaker.size());
    }

    @Test
    public void avtalePaDeltakerUtenNoenAvtaleIdOgSluttdato() {
        initAvtalerTilDBTest();
        List<Avtale> avtalePaDeltakerUtenNoenAvtaleIdOgSluttdato = avtaleRepository.finnAvtalerSomOverlapperForDeltakerVedOpprettelseAvAvtale(
                "00000000000",
                Date.valueOf(LocalDate.now())
        );
        Assertions.assertEquals(3, avtalePaDeltakerUtenNoenAvtaleIdOgSluttdato.size());
    }

    @Test
    public void avtalePaDeltakerMedKunOverlappendeStartdato() {
        initAvtalerTilDBTest();
        List<Avtale> avtalePaDeltakerMedKunOverlappendeStartdato = avtaleRepository.finnAvtalerSomOverlapperForDeltakerVedGodkjenningAvAvtale(
                "00000000000",
                UUID.randomUUID().toString(),
                Date.valueOf(LocalDate.now()),
                Date.valueOf(LocalDate.now().plusMonths(3))
        );
        Assertions.assertEquals(3, avtalePaDeltakerMedKunOverlappendeStartdato.size());
    }

    @Test
    public void avtalePaDeltakerMedKunOverlappendeSluttdato() {
        initAvtalerTilDBTest();
        List<Avtale> avtalePaDeltakerMedKunOverlappendeSluttdato = avtaleRepository.finnAvtalerSomOverlapperForDeltakerVedGodkjenningAvAvtale(
                "00000000000",
                UUID.randomUUID().toString(),
                Date.valueOf(LocalDate.now().minusMonths(1)),
                Date.valueOf(LocalDate.now().plusMonths(1))
        );
        Assertions.assertEquals(3, avtalePaDeltakerMedKunOverlappendeSluttdato.size());
    }

    @Test
    public void sjekkAtRegistreringAvArbeidstreningHvorDetAlleredeFinnesEnITidsrommetReturnererEttTreff() {
       initAvtalerTilDBTest();
       Veileder veileder_z123456 = TestData.enVeileder(new NavIdent("Z123456"));

        List<AlleredeRegistrertAvtale> treffPaAvtalerSomErUlovligMatch = veileder_z123456.hentAvtaleDeltakerAlleredeErRegistrertPaa(
                new Fnr("00000000000"),
                Tiltakstype.ARBEIDSTRENING,
                null,
                LocalDate.now(),
                LocalDate.now().plusMonths(1),
                avtaleRepository
        );
        Assertions.assertEquals(1, treffPaAvtalerSomErUlovligMatch.size());
    }

    @Test
    public void sjekkAtDetIkkeFaarNoeTreffPaaArbeidstreningSomErUtenforStartOgSluttDato() {
        Veileder veileder_z123456 = TestData.enVeileder(new NavIdent("Z123456"));
        settAvtaleInformasjon(
                TestData.enArbeidstreningAvtale(),
                new Fnr("00000000000"),
                LocalDate.now().plusMonths(1).plusDays(1),
                LocalDate.now().plusMonths(3),
                LocalDateTime.now()
        );
        settAvtaleInformasjon(
                TestData.enMentorAvtaleUsignert(),
                new Fnr("00000000000"),
                LocalDate.now(),
                LocalDate.now().plusMonths(2),
                LocalDateTime.now()
        );
        settAvtaleInformasjon(
                TestData.enInkluderingstilskuddAvtale(),
                new Fnr("00000000000"),
                LocalDate.now(),
                LocalDate.now().plusMonths(2),
                LocalDateTime.now()
        );
        List<AlleredeRegistrertAvtale> treffPaAvtalerSomErUlovligMatch = veileder_z123456.hentAvtaleDeltakerAlleredeErRegistrertPaa(
                new Fnr("00000000000"),
                Tiltakstype.ARBEIDSTRENING,
                null,
                LocalDate.now(),
                LocalDate.now().plusMonths(1),
                avtaleRepository
        );
        Assertions.assertEquals(0, treffPaAvtalerSomErUlovligMatch.size());
    }

    @Test
    public void sjekkAtDetReturneresEnTreffPaaMentorTilskuddNarDetAlleredeFinnesAvtaleSammeTidsrom() {
        Veileder veileder_z123456 = TestData.enVeileder(new NavIdent("Z123456"));
        settAvtaleInformasjon(
                TestData.enArbeidstreningAvtale(),
                new Fnr("00000000000"),
                LocalDate.now().plusMonths(1).plusDays(1),
                LocalDate.now().plusMonths(3),
                LocalDateTime.now()
        );
        settAvtaleInformasjon(
                TestData.enMentorAvtaleUsignert(),
                new Fnr("00000000000"),
                LocalDate.now(),
                LocalDate.now().plusMonths(2),
                LocalDateTime.now()
        );
        settAvtaleInformasjon(
                TestData.enInkluderingstilskuddAvtale(),
                new Fnr("00000000000"),
                LocalDate.now(),
                LocalDate.now().plusMonths(2),
                LocalDateTime.now()
        );
        List<AlleredeRegistrertAvtale> treffPaAvtalerSomErUlovligMatch = veileder_z123456.hentAvtaleDeltakerAlleredeErRegistrertPaa(
                new Fnr("00000000000"),
                Tiltakstype.MENTOR,
                null,
                LocalDate.now(),
                LocalDate.now().plusMonths(1),
                avtaleRepository
        );
        Assertions.assertEquals(1, treffPaAvtalerSomErUlovligMatch.size());
    }

    @Test
    public void sjekkAtDetReturneresAvtalerSomIkkeErFerdigUtfylt() {
        Veileder veileder_z123456 = TestData.enVeileder(new NavIdent("Z123456"));
        settAvtaleInformasjon(
                TestData.enArbeidstreningAvtale(),
                new Fnr("00000000000"),
                LocalDate.now().plusMonths(1).plusDays(1),
                LocalDate.now().plusMonths(3),
                LocalDateTime.now()
        );
        settAvtaleInformasjon(
                TestData.enArbeidstreningAvtale(),
                new Fnr("00000000000"),
                null,
                null,
                null
        );
        settAvtaleInformasjon(
                TestData.enMentorAvtaleUsignert(),
                new Fnr("00000000000"),
                LocalDate.now(),
                LocalDate.now().plusMonths(2),
                LocalDateTime.now()
        );
        settAvtaleInformasjon(
                TestData.enInkluderingstilskuddAvtale(),
                new Fnr("00000000000"),
                LocalDate.now(),
                LocalDate.now().plusMonths(2),
                LocalDateTime.now()
        );
        List<AlleredeRegistrertAvtale> treffPaAvtalerSomErUlovligMatch = veileder_z123456.hentAvtaleDeltakerAlleredeErRegistrertPaa(
                new Fnr("00000000000"),
                Tiltakstype.ARBEIDSTRENING,
                null,
                LocalDate.now(),
                LocalDate.now().plusMonths(1),
                avtaleRepository
        );
        Assertions.assertEquals(1, treffPaAvtalerSomErUlovligMatch.size());
    }
}
