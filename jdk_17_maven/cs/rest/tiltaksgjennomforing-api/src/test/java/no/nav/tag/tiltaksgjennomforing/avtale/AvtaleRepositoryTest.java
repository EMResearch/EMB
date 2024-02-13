package no.nav.tag.tiltaksgjennomforing.avtale;

import no.nav.tag.tiltaksgjennomforing.Miljø;
import no.nav.tag.tiltaksgjennomforing.avtale.events.GodkjenningerOpphevetAvVeileder;
import no.nav.tag.tiltaksgjennomforing.datadeling.AvtaleMeldingEntitetRepository;
import no.nav.tag.tiltaksgjennomforing.datavarehus.DvhMeldingEntitetRepository;
import no.nav.tag.tiltaksgjennomforing.metrikker.MetrikkRegistrering;
import no.nav.tag.tiltaksgjennomforing.utils.Now;
import no.nav.tag.tiltaksgjennomforing.varsel.SmsRepository;
import no.nav.tag.tiltaksgjennomforing.varsel.VarselRepository;
import no.nav.tag.tiltaksgjennomforing.varsel.notifikasjon.ArbeidsgiverNotifikasjonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static no.nav.tag.tiltaksgjennomforing.avtale.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles({Miljø.LOCAL, "wiremock"})
@DirtiesContext
public class AvtaleRepositoryTest {

    @Autowired
    private AvtaleRepository avtaleRepository;

    @Autowired
    private VarselRepository varselRepository;

    @Autowired
    private SmsRepository smsRepository;

    @Autowired
    private AvtaleInnholdRepository avtaleInnholdRepository;

    @Autowired
    private DvhMeldingEntitetRepository dvhMeldingEntitetRepository;
    @Autowired
    private AvtaleMeldingEntitetRepository avtaleMeldingEntitetRepository;

    @Autowired
    private ArbeidsgiverNotifikasjonRepository arbeidsgiverNotifikasjonRepository;

    @MockBean
    private MetrikkRegistrering metrikkRegistrering;


    @BeforeEach
    public void setup() {
        varselRepository.deleteAll();
        smsRepository.deleteAll();
        avtaleInnholdRepository.deleteAll();
        arbeidsgiverNotifikasjonRepository.deleteAll();
        dvhMeldingEntitetRepository.deleteAll();
        avtaleMeldingEntitetRepository.deleteAll();
        avtaleRepository.deleteAll();
    }

    @Test
    public void nyAvtaleSkalKunneLagreOgReturneresAvRepository() {
        Avtale lagretAvtale = avtaleRepository.save(TestData.enArbeidstreningAvtale());

        Optional<Avtale> avtaleOptional = avtaleRepository.findById(lagretAvtale.getId());
        assertThat(avtaleOptional).isPresent();
    }

    @Test
    public void skalKunneLagreMaalFlereGanger() {
        // Lage avtale
        Avtale lagretAvtale = avtaleRepository.save(TestData.enArbeidstreningAvtale());

        // Lagre maal skal fungere
        EndreAvtale endreAvtale = new EndreAvtale();
        Maal maal = TestData.etMaal();
        endreAvtale.setMaal(List.of(maal));
        lagretAvtale.endreAvtale(Now.instant(), endreAvtale, Avtalerolle.VEILEDER, avtalerMedTilskuddsperioder);
        avtaleRepository.save(lagretAvtale);

        // Lage ny avtale
        Avtale lagretAvtale2 = avtaleRepository.save(TestData.enArbeidstreningAvtale());

        // Lagre maal skal enda fungere
        EndreAvtale endreAvtale2 = new EndreAvtale();
        Maal maal2 = TestData.etMaal();
        endreAvtale2.setMaal(List.of(maal2));
        lagretAvtale2.endreAvtale(Now.instant(), endreAvtale2, Avtalerolle.VEILEDER, avtalerMedTilskuddsperioder);
        avtaleRepository.save(lagretAvtale2);
    }

    @Test
    public void skalKunneLagreOppgaverFlereGanger() {
        // Lage avtale
        Avtale lagretAvtale = avtaleRepository.save(TestData.enArbeidstreningAvtale());

        // Lagre maal skal fungere
        EndreAvtale endreAvtale = new EndreAvtale();
        lagretAvtale.endreAvtale(Now.instant(), endreAvtale, Avtalerolle.VEILEDER, avtalerMedTilskuddsperioder);
        avtaleRepository.save(lagretAvtale);

        // Lage ny avtale
        Avtale lagretAvtale2 = avtaleRepository.save(TestData.enArbeidstreningAvtale());

        // Lagre maal skal enda fungere
        EndreAvtale endreAvtale2 = new EndreAvtale();
        lagretAvtale2.endreAvtale(Now.instant(), endreAvtale2, Avtalerolle.VEILEDER, avtalerMedTilskuddsperioder);
        avtaleRepository.save(lagretAvtale2);
    }

    @Test
    public void skalKunneLagreTilskuddsPeriode() {
        // Lage avtale
        Avtale lagretAvtale = TestData.enMidlertidigLonnstilskuddAvtaleMedAltUtfylt();
        lagretAvtale.getGjeldendeInnhold().setSumLonnstilskudd(20000);
        lagretAvtale = avtaleRepository.save(lagretAvtale);

        // Lagre tilskuddsperiode skal fungere
        EndreAvtale endreAvtale = new EndreAvtale();
        endreAvtale.setStartDato(lagretAvtale.getGjeldendeInnhold().getStartDato());
        endreAvtale.setSluttDato(lagretAvtale.getGjeldendeInnhold().getSluttDato());
        endreAvtale.setManedslonn(20000);
        endreAvtale.setStillingprosent(100);
        endreAvtale.setOtpSats(0.02);
        endreAvtale.setFeriepengesats(BigDecimal.valueOf(0.12));
        endreAvtale.setArbeidsgiveravgift(BigDecimal.valueOf(0.141));
        endreAvtale.setLonnstilskuddProsent(40);

        lagretAvtale.endreAvtale(Now.instant(), endreAvtale, Avtalerolle.VEILEDER, avtalerMedTilskuddsperioder);
        Avtale nyLagretAvtale = avtaleRepository.save(lagretAvtale);

        var perioder = nyLagretAvtale.getTilskuddPeriode();
        assertThat(perioder).isNotEmpty();
        assertThat(lagretAvtale.getId()).isEqualTo(perioder.first().getAvtale().getId());
    }

    @Test
    public void avtale_godkjent_pa_vegne_av_skal_lagres_med_pa_vegne_av_grunn() {
        Avtale avtale = TestData.enAvtaleMedAltUtfylt();
        avtale.getGjeldendeInnhold().setGodkjentAvArbeidsgiver(Now.localDateTime());
        GodkjentPaVegneGrunn godkjentPaVegneGrunn = TestData.enGodkjentPaVegneGrunn();
        godkjentPaVegneGrunn.setIkkeBankId(true);
        Veileder veileder = TestData.enVeileder(avtale);

        veileder.godkjennForVeilederOgDeltaker(godkjentPaVegneGrunn, avtale);
        Avtale lagretAvtale = avtaleRepository.save(avtale);

        assertThat(lagretAvtale.getGjeldendeInnhold().getGodkjentPaVegneGrunn().isIkkeBankId()).isEqualTo(godkjentPaVegneGrunn.isIkkeBankId());
    }

    @Test
    public void lagre_pa_vegne_skal_publisere_domainevent() {
        Avtale avtale = TestData.enAvtaleMedAltUtfylt();
        avtale.getGjeldendeInnhold().setGodkjentAvArbeidsgiver(Now.localDateTime());
        Veileder veileder = TestData.enVeileder(avtale);
        GodkjentPaVegneGrunn godkjentPaVegneGrunn = TestData.enGodkjentPaVegneGrunn();
        veileder.godkjennForVeilederOgDeltaker(godkjentPaVegneGrunn, avtale);

        avtaleRepository.save(avtale);
        verify(metrikkRegistrering).godkjentPaVegneAv(any());
    }

    @Test
    public void opprettAvtale__skal_publisere_domainevent() {
        Avtale nyAvtale = Avtale.veilederOppretterAvtale(new OpprettAvtale(new Fnr("10101033333"), new BedriftNr("101033333"), Tiltakstype.ARBEIDSTRENING), new NavIdent("Q000111"));
        avtaleRepository.save(nyAvtale);
        verify(metrikkRegistrering).avtaleOpprettet(any());
    }

    @Test
    public void endreAvtale__skal_publisere_domainevent() {
        Avtale avtale = avtaleRepository.save(TestData.enArbeidstreningAvtale());
        verify(metrikkRegistrering, never()).avtaleEndret(any());
        avtale.endreAvtale(Now.instant(), TestData.ingenEndring(), Avtalerolle.VEILEDER, avtalerMedTilskuddsperioder);
        avtaleRepository.save(avtale);
        verify(metrikkRegistrering).avtaleEndret(any());
    }

    @Test
    public void godkjennForArbeidsgiver__skal_publisere_domainevent() {
        Avtale avtale = TestData.enAvtaleMedAltUtfylt();
        TestData.enArbeidsgiver(avtale).godkjennAvtale(avtale.getSistEndret(), avtale);
        avtaleRepository.save(avtale);
        verify(metrikkRegistrering).godkjentAvArbeidsgiver(any());
    }

    @Test
    public void godkjennForDeltaker__skal_publisere_domainevent() {
        Avtale avtale = TestData.enAvtaleMedAltUtfylt();
        TestData.enDeltaker(avtale).godkjennAvtale(avtale.getSistEndret(), avtale);
        avtaleRepository.save(avtale);
        verify(metrikkRegistrering).godkjentAvDeltaker(any());
    }

    @Test
    public void godkjennForVeileder__skal_publisere_domainevent() {
        Avtale avtale = TestData.enAvtaleMedAltUtfylt();
        avtale.getGjeldendeInnhold().setGodkjentAvDeltaker(Now.localDateTime());
        avtale.getGjeldendeInnhold().setGodkjentAvArbeidsgiver(Now.localDateTime());
        TestData.enVeileder(avtale).godkjennAvtale(avtale.getSistEndret(), avtale);
        avtaleRepository.save(avtale);
        verify(metrikkRegistrering).godkjentAvVeileder(any());
    }

    @Test
    public void opphevGodkjenning__skal_publisere_domainevent() {
        Avtale avtale = TestData.enAvtaleMedAltUtfylt();
        TestData.enArbeidsgiver(avtale).godkjennForAvtalepart(avtale);
        TestData.enVeileder(avtale).opphevGodkjenninger(avtale);
        avtaleRepository.save(avtale);
        verify(metrikkRegistrering).godkjenningerOpphevet(any(GodkjenningerOpphevetAvVeileder.class));
    }

    @Test
    public void finnGodkjenteAvtalerMedTilskuddsperiodestatusOgNavEnheter__skal_ikke_kunne_hente_avtale_med_tiltakstype_arbeidstrening_3() {
        Avtale avtale = enMidlertidigLønnstilskuddsAvtaleMedStartOgSluttGodkjentAvAlleParter(Now.localDate().plusDays(1), Now.localDate().plusMonths(3).plusDays(1));
        Avtale avtale2 = enMidlertidigLønnstilskuddsAvtaleMedStartOgSluttGodkjentAvAlleParter(Now.localDate().plusDays(5), Now.localDate().plusMonths(3).plusDays(5));
        Avtale avtale3 = enMidlertidigLønnstilskuddsAvtaleMedStartOgSluttGodkjentAvAlleParter(Now.localDate().plusDays(10), Now.localDate().plusMonths(3).plusDays(10));
        Avtale avtale4 = enMidlertidigLønnstilskuddsAvtaleMedStartOgSluttGodkjentAvAlleParter(Now.localDate().plusDays(15), Now.localDate().plusMonths(3).plusDays(15));
        avtale.getGjeldendeInnhold().setDeltakerFornavn("Arne");
        avtale2.getGjeldendeInnhold().setDeltakerFornavn("Bjarne");
        avtale3.getGjeldendeInnhold().setDeltakerFornavn("Carl");

        avtaleRepository.save(avtale);
        avtaleRepository.save(avtale2);
        avtaleRepository.save(avtale3);
        avtaleRepository.save(avtale4);

        Set<String> navEnheter = Set.of(ENHET_OPPFØLGING.getVerdi());
        Set<Tiltakstype> tiltakstype = Set.of(Tiltakstype.MIDLERTIDIG_LONNSTILSKUDD, Tiltakstype.VARIG_LONNSTILSKUDD);
        Sort by = Sort.by(Sort.Order.asc("startDato"));
        Pageable pageable = PageRequest.of(0, 10, by);
        long plussDato = ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.now().plusMonths(3));
        LocalDate decisiondate = LocalDate.now().plusDays(plussDato);

        Page<BeslutterOversiktDTO> beslutterOversikt = avtaleRepository.finnGodkjenteAvtalerMedTilskuddsperiodestatusOgNavEnheter(
                TilskuddPeriodeStatus.UBEHANDLET,
                decisiondate,
                tiltakstype,
                navEnheter,
                null,
                null,
                pageable
        );

        List<BeslutterOversikt> beslutterOversiktList = BeslutterOversikt.getBeslutterOversikt(beslutterOversikt);
        assertThat(beslutterOversiktList.size()).isEqualTo(4);
    }

    @Test
    public void findAllByEnhet__skal_kunne_hente_avtale_med_enhet() {
        Pageable pageable = PageRequest.of(0, 100);
        Avtale lagretAvtale = TestData.enArbeidstreningAvtaleOpprettetAvArbeidsgiverOgErUfordeltMedOppfølgningsEnhet();
        avtaleRepository.save(lagretAvtale);

        Page<Avtale> avtaleMedRiktigEnhet = avtaleRepository
                .findAllByVeilederNavIdentIsNullAndEnhetGeografiskOrVeilederNavIdentIsNullAndEnhetOppfolging(ENHET_GEOGRAFISK.getVerdi(), ENHET_OPPFØLGING.getVerdi(), pageable);

        assertThat(avtaleMedRiktigEnhet.getContent()).isNotEmpty();
    }

    @Test
    public void findAllByEnhet__skal_ikke_kunne_hente_avtale_med_feil_enhet() {
        Pageable pageable = PageRequest.of(0, 100);
        Avtale lagretAvtale = TestData.enArbeidstreningAvtaleOpprettetAvArbeidsgiverOgErUfordeltMedOppfølgningsEnhet();
        avtaleRepository.save(lagretAvtale);

        Page<Avtale> avtaleMedRiktigEnhet = avtaleRepository
            .findAllByVeilederNavIdentIsNullAndEnhetGeografiskOrVeilederNavIdentIsNullAndEnhetOppfolging(ENHET_GEOGRAFISK.getVerdi(), ENHET_GEOGRAFISK.getVerdi(), pageable);

        assertThat(avtaleMedRiktigEnhet.getContent()).isEmpty();
    }

    @Test
    public void findAllByEnhet__skal_kunne_hente_avtale_med_både_geografisk_og_oppfølgningsenhet() {
        Pageable pageable = PageRequest.of(0, 100);
        Avtale lagretAvtale = TestData.enArbeidstreningAvtaleOpprettetAvArbeidsgiverOgErUfordeltMedOppfølgningsEnhetOgGeografiskEnhet();
        avtaleRepository.save(lagretAvtale);

        Page<Avtale> avtaleMedRiktigEnhet = avtaleRepository
            .findAllByVeilederNavIdentIsNullAndEnhetGeografiskOrVeilederNavIdentIsNullAndEnhetOppfolging(ENHET_OPPFØLGING.getVerdi(), ENHET_OPPFØLGING.getVerdi(), pageable);

        assertThat(avtaleMedRiktigEnhet.getContent()).isNotEmpty();
    }

    @Test
    public void findAllByEnhet__skal_kunne_hente_avtale_med_både_oppfølgning_og_geografiskenhet() {
        Pageable pageable = PageRequest.of(0, 100);
        Avtale lagretAvtale = TestData.enArbeidstreningAvtaleOpprettetAvArbeidsgiverOgErUfordeltMedOppfølgningsEnhetOgGeografiskEnhet();
        avtaleRepository.save(lagretAvtale);

        Page<Avtale> avtaleMedRiktigEnhet = avtaleRepository.findAllByVeilederNavIdentIsNullAndEnhetGeografiskOrVeilederNavIdentIsNullAndEnhetOppfolging(ENHET_GEOGRAFISK.getVerdi(), ENHET_GEOGRAFISK.getVerdi(), pageable);

        assertThat(avtaleMedRiktigEnhet.getContent()).isNotEmpty();
    }
}
