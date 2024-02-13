package no.nav.tag.tiltaksgjennomforing.avtale;

import static no.nav.tag.tiltaksgjennomforing.avtale.Tiltakstype.VARIG_LONNSTILSKUDD;
import static org.assertj.core.api.Assertions.assertThat;

import no.nav.tag.tiltaksgjennomforing.AssertFeilkode;
import no.nav.tag.tiltaksgjennomforing.avtale.RefusjonKontaktperson.Fields;
import no.nav.tag.tiltaksgjennomforing.exceptions.Feilkode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VarigLonnstilskuddStrategyTest {

    private AvtaleInnhold avtaleInnhold;
    private AvtaleInnholdStrategy strategy;

    @BeforeEach
    public void setUp() {
        avtaleInnhold = new AvtaleInnhold();
        strategy = AvtaleInnholdStrategyFactory.create(avtaleInnhold, VARIG_LONNSTILSKUDD);
    }

    @Test
    void test_at_feil_når_familietilknytning_ikke_er_fylt_ut() {
        EndreAvtale endreAvtale = TestData.endringPåAlleLønnstilskuddFelter();
        endreAvtale.setHarFamilietilknytning(true);
        endreAvtale.setFamilietilknytningForklaring(null);
        strategy.endre(endreAvtale);

        assertThat(strategy.alleFelterSomMåFyllesUt()).containsKey(AvtaleInnhold.Fields.familietilknytningForklaring);
    }

    @Test
    void test_at_ikke_feil_når_ikke_forklaring_og_nei_på_familietilknytning() {
        EndreAvtale endreAvtale = TestData.endringPåAlleLønnstilskuddFelter();
        endreAvtale.setHarFamilietilknytning(false);
        endreAvtale.setFamilietilknytningForklaring(null);
        strategy.endre(endreAvtale);

        assertThat(strategy.alleFelterSomMåFyllesUt()).doesNotContainKey(AvtaleInnhold.Fields.familietilknytningForklaring);
    }

    @Test
    void test_at_ikke_feil_når_alt_fylt_ut_og_har_familietilknytning() {
        EndreAvtale endreAvtale = TestData.endringPåAlleLønnstilskuddFelter();
        endreAvtale.setHarFamilietilknytning(true);
        endreAvtale.setFamilietilknytningForklaring("En god forklaring");
        strategy.endre(endreAvtale);
    }

    @Test
    public void sjekk_riktig_otp_sats() {
        strategy = AvtaleInnholdStrategyFactory.create(avtaleInnhold, VARIG_LONNSTILSKUDD);
        EndreAvtale endreAvtale = TestData.endringPåAlleLønnstilskuddFelter();
        endreAvtale.setOtpSats(0.301);
        AssertFeilkode.assertFeilkode(Feilkode.FEIL_OTP_SATS, () -> strategy.endre(endreAvtale));
        endreAvtale.setOtpSats(-0.001);
        AssertFeilkode.assertFeilkode(Feilkode.FEIL_OTP_SATS, () -> strategy.endre(endreAvtale));

        endreAvtale.setOtpSats(0.0);
        strategy.endre(endreAvtale);

        endreAvtale.setOtpSats(null);
        strategy.endre(endreAvtale);

        endreAvtale.setOtpSats(0.3);
        strategy.endre(endreAvtale);
    }

    @Test
    public void sjekk_riktig_refusjon_kontakt_person_må_fylles_ut() {
        strategy = AvtaleInnholdStrategyFactory.create(avtaleInnhold, VARIG_LONNSTILSKUDD);
        EndreAvtale endreAvtale = TestData.endringPåAlleLønnstilskuddFelter();
        endreAvtale.setRefusjonKontaktperson(new RefusjonKontaktperson(null, "Duck","12345678", true));
        strategy.endre(endreAvtale);
        assertThat(strategy.alleFelterSomMåFyllesUt()).extractingByKey(Fields.refusjonKontaktpersonFornavn).isNull();
    }

    @Test
    void lonnstilskuddsprosent_må_fylles_ut() {
        EndreAvtale endreAvtale = TestData.endringPåAlleLønnstilskuddFelter();
        endreAvtale.setLonnstilskuddProsent(null);
        strategy.endre(endreAvtale);
        assertThat(strategy.alleFelterSomMåFyllesUt()).extractingByKey(AvtaleInnhold.Fields.lonnstilskuddProsent).isNull();
    }
}