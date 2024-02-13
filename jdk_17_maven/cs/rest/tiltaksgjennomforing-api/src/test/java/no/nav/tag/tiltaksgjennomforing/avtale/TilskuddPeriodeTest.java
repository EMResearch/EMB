package no.nav.tag.tiltaksgjennomforing.avtale;

import static no.nav.tag.tiltaksgjennomforing.AssertFeilkode.assertFeilkode;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.EnumSet;
import no.nav.tag.tiltaksgjennomforing.exceptions.Feilkode;
import no.nav.tag.tiltaksgjennomforing.utils.Now;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class TilskuddPeriodeTest {
    @Test
    void behandle_flere_ganger__etter_godkjenning() {
        TilskuddPeriode tilskuddPeriode = TestData.enTilskuddPeriode();
        tilskuddPeriode.godkjenn(TestData.enNavIdent(), TestData.ENHET_GEOGRAFISK.getVerdi());
        assertFeilkode(Feilkode.TILSKUDDSPERIODE_ER_ALLEREDE_BEHANDLET, () -> tilskuddPeriode.godkjenn(TestData.enNavIdent(), TestData.ENHET_GEOGRAFISK.getVerdi()));
        assertFeilkode(Feilkode.TILSKUDDSPERIODE_ER_ALLEREDE_BEHANDLET, () -> tilskuddPeriode.avslå(TestData.enNavIdent(), EnumSet.of(Avslagsårsak.FEIL_I_FAKTA), "Faktafeil"));
    }

    @Test
    void behandle_flere_ganger__etter_avslag() {
        TilskuddPeriode tilskuddPeriode = TestData.enTilskuddPeriode();
        tilskuddPeriode.avslå(TestData.enNavIdent(), EnumSet.of(Avslagsårsak.FEIL_I_FAKTA), "Faktafeil");
        assertFeilkode(Feilkode.TILSKUDDSPERIODE_ER_ALLEREDE_BEHANDLET, () -> tilskuddPeriode.godkjenn(TestData.enNavIdent(), TestData.ENHET_GEOGRAFISK.getVerdi()));
        assertFeilkode(Feilkode.TILSKUDDSPERIODE_ER_ALLEREDE_BEHANDLET, () -> tilskuddPeriode.avslå(TestData.enNavIdent(), EnumSet.of(Avslagsårsak.FEIL_I_FAKTA), "Faktafeil"));
    }

    @Test
    void godkjenn_setter_riktige_felter() {
        TilskuddPeriode tilskuddPeriode = TestData.enTilskuddPeriode();
        NavIdent beslutter = TestData.enNavIdent();
        tilskuddPeriode.godkjenn(beslutter, TestData.ENHET_GEOGRAFISK.getVerdi());
        assertThat(tilskuddPeriode.getGodkjentAvNavIdent()).isEqualTo(beslutter);
        assertThat(tilskuddPeriode.getGodkjentTidspunkt()).isNotNull();
        assertThat(tilskuddPeriode.getStatus()).isEqualTo(TilskuddPeriodeStatus.GODKJENT);
    }

    @Test
    void avslå_setter_riktige_felter() {
        TilskuddPeriode tilskuddPeriode = TestData.enTilskuddPeriode();
        NavIdent beslutter = TestData.enNavIdent();
        tilskuddPeriode.avslå(beslutter, EnumSet.of(Avslagsårsak.FEIL_I_FAKTA, Avslagsårsak.ANNET), "Feil i fakta");
        assertThat(tilskuddPeriode.getAvslåttAvNavIdent()).isEqualTo(beslutter);
        assertThat(tilskuddPeriode.getAvslåttTidspunkt()).isNotNull();
        assertThat(tilskuddPeriode.getStatus()).isEqualTo(TilskuddPeriodeStatus.AVSLÅTT);
        assertThat(tilskuddPeriode.getAvslagsårsaker()).contains(Avslagsårsak.FEIL_I_FAKTA, Avslagsårsak.ANNET);
    }

    @Test
    void avslå__uten_årsaker() {
        TilskuddPeriode tilskuddPeriode = TestData.enTilskuddPeriode();
        NavIdent beslutter = TestData.enNavIdent();
        assertFeilkode(Feilkode.TILSKUDDSPERIODE_INGEN_AVSLAGSAARSAKER, () -> tilskuddPeriode.avslå(beslutter, EnumSet.noneOf(Avslagsårsak.class), "Feil i fakta"));
    }

    @Test
    void avslå__uten_forklaring() {
        TilskuddPeriode tilskuddPeriode = TestData.enTilskuddPeriode();
        NavIdent beslutter = TestData.enNavIdent();
        assertFeilkode(Feilkode.TILSKUDDSPERIODE_AVSLAGSFORKLARING_PAAKREVD, () -> tilskuddPeriode.avslå(beslutter, EnumSet.of(Avslagsårsak.FEIL_I_REGELFORSTÅELSE), "   "));
    }

    @Test
    void sjekker_utbetalt_status() {
        TilskuddPeriode tilskuddPeriode = TestData.enTilskuddPeriode();
        tilskuddPeriode.setRefusjonStatus(RefusjonStatus.UTBETALT);
        assertThat(tilskuddPeriode.erUtbetalt()).isTrue();
    }

    @Test
    @Disabled("Tester kun midlertidig sperre for å ikke kunne godkjenne tilskudd for neste år.")
    void godkjenn__skal_ikke_kunne_godkjenne_neste_års_tilskuddsperiode() {
        //TODO: Dette er en test av en midlertidig sperre.
        Now.fixedDate(LocalDate.of(2022, 10, 15));
        TilskuddPeriode tilskuddPeriode = TestData.enTilskuddPeriode();
        tilskuddPeriode.setStartDato(LocalDate.of(2023, 1, 1));
        tilskuddPeriode.setSluttDato(LocalDate.of(2023, 1, 31));

        assertFeilkode(Feilkode.TILSKUDDSPERIODE_BEHANDLE_FOR_TIDLIG, () -> tilskuddPeriode.godkjenn(TestData.enNavIdent(), TestData.ENHET_GEOGRAFISK.getVerdi()));

        Now.fixedDate(LocalDate.of(2022, 12, 15));
        assertFeilkode(Feilkode.TILSKUDDSPERIODE_BEHANDLE_FOR_TIDLIG, () -> tilskuddPeriode.godkjenn(TestData.enNavIdent(), TestData.ENHET_GEOGRAFISK.getVerdi()));

        Now.fixedDate(LocalDate.of(2023, 1, 1));
        tilskuddPeriode.godkjenn(TestData.enNavIdent(), TestData.ENHET_GEOGRAFISK.getVerdi());

        Now.resetClock();
    }
}