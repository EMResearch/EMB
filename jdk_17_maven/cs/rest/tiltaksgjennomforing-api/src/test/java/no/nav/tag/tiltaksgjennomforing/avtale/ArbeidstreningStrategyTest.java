package no.nav.tag.tiltaksgjennomforing.avtale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static no.nav.tag.tiltaksgjennomforing.avtale.AvtaleInnhold.Fields.*;
import static org.assertj.core.api.Assertions.assertThat;

class ArbeidstreningStrategyTest {

    private AvtaleInnhold avtaleInnhold;
    private AvtaleInnholdStrategy strategy;

    @BeforeEach
    private void setUp() {
        avtaleInnhold = new AvtaleInnhold();
        strategy = AvtaleInnholdStrategyFactory.create(avtaleInnhold, Tiltakstype.ARBEIDSTRENING);
    }

    @Test
    void test_at_felter_relevante_i_arbeidstrening_kan_endres() {
        strategy.endre(TestData.endringPåAlleArbeidstreningFelter());

        // Test for collections
        assertThat(avtaleInnhold.getMaal()).isNotEmpty();

        // Test for ikke-collections
        assertThat(avtaleInnhold).extracting(
                deltakerFornavn,
                deltakerEtternavn,
                deltakerTlf,
                bedriftNavn,
                arbeidsgiverFornavn,
                arbeidsgiverEtternavn,
                arbeidsgiverTlf,
                veilederFornavn,
                veilederEtternavn,
                veilederTlf,
                stillingstittel,
                arbeidsoppgaver,
                stillingprosent,
                antallDagerPerUke,
                startDato,
                sluttDato,
                tilrettelegging,
                oppfolging,
                stillingKonseptId,
                stillingStyrk08
        ).filteredOn(Objects::isNull).isEmpty();
    }
}