package no.nav.tag.tiltaksgjennomforing.avtale;

import no.nav.tag.tiltaksgjennomforing.utils.Now;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

public class AvtaleArenaMigreringTest {

    @Test
    public void lonnstilskudd_tilskuddsperioder_skal_ha_status_ubehandlet_hvis_ikke_ryddeavtale() {
        Now.fixedDate(LocalDate.of(2023, 02, 15));
        Avtale avtale = TestData.enMidlertidigLønnstilskuddsAvtaleMedStartOgSluttGodkjentAvAlleParter(LocalDate.of(2022, 05, 01), LocalDate.of(2023, 04,30));
        assertThat(avtale.getTilskuddPeriode()).isNotEmpty();

        avtale.getTilskuddPeriode().forEach(tilskuddPeriode -> {
            assertThat(tilskuddPeriode.getStatus()).isEqualTo(TilskuddPeriodeStatus.UBEHANDLET);
        });
        Now.resetClock();
    }

    @Test
    public void lonnstilskudd_skal_generere_tilskuddsperioder_med_behandlet_status_om_ryddeavtale() {
        Now.fixedDate(LocalDate.of(2023, 02, 15));
        Avtale avtale = TestData.enMidlertidigLønnstilskuddsRyddeAvtaleMedStartOgSluttGodkjentAvAlleParter(LocalDate.of(2022, 05, 01), LocalDate.of(2023, 04,30));
        assertThat(avtale.getTilskuddPeriode()).isNotEmpty();

        avtale.getTilskuddPeriode().forEach(tilskuddPeriode -> {

        });

        TilskuddPeriode sisteBehandletIArena = avtale.getTilskuddPeriode().stream().filter(tilskuddPeriode -> tilskuddPeriode.getStartDato().isAfter(LocalDate.of(2011, 12, 31))).findFirst().get();
        assertThat(sisteBehandletIArena.getStatus()).isEqualTo(TilskuddPeriodeStatus.BEHANDLET_I_ARENA);
        TilskuddPeriode førsteUbehandlet = avtale.getTilskuddPeriode().stream().filter(tilskuddPeriode -> tilskuddPeriode.getStartDato().isAfter(LocalDate.of(2023, 01, 01))).findFirst().get();
        assertThat(førsteUbehandlet.getStatus()).isEqualTo(TilskuddPeriodeStatus.UBEHANDLET);

        Now.resetClock();
    }
}
