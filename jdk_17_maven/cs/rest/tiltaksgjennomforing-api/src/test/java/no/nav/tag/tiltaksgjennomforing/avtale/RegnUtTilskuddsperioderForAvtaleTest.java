package no.nav.tag.tiltaksgjennomforing.avtale;

import no.nav.tag.tiltaksgjennomforing.enhet.Kvalifiseringsgruppe;
import no.nav.tag.tiltaksgjennomforing.exceptions.FeilkodeException;
import no.nav.tag.tiltaksgjennomforing.utils.Now;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static no.nav.tag.tiltaksgjennomforing.avtale.TestData.avtalerMedTilskuddsperioder;
import static no.nav.tag.tiltaksgjennomforing.utils.DatoUtils.sisteDatoIMnd;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class RegnUtTilskuddsperioderForAvtaleTest {

    @Test
    public void en_tilskuddsperiode() {
        Now.fixedDate(LocalDate.of(2021, 1, 1));
        LocalDate fra = LocalDate.of(2021, 1, 1);
        LocalDate til = LocalDate.of(2021, 3, 31);

        Avtale avtale = TestData.enMidlertidigLonnstilskuddAvtaleMedAltUtfylt();
        EndreAvtale endreAvtale = TestData.endringPåAlleLønnstilskuddFelter();
        endreAvtale.setStartDato(fra);
        endreAvtale.setSluttDato(til);
        avtale.endreAvtale(Now.instant(), endreAvtale, Avtalerolle.VEILEDER, avtalerMedTilskuddsperioder);


        assertThat(avtale.getTilskuddPeriode().size()).isEqualTo(3);
        assertThat(avtale.getTilskuddPeriode().first().getBeløp()).isEqualTo(avtale.getGjeldendeInnhold().getSumLonnstilskudd());
        harRiktigeEgenskaper(avtale);
        Now.resetClock();
    }

    @Test
    public void splitt_ved_nyttår() {
        Now.fixedDate(LocalDate.of(2020, 12, 1));
        LocalDate fra = LocalDate.of(2020, 12, 1);
        LocalDate til = LocalDate.of(2021, 1, 31);

        Avtale avtale = TestData.enMidlertidigLonnstilskuddAvtaleMedAltUtfylt();
        EndreAvtale endreAvtale = TestData.endringPåAlleLønnstilskuddFelter();
        endreAvtale.setStartDato(fra);
        endreAvtale.setSluttDato(til);
        avtale.endreAvtale(Now.instant(), endreAvtale, Avtalerolle.VEILEDER, avtalerMedTilskuddsperioder);
        var tilskuddPerioder = avtale.getTilskuddPeriode();

        assertThat(tilskuddPerioder.size()).isEqualTo(2);
        Iterator<TilskuddPeriode> iterator = tilskuddPerioder.iterator();
        TilskuddPeriode første = iterator.next();
        TilskuddPeriode andre = iterator.next();
        assertThat(første.getBeløp()).isEqualTo(andre.getBeløp());
        harRiktigeEgenskaper(avtale);
        Now.resetClock();
    }

    @Test
    public void reduksjon_etter_6_mnd__30_prosent_lonnstilskudd() {
        Avtale avtale = TestData.enMidlertidigLonnstilskuddAvtaleMedAltUtfylt();
        avtale.setKvalifiseringsgruppe(Kvalifiseringsgruppe.SITUASJONSBESTEMT_INNSATS);
        EndreAvtale endreAvtale = TestData.endringPåAlleLønnstilskuddFelter();
        endreAvtale.setLonnstilskuddProsent(40);
        avtale.endreAvtale(Now.instant(), endreAvtale, Avtalerolle.VEILEDER, avtalerMedTilskuddsperioder);
        TilskuddPeriode tilskuddpeirode6mndEtterStart = finnTilskuddsperiodeForDato(avtale.getGjeldendeInnhold().getStartDato().plusMonths(6), avtale);
        TilskuddPeriode tilskuddperiodeDagenFør6Mnd = finnTilskuddsperiodeForDato(avtale.getGjeldendeInnhold().getStartDato().plusMonths(6).minusDays(1), avtale);

        assertThat(tilskuddpeirode6mndEtterStart.getLonnstilskuddProsent()).isEqualTo(30);
        assertThat(tilskuddperiodeDagenFør6Mnd.getLonnstilskuddProsent()).isEqualTo(40);

        harRiktigeEgenskaper(avtale);
    }

    private TilskuddPeriode finnTilskuddsperiodeForDato(LocalDate dato, Avtale avtale) {
        for (TilskuddPeriode tilskuddsperiode : avtale.getTilskuddPeriode()) {
            if (tilskuddsperiode.getStartDato().isBefore(dato.plusDays(1)) && tilskuddsperiode.getSluttDato().isAfter(dato.minusDays(1))) {
                return tilskuddsperiode;
            }
        }
        return null;
    }

    @Test
    public void finnTilskuddsperiodeForDato() {
        Now.fixedDate(LocalDate.of(2021, 1, 1));
        Avtale avtale = TestData.enMidlertidigLonnstilskuddAvtaleMedAltUtfylt();
        EndreAvtale endreAvtale = TestData.endringPåAlleLønnstilskuddFelter();
        endreAvtale.setStartDato(LocalDate.of(2021, 1, 1));
        endreAvtale.setSluttDato(LocalDate.of(2021, 10, 1));
        avtale.endreAvtale(Now.instant(), endreAvtale, Avtalerolle.VEILEDER, avtalerMedTilskuddsperioder);

        TilskuddPeriode tilskuddPeriode1 = finnTilskuddsperiodeForDato(LocalDate.of(2021, 1, 1), avtale);
        TilskuddPeriode tilskuddPeriode2 = finnTilskuddsperiodeForDato(LocalDate.of(2021, 2, 1), avtale);

        assertThat(tilskuddPeriode1).isEqualTo(avtale.tilskuddsperiode(0));
        assertThat(tilskuddPeriode2).isEqualTo(avtale.tilskuddsperiode(1));
        Now.resetClock();
    }

    @Test
    public void sjekkAtEnhetsnrOgEnhetsnavnBlirSattPaEndreAvtale() {
        final String ENHETS_NR = "1001";
        final String ENHETS_NAVN = "NAV Ullensaker";

        Avtale avtale = TestData.enMidlertidigLonnstilskuddAvtaleMedAltUtfylt();
        EndreAvtale endreAvtale = TestData.endringPåAlleLønnstilskuddFelter();
        endreAvtale.setStartDato(avtale.getGjeldendeInnhold().getStartDato());
        endreAvtale.setSluttDato(avtale.getGjeldendeInnhold().getSluttDato());

        avtale.oppdatereKostnadsstedForTilskuddsperioder(new NyttKostnadssted(ENHETS_NR, ENHETS_NAVN));
        assertThat(avtale.tilskuddsperiode(0).getEnhet()).isEqualTo(ENHETS_NR);
        assertThat(avtale.tilskuddsperiode(0).getEnhetsnavn()).isEqualTo(ENHETS_NAVN);
        assertThat(avtale.tilskuddsperiode(1).getEnhet()).isEqualTo(ENHETS_NR);
        assertThat(avtale.tilskuddsperiode(2).getEnhetsnavn()).isEqualTo(ENHETS_NAVN);

        avtale.endreAvtale(Now.instant(), endreAvtale, Avtalerolle.VEILEDER, avtalerMedTilskuddsperioder);
        assertThat(avtale.tilskuddsperiode(0).getEnhet()).isEqualTo(ENHETS_NR);
        assertThat(avtale.tilskuddsperiode(0).getEnhetsnavn()).isEqualTo(ENHETS_NAVN);
        assertThat(avtale.tilskuddsperiode(1).getEnhet()).isEqualTo(ENHETS_NR);
        assertThat(avtale.tilskuddsperiode(2).getEnhetsnavn()).isEqualTo(ENHETS_NAVN);
    }

    @Test
    public void reduksjon_etter_12_mnd_60_prosent_lonnstilskudd() {
        Avtale avtale = TestData.enMidlertidigLonnstilskuddAvtaleMedAltUtfylt();
        avtale.setKvalifiseringsgruppe(Kvalifiseringsgruppe.SPESIELT_TILPASSET_INNSATS);
        EndreAvtale endreAvtale = TestData.endringPåAlleLønnstilskuddFelter();
        endreAvtale.setLonnstilskuddProsent(60);
        endreAvtale.setSluttDato(endreAvtale.getStartDato().plusMonths(13));
        avtale.endreAvtale(Now.instant(), endreAvtale, Avtalerolle.VEILEDER, avtalerMedTilskuddsperioder);

        TilskuddPeriode sisteTilskuddsperiode = avtale.tilskuddsperiode(avtale.getTilskuddPeriode().size() - 1);
        assertThat(sisteTilskuddsperiode.getLonnstilskuddProsent()).isEqualTo(50);
        harRiktigeEgenskaper(avtale);
    }

    @Test
    public void splitt_etter_reduksjon_30_prosnt_lonnstilskudd() {
        Now.fixedDate(LocalDate.of(2020, 6, 28));
        LocalDate startDato = LocalDate.of(2020, 6, 30);
        LocalDate sluttDato = LocalDate.of(2021, 1, 2);
        Avtale avtale = TestData.enMidlertidigLonnstilskuddAvtaleMedAltUtfylt();

        EndreAvtale endreAvtale = TestData.endringPåAlleLønnstilskuddFelter();
        endreAvtale.setLonnstilskuddProsent(40);
        endreAvtale.setStartDato(startDato);
        endreAvtale.setSluttDato(sluttDato);

        avtale.endreAvtale(Now.instant(), endreAvtale, Avtalerolle.VEILEDER, avtalerMedTilskuddsperioder);
        assertThat(avtale.getTilskuddPeriode().size()).isEqualTo(9);
        harRiktigeEgenskaper(avtale);
        Now.resetClock();
    }

    @Test
    public void sjett_at_reduksjon_skjer_tidlig_12_mnd_ved_68_prosent_eller_høyre(){
        Now.fixedDate(LocalDate.of(2021, 1, 1));
        LocalDate startDato = LocalDate.of(2021, 1, 1);
        LocalDate sluttDato = LocalDate.of(2022, 1, 10);
        Avtale avtale = TestData.enLonnstilskuddAvtaleMedAltUtfylt(Tiltakstype.VARIG_LONNSTILSKUDD);

        EndreAvtale endreAvtale = TestData.endringPåAlleLønnstilskuddFelter();
        endreAvtale.setStartDato(startDato);
        endreAvtale.setSluttDato(sluttDato);
        endreAvtale.setLonnstilskuddProsent(68);

        avtale.endreAvtale(Now.instant(), endreAvtale, Avtalerolle.VEILEDER, avtalerMedTilskuddsperioder);

        assertThat(avtale.getGjeldendeInnhold().getDatoForRedusertProsent()).isEqualTo(LocalDate.of(2022, 1, 1));
        assertThat(avtale.tilskuddsperiode(0).getLonnstilskuddProsent()).isEqualTo(68);
        assertThat(avtale.tilskuddsperiode(avtale.getTilskuddPeriode().size() - 1).getLonnstilskuddProsent()).isEqualTo(67);
        harRiktigeEgenskaper(avtale);
        Now.resetClock();
    }

    @Test
    public void sjett_at_reduksjon_skjer_etter_12_mnd_ved_høyre_enn_68_prosent(){
        Now.fixedDate(LocalDate.of(2023, 1, 2));
        LocalDate startDato = LocalDate.of(2022, 1, 1);
        LocalDate sluttDato = LocalDate.of(2023, 12, 12);
        Avtale avtale = TestData.enLonnstilskuddAvtaleMedAltUtfylt(Tiltakstype.VARIG_LONNSTILSKUDD);
        avtale.setGodkjentForEtterregistrering(true);
        EndreAvtale endreAvtale = TestData.endringPåAlleLønnstilskuddFelter();

        endreAvtale.setStartDato(startDato);
        endreAvtale.setSluttDato(sluttDato);
        endreAvtale.setLonnstilskuddProsent(70);

        avtale.endreAvtale(Now.instant(), endreAvtale, Avtalerolle.VEILEDER, avtalerMedTilskuddsperioder);

        assertThat(avtale.getGjeldendeInnhold().getDatoForRedusertProsent()).isEqualTo(LocalDate.of(2023, 1, 1));
        assertThat(avtale.tilskuddsperiode(0).getLonnstilskuddProsent()).isEqualTo(70);
        assertThat(avtale.tilskuddsperiode(avtale.getTilskuddPeriode().size() - 1).getLonnstilskuddProsent()).isEqualTo(67);
        harRiktigeEgenskaper(avtale);
        Now.resetClock();
    }

    @Test
    public void sjett_at_reduksjon_ikke_skjer_etter_12_mnd_under_68_prosent(){
        Now.fixedDate(LocalDate.of(2021, 1, 1));
        LocalDate startDato = LocalDate.of(2021, 2, 1);
        LocalDate sluttDato = LocalDate.of(2023, 3, 10);
        Avtale avtale = TestData.enLonnstilskuddAvtaleMedAltUtfylt(Tiltakstype.VARIG_LONNSTILSKUDD);

        EndreAvtale endreAvtale = TestData.endringPåAlleLønnstilskuddFelter();
        endreAvtale.setStartDato(startDato);
        endreAvtale.setSluttDato(sluttDato);
        endreAvtale.setLonnstilskuddProsent(40);

        avtale.endreAvtale(Now.instant(), endreAvtale, Avtalerolle.VEILEDER, avtalerMedTilskuddsperioder);

        assertThat(avtale.getGjeldendeInnhold().getDatoForRedusertProsent()).isNull();
        assertThat(avtale.getGjeldendeInnhold().getLonnstilskuddProsent()).isEqualTo(40);
        harRiktigeEgenskaper(avtale);
        Now.resetClock();
    }

    @Test
    public void sjett_at_reduksjon_ikke_skjer_før_12_mnd_over_68_prosent_eller_høyre(){
        Now.fixedDate(LocalDate.of(2021, 1, 1));
        LocalDate startDato = LocalDate.of(2021, 2, 1);
        LocalDate sluttDato = LocalDate.of(2021, 3, 1);
        Avtale avtale = TestData.enLonnstilskuddAvtaleMedAltUtfylt(Tiltakstype.VARIG_LONNSTILSKUDD);

        EndreAvtale endreAvtale = TestData.endringPåAlleLønnstilskuddFelter();
        endreAvtale.setStartDato(startDato);
        endreAvtale.setSluttDato(sluttDato);
        endreAvtale.setLonnstilskuddProsent(69);

        avtale.endreAvtale(Now.instant(), endreAvtale, Avtalerolle.VEILEDER, avtalerMedTilskuddsperioder);

        assertThat(avtale.getGjeldendeInnhold().getDatoForRedusertProsent()).isNull();
        assertThat(avtale.getGjeldendeInnhold().getLonnstilskuddProsent()).isEqualTo(69);
        harRiktigeEgenskaper(avtale);
        Now.resetClock();
    }

    @Test
    public void sjett_at_reduksjon_ikke_skjer_før_12_mnd_under_68_prosent(){
        Now.fixedDate(LocalDate.of(2021, 1, 1));
        LocalDate startDato = LocalDate.of(2021, 2, 1);
        LocalDate sluttDato = LocalDate.of(2021, 6, 1);
        Avtale avtale = TestData.enLonnstilskuddAvtaleMedAltUtfylt(Tiltakstype.VARIG_LONNSTILSKUDD);

        EndreAvtale endreAvtale = TestData.endringPåAlleLønnstilskuddFelter();
        endreAvtale.setStartDato(startDato);
        endreAvtale.setSluttDato(sluttDato);
        endreAvtale.setLonnstilskuddProsent(35);

        avtale.endreAvtale(Now.instant(), endreAvtale, Avtalerolle.VEILEDER, avtalerMedTilskuddsperioder);

        assertThat(avtale.getGjeldendeInnhold().getDatoForRedusertProsent()).isNull();
        assertThat(avtale.getGjeldendeInnhold().getLonnstilskuddProsent()).isEqualTo(35);
        harRiktigeEgenskaper(avtale);
        Now.resetClock();
    }

    @Test
    public void sjekk_at_reduksjon_skjer_etter_6_mnd_ved_40_prosent() {
        Now.fixedDate(LocalDate.of(2021, 1, 1));
        LocalDate startDato = LocalDate.of(2021, 1, 1);
        LocalDate sluttDato = LocalDate.of(2021, 7, 1);
        Avtale avtale = TestData.enMidlertidigLonnstilskuddAvtaleMedAltUtfylt();
        EndreAvtale endreAvtale = TestData.endringPåAlleLønnstilskuddFelter();
        endreAvtale.setStartDato(startDato);
        endreAvtale.setSluttDato(sluttDato);
        endreAvtale.setLonnstilskuddProsent(40);
        avtale.endreAvtale(Now.instant(), endreAvtale, Avtalerolle.VEILEDER, avtalerMedTilskuddsperioder);

        assertThat(avtale.getGjeldendeInnhold().getDatoForRedusertProsent()).isEqualTo(LocalDate.of(2021, 7, 1));
        harRiktigeEgenskaper(avtale);
        Now.resetClock();
    }

    @Test
    public void sjekk_at_reduksjon_skjer_etter_12_mnd_ved_60_prosent() {
        Now.fixedDate(LocalDate.of(2021,1,1));
        LocalDate startDato = LocalDate.of(2021, 1, 1);
        LocalDate sluttDato = LocalDate.of(2022, 12, 31);
        Avtale avtale = TestData.enMidlertidigLonnstilskuddAvtaleMedAltUtfylt();
        avtale.setKvalifiseringsgruppe(Kvalifiseringsgruppe.SPESIELT_TILPASSET_INNSATS);
        EndreAvtale endreAvtale = TestData.endringPåAlleLønnstilskuddFelter();
        endreAvtale.setStartDato(startDato);
        endreAvtale.setSluttDato(sluttDato);
        endreAvtale.setLonnstilskuddProsent(60);
        avtale.endreAvtale(Now.instant(), endreAvtale, Avtalerolle.VEILEDER, avtalerMedTilskuddsperioder);

        assertThat(avtale.getGjeldendeInnhold().getDatoForRedusertProsent()).isEqualTo(LocalDate.of(2022, 1, 1));
        harRiktigeEgenskaper(avtale);
        Now.resetClock();
    }

    @Test
    public void sjekk_at_ingen_redusering_under_1_år_60_prosent() {
        Now.fixedDate(LocalDate.of(2021, 1, 1));
        LocalDate startDato = LocalDate.of(2021, 1, 1);
        LocalDate sluttDato = LocalDate.of(2021, 12, 31);
        Avtale avtale = TestData.enMidlertidigLonnstilskuddAvtaleMedAltUtfylt();
        avtale.setKvalifiseringsgruppe(Kvalifiseringsgruppe.VARIG_TILPASSET_INNSATS);
        EndreAvtale endreAvtale = TestData.endringPåAlleLønnstilskuddFelter();
        endreAvtale.setStartDato(startDato);
        endreAvtale.setSluttDato(sluttDato);
        endreAvtale.setLonnstilskuddProsent(60);
        avtale.endreAvtale(Now.instant(), endreAvtale, Avtalerolle.VEILEDER, avtalerMedTilskuddsperioder);

        assertThat(avtale.getGjeldendeInnhold().getDatoForRedusertProsent()).isNull();
        harRiktigeEgenskaper(avtale);
        Now.resetClock();
    }

    @Test
    public void sjekk_at_varig_lonnstilskudd_ikke_reduserses() {
        Now.fixedDate(LocalDate.of(2021, 1, 1));
        Avtale avtale = TestData.enLonnstilskuddAvtaleMedAltUtfylt(Tiltakstype.VARIG_LONNSTILSKUDD);
        EndreAvtale endreAvtale = TestData.endringPåAlleLønnstilskuddFelter();
        endreAvtale.setStartDato(LocalDate.of(2021, 1, 1));
        endreAvtale.setSluttDato(LocalDate.of(2031, 1, 1));
        endreAvtale.setLonnstilskuddProsent(60);
        avtale.endreAvtale(Now.instant(), endreAvtale, Avtalerolle.VEILEDER, avtalerMedTilskuddsperioder);

        assertThat(avtale.tilskuddsperiode(avtale.getTilskuddPeriode().size() - 1).getLonnstilskuddProsent()).isEqualTo(60);
        assertThat(avtale.getGjeldendeInnhold().getDatoForRedusertProsent()).isNull();
        Now.resetClock();
    }

    @Test
    public void sjekk_at_avtalen_ikke_annulleres_om_den_har_en_utbetalt_tilskuddsperiode() {
        Avtale avtale = TestData.enLonnstilskuddAvtaleMedAltUtfylt(Tiltakstype.VARIG_LONNSTILSKUDD);
        avtale.tilskuddsperiode(0).setRefusjonStatus(RefusjonStatus.UTBETALT);
        assertThrows(FeilkodeException.class, () -> avtale.annuller(TestData.enVeileder(avtale), ""));
    }

    @Test
    public void sjekk_at_avtalen_ikke_annulleres_om_den_har_en_godkjent_refusjon_på_tilskuddsperiode() {
        Avtale avtale = TestData.enLonnstilskuddAvtaleMedAltUtfylt(Tiltakstype.VARIG_LONNSTILSKUDD);
        avtale.tilskuddsperiode(0).setRefusjonStatus(RefusjonStatus.SENDT_KRAV);
        assertThrows(FeilkodeException.class, () -> avtale.annuller(TestData.enVeileder(avtale), ""));
    }

    @Test
    public void sjekk_at_utbetalte_perioder_beholdes_ved_endring() {
        Avtale avtale = TestData.enLonnstilskuddAvtaleMedAltUtfylt(Tiltakstype.VARIG_LONNSTILSKUDD);
        avtale.tilskuddsperiode(0).setRefusjonStatus(RefusjonStatus.UTBETALT);
        UUID idPåUtbetaltTilskuddsperiode = avtale.tilskuddsperiode(0).getId();
        Integer beløpPåUtbetaltTilskuddsperiode = avtale.tilskuddsperiode(0).getBeløp();

        assertThat(avtale.tilskuddsperiode(0).getRefusjonStatus()).isEqualTo(RefusjonStatus.UTBETALT);
        assertThat(avtale.tilskuddsperiode(0).getId()).isEqualTo(idPåUtbetaltTilskuddsperiode);
        assertThat(avtale.tilskuddsperiode(0).getBeløp()).isEqualTo(beløpPåUtbetaltTilskuddsperiode);
        harRiktigeEgenskaper(avtale);
    }

    @Test
    public void sjekk_at_nye_perioder_ved_forlengelse_starter_etter_utbetalte_perioder() {
        Now.fixedDate(LocalDate.of(2021, 1, 1));
        Avtale avtale = TestData.enMidlertidigLønnstilskuddsAvtaleMedStartOgSluttGodkjentAvAlleParter(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 4, 1));

        avtale.tilskuddsperiode(0).setRefusjonStatus(RefusjonStatus.UTBETALT);
        avtale.tilskuddsperiode(1).setRefusjonStatus(RefusjonStatus.UTBETALT);

        avtale.forlengAvtale(avtale.getGjeldendeInnhold().getSluttDato().plusMonths(3), TestData.enNavIdent());

        assertThat(avtale.tilskuddsperiode(0).getRefusjonStatus()).isEqualTo(RefusjonStatus.UTBETALT);
        assertThat(avtale.tilskuddsperiode(1).getRefusjonStatus()).isEqualTo(RefusjonStatus.UTBETALT);

        assertThat(avtale.tilskuddsperiode(1).getStartDato()).isEqualTo(avtale.tilskuddsperiode(0).getSluttDato().plusDays(1));
        assertThat(avtale.tilskuddsperiode(2).getStatus()).isEqualTo(TilskuddPeriodeStatus.UBEHANDLET);

        harRiktigeEgenskaper(avtale);
        Now.resetClock();
    }

    @Test
    public void sjekk_at_godkjent_perioder_beholdes_ved_endring_som_påvirker_økonomi() {
        Avtale avtale = TestData.enLonnstilskuddAvtaleGodkjentAvVeileder();
        avtale.tilskuddsperiode(0).setStatus(TilskuddPeriodeStatus.GODKJENT);
        Integer beløpFørEndring = avtale.tilskuddsperiode(0).getBeløp();

        EndreTilskuddsberegning endreTilskuddsberegning = EndreTilskuddsberegning.builder()
                .manedslonn(99999)
                .arbeidsgiveravgift(avtale.getGjeldendeInnhold().getArbeidsgiveravgift())
                .feriepengesats(avtale.getGjeldendeInnhold().getFeriepengesats())
                .otpSats(avtale.getGjeldendeInnhold().getOtpSats())
                .build();
        avtale.endreTilskuddsberegning(endreTilskuddsberegning, TestData.enNavIdent());

        assertThat(avtale.tilskuddsperiode(0).getStatus()).isEqualTo(TilskuddPeriodeStatus.GODKJENT);
        assertThat(avtale.tilskuddsperiode(0).getBeløp()).isEqualTo(beløpFørEndring);
        harRiktigeEgenskaper(avtale);
    }

    @Test
    public void sjekk_at_godkjent_periode_annulleres() {
        Avtale avtale = TestData.enLonnstilskuddAvtaleMedAltUtfylt(Tiltakstype.VARIG_LONNSTILSKUDD);
        avtale.tilskuddsperiode(0).setStatus(TilskuddPeriodeStatus.GODKJENT);
        UUID idPåGodkjentTilskuddsperiode = avtale.tilskuddsperiode(0).getId();

        avtale.annuller(TestData.enVeileder(avtale), "");

        assertThat(avtale.tilskuddsperiode(0).getStatus()).isEqualTo(TilskuddPeriodeStatus.ANNULLERT);
        assertThat(avtale.tilskuddsperiode(0).getId()).isEqualTo(idPåGodkjentTilskuddsperiode);
    }

    @Test
    public void sjekk_at_ubehandlet_periode_slettes() {
        Avtale avtale = TestData.enLonnstilskuddAvtaleMedAltUtfylt(Tiltakstype.VARIG_LONNSTILSKUDD);
        avtale.tilskuddsperiode(0).setStatus(TilskuddPeriodeStatus.UBEHANDLET);

        avtale.annuller(TestData.enVeileder(avtale), "");

        assertThat(avtale.getTilskuddPeriode()).isEmpty();
    }

    @Test
    public void sjekk_at_godkjent_periode_annulleres_ved_forlengelse() {
        Now.fixedDate(LocalDate.of(2021, 1, 1));
        LocalDate avtaleFørsteDag = LocalDate.of(2021, 1, 1);
        Avtale avtale = TestData.enMidlertidigLønnstilskuddsAvtaleMedStartOgSluttGodkjentAvAlleParter(avtaleFørsteDag, avtaleFørsteDag);

        avtale.tilskuddsperiode(0).setStatus(TilskuddPeriodeStatus.GODKJENT);
        UUID idPåGodkjentTilskuddsperiode = avtale.tilskuddsperiode(0).getId();

        avtale.forlengAvtale(avtaleFørsteDag.plusDays(1), TestData.enNavIdent());

        assertThat(avtale.tilskuddsperiode(0).getStatus()).isEqualTo(TilskuddPeriodeStatus.ANNULLERT);
        assertThat(avtale.tilskuddsperiode(0).getId()).isEqualTo(idPåGodkjentTilskuddsperiode);
        assertThat(avtale.tilskuddsperiode(0).getStartDato()).isEqualTo(avtaleFørsteDag);
        assertThat(avtale.tilskuddsperiode(0).getSluttDato()).isEqualTo(avtaleFørsteDag);

        assertThat(avtale.tilskuddsperiode(1).getStatus()).isEqualTo(TilskuddPeriodeStatus.UBEHANDLET);
        assertThat(avtale.tilskuddsperiode(1).getStartDato()).isEqualTo(avtaleFørsteDag);
        assertThat(avtale.tilskuddsperiode(1).getSluttDato()).isEqualTo(avtaleFørsteDag.plusDays(1));

        harRiktigeEgenskaper(avtale);
        Now.resetClock();
    }

    @Test
    public void sjekk_at_godkjent_periode_ikke_annulleres_ved_forlengelse_om_den_dekker_hele_måneden() {
        Now.fixedDate(LocalDate.of(2023, 1, 1));
        LocalDate avtaleFørsteDag = LocalDate.of(2023, 1, 1);
        Avtale avtale = TestData.enMidlertidigLønnstilskuddsAvtaleMedStartOgSluttGodkjentAvAlleParter(avtaleFørsteDag, sisteDatoIMnd(avtaleFørsteDag));

        avtale.tilskuddsperiode(0).setStatus(TilskuddPeriodeStatus.GODKJENT);
        UUID idPåGodkjentTilskuddsperiode = avtale.tilskuddsperiode(0).getId();

        avtale.forlengAvtale(sisteDatoIMnd(avtaleFørsteDag).plusDays(2), TestData.enNavIdent());

        assertThat(avtale.tilskuddsperiode(0).getStatus()).isEqualTo(TilskuddPeriodeStatus.GODKJENT);
        assertThat(avtale.tilskuddsperiode(0).getId()).isEqualTo(idPåGodkjentTilskuddsperiode);
        assertThat(avtale.tilskuddsperiode(0).getStartDato()).isEqualTo(avtaleFørsteDag);
        assertThat(avtale.tilskuddsperiode(0).getSluttDato()).isEqualTo(LocalDate.of(2023, 1, 31));

        assertThat(avtale.tilskuddsperiode(1).getStatus()).isEqualTo(TilskuddPeriodeStatus.UBEHANDLET);
        assertThat(avtale.tilskuddsperiode(1).getStartDato()).isEqualTo(LocalDate.of(2023, 2, 1));
        assertThat(avtale.tilskuddsperiode(1).getSluttDato()).isEqualTo(LocalDate.of(2023, 2, 2));

        harRiktigeEgenskaper(avtale);
        Now.resetClock();
    }

    @Test
    public void sjekk_at_godkjent_periode_ikke_annulleres_ved_økonomiendring_i_et_hull() {
        Now.fixedDate(LocalDate.of(2020, 6, 28));
        LocalDate avtaleStart = LocalDate.of(2021, 1, 1);
        LocalDate avtaleSlutt = LocalDate.of(2021, 8, 1);
        Avtale avtale = TestData.enMidlertidigLønnstilskuddsAvtaleMedStartOgSluttGodkjentAvAlleParter(avtaleStart, avtaleSlutt);


        avtale.tilskuddsperiode(0).setStatus(TilskuddPeriodeStatus.GODKJENT);
        avtale.tilskuddsperiode(0).setRefusjonStatus(RefusjonStatus.UTBETALT);
        avtale.tilskuddsperiode(1).setStatus(TilskuddPeriodeStatus.GODKJENT);
        avtale.tilskuddsperiode(2).setRefusjonStatus(RefusjonStatus.UTBETALT);
        avtale.tilskuddsperiode(2).setStatus(TilskuddPeriodeStatus.GODKJENT);

        EndreTilskuddsberegning endreTilskuddsberegning = EndreTilskuddsberegning.builder()
                .manedslonn(77777)
                .arbeidsgiveravgift(avtale.getGjeldendeInnhold().getArbeidsgiveravgift())
                .feriepengesats(avtale.getGjeldendeInnhold().getFeriepengesats())
                .otpSats(avtale.getGjeldendeInnhold().getOtpSats())
                .build();
        avtale.endreTilskuddsberegning(endreTilskuddsberegning, TestData.enNavIdent());

        avtale.tilskuddsperiode(0).setStatus(TilskuddPeriodeStatus.GODKJENT);
        avtale.tilskuddsperiode(0).setRefusjonStatus(RefusjonStatus.UTBETALT);
        avtale.tilskuddsperiode(1).setStatus(TilskuddPeriodeStatus.GODKJENT);
        avtale.tilskuddsperiode(2).setRefusjonStatus(RefusjonStatus.UTBETALT);
        avtale.tilskuddsperiode(2).setStatus(TilskuddPeriodeStatus.GODKJENT);

        harRiktigeEgenskaper(avtale);
        Now.resetClock();
    }

    @Test
    public void genererMaks1MndTilskuddsperiode() {
        Now.fixedDate(LocalDate.of(2021, 1, 01));
        LocalDate avtaleStart = LocalDate.of(2021, 1, 1);
        LocalDate avtaleSlutt = LocalDate.of(2021, 6, 2);
        Avtale avtale = TestData.enMidlertidigLønnstilskuddsAvtaleMedStartOgSluttGodkjentAvAlleParter(avtaleStart, avtaleSlutt);

        avtale.getTilskuddPeriode().forEach(tilskuddPeriode -> {
            LocalDate fra = tilskuddPeriode.getStartDato();
            LocalDate til = tilskuddPeriode.getSluttDato();
            // fra og til dato er i samme måned
            assertThat(fra.getMonth()).isEqualTo(til.getMonth());
            // Det er maks 1 måneds lengde på tilskuddsperiodene
            int dagerITilskuddsperiode = fra.until(til).getDays() + 1;
            int dagerIMåneden = fra.lengthOfMonth();
            assertThat(dagerITilskuddsperiode).isLessThanOrEqualTo(dagerIMåneden);
        });
        Now.resetClock();
    }

    @Test
    public void splittVedMånedsskifte() {
        Now.fixedDate(LocalDate.of(2021, 1, 1));
        LocalDate avtaleStart = LocalDate.of(2021, 1, 20);
        LocalDate avtaleSlutt = LocalDate.of(2022, 3, 2);
        Avtale avtale = TestData.enMidlertidigLønnstilskuddsAvtaleMedStartOgSluttGodkjentAvAlleParter(avtaleStart, avtaleSlutt);

        avtale.getTilskuddPeriode().forEach(tilskuddPeriode -> {
            // start- og sluttdato er alltid i samme måned
            assertThat(tilskuddPeriode.getStartDato().getMonth()).isEqualTo(tilskuddPeriode.getSluttDato().getMonth());
        });


        Now.resetClock();
    }

    /* ------------ Metoder som kun brukes innad i denne test-klassen ------------ */
    private void harRiktigeEgenskaper(Avtale avtale) {
        harOverlappendeDatoer(avtale.getTilskuddPeriode());
        harAlleDageneIAvtalenperioden(avtale.getTilskuddPeriode(), avtale.getGjeldendeInnhold().getStartDato(), avtale.getGjeldendeInnhold().getSluttDato());
        harRiktigeLøpenumre(avtale.getTilskuddPeriode());
    }

    private void harAlleDageneIAvtalenperioden(Collection<TilskuddPeriode> tilskuddPerioder, LocalDate avtaleStart, LocalDate avtaleSlutt) {
        long antallDager = avtaleStart.until(avtaleSlutt.plusDays(1), ChronoUnit.DAYS);
        Set<LocalDate> dateSet = new HashSet<>();
        List<TilskuddPeriode> tilskuddsperioderUtenAnnullerte = tilskuddPerioder.stream().filter(tilskuddPeriode -> tilskuddPeriode.getStatus() != TilskuddPeriodeStatus.ANNULLERT).collect(Collectors.toList());
        for (TilskuddPeriode periode : tilskuddsperioderUtenAnnullerte) {
            Set<LocalDate> localDates = periode.getStartDato().datesUntil(periode.getSluttDato().plusDays(1)).collect(Collectors.toSet());
            dateSet.addAll(localDates);
        }
        if (dateSet.size() != antallDager) {
            fail("Ulikt antall dager i avtalen og tilskuddsperiodene");
        }

    }

    private void harOverlappendeDatoer(Collection<TilskuddPeriode> tilskuddPerioder) {
        Set<LocalDate> dateSet = new HashSet<>();
        List<TilskuddPeriode> tilskuddsperioderUtenAnnullerte = tilskuddPerioder.stream().filter(tilskuddPeriode -> tilskuddPeriode.getStatus() != TilskuddPeriodeStatus.ANNULLERT).collect(Collectors.toList());
        for (TilskuddPeriode periode : tilskuddsperioderUtenAnnullerte) {
            Set<LocalDate> localDates = periode.getStartDato().datesUntil(periode.getSluttDato().plusDays(1)).collect(Collectors.toSet());

            if (!Collections.disjoint(dateSet, localDates)) {
                fail("Det finnes overlappende datoer");
            } else {
                dateSet.addAll(localDates);
            }
        }
    }

    private void harRiktigeLøpenumre(Collection<TilskuddPeriode> tilskuddPerioder) {
        int løpenummer = 1;
        for (TilskuddPeriode tilskuddPeriode : tilskuddPerioder) {
            assertThat(tilskuddPeriode.getLøpenummer()).isEqualTo(løpenummer++);
        }
    }


    /* ------------ Tester av metoder som kun brukes innad i denne test-klassen ------------ */

    @Test
    public void sjekk_at_ikkeoverlappende_periode_ikke_har_overlappende_datoer() {
        TilskuddPeriode tilskuddPeriode1 = new TilskuddPeriode(1000, LocalDate.of(2021, 1, 1), LocalDate.of(2021, 3, 31), 60);
        TilskuddPeriode tilskuddPeriode2 = new TilskuddPeriode(1000, LocalDate.of(2021, 4, 1), LocalDate.of(2021, 6, 1), 60);
        harOverlappendeDatoer(new TreeSet<>(Set.of(tilskuddPeriode1, tilskuddPeriode2)));
    }

    @Test
    public void sjekk_at_har_overlappende_datoer() {
        TilskuddPeriode tilskuddPeriode1 = new TilskuddPeriode(1000, LocalDate.of(2021, 1, 1), LocalDate.of(2021, 3, 31), 60);
        TilskuddPeriode tilskuddPeriode2 = new TilskuddPeriode(1000, LocalDate.of(2021, 3, 31), LocalDate.of(2021, 6, 1), 60);
        assertThatThrownBy(() -> harOverlappendeDatoer(List.of(tilskuddPeriode1, tilskuddPeriode2))).isInstanceOf(AssertionError.class);
    }

    @Test
    public void sjekk_at_alle_dagene_i_avtalen_er_i_tilskuddsperiodene() {
        TilskuddPeriode tilskuddPeriode1 = new TilskuddPeriode(1000, LocalDate.of(2021, 1, 1), LocalDate.of(2021, 3, 31), 60);
        TilskuddPeriode tilskuddPeriode2 = new TilskuddPeriode(1000, LocalDate.of(2021, 4, 1), LocalDate.of(2021, 6, 1), 60);
        harAlleDageneIAvtalenperioden(List.of(tilskuddPeriode1, tilskuddPeriode2), LocalDate.of(2021, 1, 1), LocalDate.of(2021, 6, 1));
    }

    @Test
    public void sjekk_at_det_feiler_nar_ikke_alle_dagene_i_avtalen_er_i_tilskuddsperiodene() {
        TilskuddPeriode tilskuddPeriode1 = new TilskuddPeriode(1000, LocalDate.of(2021, 1, 1), LocalDate.of(2021, 3, 31), 60);
        TilskuddPeriode tilskuddPeriode2 = new TilskuddPeriode(1000, LocalDate.of(2021, 4, 1), LocalDate.of(2021, 5, 25), 60);
        assertThatThrownBy(() -> harAlleDageneIAvtalenperioden(List.of(tilskuddPeriode1, tilskuddPeriode2), LocalDate.of(2021, 1, 1), LocalDate.of(2021, 5, 26))).isInstanceOf(AssertionError.class);
    }

}