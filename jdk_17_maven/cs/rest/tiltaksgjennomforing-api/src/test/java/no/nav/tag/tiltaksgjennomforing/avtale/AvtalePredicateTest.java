package no.nav.tag.tiltaksgjennomforing.avtale;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class AvtalePredicateTest {

    private static Tiltakstype annenTiltakstypeEnnPåAvtale(Avtale avtale) {
        for (Tiltakstype t : Tiltakstype.values()) {
            if (!t.equals(avtale.getTiltakstype())) {
                return t;
            }
        }
        return null;
    }

    private static Status annenStatusEnnPåAvtale(Avtale avtale) {
        for (Status s : Status.values()) {
            if (!s.equals(avtale.getTiltakstype())) {
                return s;
            }
        }
        return null;
    }

    @Test
    void veileder_nav_ident_oppgitt() {
        Avtale avtale = TestData.enArbeidstreningAvtale();
        AvtalePredicate query = new AvtalePredicate();
        query.setVeilederNavIdent(avtale.getVeilederNavIdent());
        assertThat(query.test(avtale)).isTrue();
    }

    @Test
    void deltaker_oppgitt() {
        Avtale avtale = TestData.enArbeidstreningAvtale();
        AvtalePredicate query = new AvtalePredicate();
        query.setDeltakerFnr(avtale.getDeltakerFnr());
        assertThat(query.test(avtale)).isTrue();
    }

    @Test
    void enhet_oppfølgning_oppgitt() {
        Avtale avtale = TestData.enArbeidstreningAvtaleOpprettetAvArbeidsgiverOgErUfordeltMedOppfølgningsEnhet();
        AvtalePredicate query = new AvtalePredicate();
        query.setNavEnhet(TestData.ENHET_OPPFØLGING.getVerdi());
        assertThat(query.test(avtale)).isTrue();
    }

    @Test
    void enhet_geografisk_oppgitt() {
        Avtale avtale = TestData.enArbeidstreningAvtaleOpprettetAvArbeidsgiverOgErUfordeltMedGeografiskEnhet();
        AvtalePredicate query = new AvtalePredicate();
        query.setNavEnhet(TestData.ENHET_GEOGRAFISK.getVerdi());
        assertThat(query.test(avtale)).isTrue();
    }

    @Test
    void bedrift_oppgitt() {
        Avtale avtale = TestData.enArbeidstreningAvtale();
        AvtalePredicate query = new AvtalePredicate();
        query.setBedriftNr(avtale.getBedriftNr());
        assertThat(query.test(avtale)).isTrue();
    }

    @Test
    void tiltakstype_oppgitt() {
        Avtale avtale = TestData.enArbeidstreningAvtale();
        AvtalePredicate query = new AvtalePredicate();
        query.setTiltakstype(avtale.getTiltakstype());
        assertThat(query.test(avtale)).isTrue();
    }

    @Test
    void tiltakstype_annen_type() {
        Avtale avtale = TestData.enArbeidstreningAvtale();
        AvtalePredicate query = new AvtalePredicate();
        query.setTiltakstype(annenTiltakstypeEnnPåAvtale(avtale));
        assertThat(query.test(avtale)).isFalse();
    }

    @Test
    void status_oppgitt() {
        Avtale avtale = TestData.enArbeidstreningAvtale();
        AvtalePredicate query = new AvtalePredicate();
        query.setStatus(avtale.statusSomEnum());
        assertThat(query.test(avtale)).isTrue();
    }

    @Test
    void status_annen_type() {
        Avtale avtale = TestData.enArbeidstreningAvtale();
        AvtalePredicate query = new AvtalePredicate();
        query.setStatus(annenStatusEnnPåAvtale(avtale));
        assertThat(query.test(avtale)).isFalse();
    }

    @Test
    void kombinasjon_med_feil() {
        Avtale avtale = TestData.enArbeidstreningAvtale();
        AvtalePredicate query = new AvtalePredicate();
        query.setVeilederNavIdent(avtale.getVeilederNavIdent());
        query.setBedriftNr(avtale.getBedriftNr());
        query.setDeltakerFnr(new Fnr("66666666666"));
        assertThat(query.test(avtale)).isFalse();
    }

    @Test
    void ingenting_oppgitt() {
        AvtalePredicate query = new AvtalePredicate();
        assertThat(query.test(TestData.enArbeidstreningAvtale())).isTrue();
    }

    @Test
    void  avtaleNr_oppgitt() {
        Avtale avtale = TestData.enArbeidstreningsAvtaleMedGittAvtaleNr();
        AvtalePredicate query = new AvtalePredicate();
        query.setAvtaleNr(TestData.ET_AVTALENR);
        assertThat(query.test(avtale)).isTrue();
    }

    @Test
    void ignorerer_ugyldig_felt_i_prediacate() {
        FilterSok filterSok = new FilterSok();
        filterSok.setQueryParametre("""
                {"avtaleNr": 1, "avtaleTulleNummer": 1337}
                """);
        AvtalePredicate avtalePredicate = filterSok.getAvtalePredicate();
        assertThat(avtalePredicate.getAvtaleNr()).isEqualTo(1);
    }

    @Test
    void ignorerer_ugyldig_json_i_prediacate() {
        FilterSok filterSok = new FilterSok();
        filterSok.setQueryParametre("""
                {
                """);
        AvtalePredicate avtalePredicate = filterSok.getAvtalePredicate();
        assertThat(avtalePredicate).isEqualTo(new AvtalePredicate());
    }
}