package no.nav.tag.tiltaksgjennomforing.avtale;

import static no.nav.tag.tiltaksgjennomforing.avtale.Tiltakstype.MIDLERTIDIG_LONNSTILSKUDD;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import no.nav.tag.tiltaksgjennomforing.enhet.Kvalifiseringsgruppe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BeregningLonnstilskuddTest {

    private AvtaleInnhold avtaleInnhold;
    private AvtaleInnholdStrategy strategy;

    @BeforeEach
    public void setUp() {
        avtaleInnhold = new AvtaleInnhold();
        strategy = AvtaleInnholdStrategyFactory.create(avtaleInnhold, MIDLERTIDIG_LONNSTILSKUDD);
    }

    @Test
    public void test_regn_ut_sumLonntilskudd_til_0_nar_lonnstilskudd_prosent_er_null() {
        // GIVEN
        EndreAvtale endreAvtale = new EndreAvtale();
        endreAvtale.setManedslonn(20000);
        endreAvtale.setFeriepengesats(new BigDecimal(0.102));
        endreAvtale.setArbeidsgiveravgift(new BigDecimal(0.064));

        // WHEN
        strategy.endre(endreAvtale);

        // THEN
        assertThat(avtaleInnhold.getSumLonnstilskudd()).isNull();
    }

    @Test
    public void test_regn_ut_sumLonntilskudd_nå_rotp_ikke_er_satt() {
        // GIVEN
        EndreAvtale endreAvtale = new EndreAvtale();
        endreAvtale.setManedslonn(20000);
        endreAvtale.setLonnstilskuddProsent(60);
        endreAvtale.setOtpSats(0.02);
        endreAvtale.setFeriepengesats(new BigDecimal(0.12));
        endreAvtale.setArbeidsgiveravgift(new BigDecimal(0.141));

        // WHEN
        strategy.endreAvtaleInnholdMedKvalifiseringsgruppe(endreAvtale, Kvalifiseringsgruppe.VARIG_TILPASSET_INNSATS);

        // THEN
        assertThat(avtaleInnhold.getSumLonnstilskudd()).isEqualTo(15642);
    }

    @Test
    public void test_regn_ut_sumLonntilskudd() {
        // GIVEN
        EndreAvtale endreAvtale = new EndreAvtale();
        endreAvtale.setManedslonn(20000);
        endreAvtale.setOtpSats(0.02);
        endreAvtale.setLonnstilskuddProsent(60);
        endreAvtale.setFeriepengesats(new BigDecimal(0.12));
        endreAvtale.setArbeidsgiveravgift(new BigDecimal(0.141));

        // WHEN
        strategy.endreAvtaleInnholdMedKvalifiseringsgruppe(endreAvtale, Kvalifiseringsgruppe.VARIG_TILPASSET_INNSATS);

        // THEN
        assertThat(avtaleInnhold.getSumLonnstilskudd()).isEqualTo(15642);
    }

    @Test
    public void test_regn_ut_arbeidsgiveravgiftbelop() {
        // GIVEN
        EndreAvtale endreAvtale = new EndreAvtale();
        endreAvtale.setManedslonn(20000);
        endreAvtale.setOtpSats(0.02);
        endreAvtale.setFeriepengesats(new BigDecimal(0.12));
        endreAvtale.setArbeidsgiveravgift(new BigDecimal(0.141));

        // WHEN
        strategy.endre(endreAvtale);

        // THEN
        assertThat(avtaleInnhold.getArbeidsgiveravgiftBelop()).isEqualTo(3222);
    }

    @Test
    public void test_regn_ut_oblig_tjenstepensjon() {
        // GIVEN
        EndreAvtale endreAvtale = new EndreAvtale();
        endreAvtale.setManedslonn(20000);
        endreAvtale.setOtpSats(0.02);
        endreAvtale.setFeriepengesats(new BigDecimal(0.12));

        // WHEN
        strategy.endre(endreAvtale);

        // THEN
        assertThat(avtaleInnhold.getOtpBelop()).isEqualTo(448);
    }

    @Test
    public void test_regn_ut_oblig_tjenstepensjon_til_null_om_Feriepengersats_er_null() {
        // GIVEN
        EndreAvtale endreAvtale = TestData.endringPåAlleLønnstilskuddFelter();
        endreAvtale.setFeriepengesats(null);

        // WHEN
        strategy.endre(endreAvtale);

        // THEN
        assertThat(avtaleInnhold.getOtpBelop()).isNull();
        assertThat(avtaleInnhold.getFeriepengerBelop()).isNull();
    }

    @Test
    public void test_regn_ut_oblig_tjenstepensjon_til_null_om_manedslonn_er_null() {
        // GIVEN
        EndreAvtale endreAvtale = TestData.endringPåAlleLønnstilskuddFelter();
        endreAvtale.setManedslonn(null);

        // WHEN
        strategy.endre(endreAvtale);

        // THEN
        assertThat(avtaleInnhold.getOtpBelop()).isNull();
        assertThat(avtaleInnhold.getFeriepengerBelop()).isNull();
    }

    @Test
    public void test_regn_ut_feriepenger_belop() {
        // GIVEN
        EndreAvtale endreAvtale = new EndreAvtale();
        endreAvtale.setManedslonn(20000);
        endreAvtale.setFeriepengesats(new BigDecimal(0.12));

        // WHEN
        strategy.endre(endreAvtale);

        // THEN
        assertThat(avtaleInnhold.getFeriepengerBelop()).isEqualTo(2400);
    }

    @Test
    public void test_regn_ut_lonn_ved_100_prosent() {
        // GIVEN
        EndreAvtale endreAvtale = new EndreAvtale();
        endreAvtale.setStillingprosent(50);
        endreAvtale.setManedslonn(10000);
        endreAvtale.setOtpSats(0.02);
        endreAvtale.setFeriepengesats(new BigDecimal(0.125));
        endreAvtale.setArbeidsgiveravgift(new BigDecimal(0.0));
        // WHEN
        strategy.endre(endreAvtale);

        // THEN
        assertThat(avtaleInnhold.getManedslonn100pst()).isEqualTo(22950);
    }

}
