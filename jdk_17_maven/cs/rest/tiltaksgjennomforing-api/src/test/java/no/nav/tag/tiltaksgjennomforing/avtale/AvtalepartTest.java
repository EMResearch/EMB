package no.nav.tag.tiltaksgjennomforing.avtale;

import static no.nav.tag.tiltaksgjennomforing.AssertFeilkode.assertFeilkode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.EnumSet;

import no.nav.tag.tiltaksgjennomforing.exceptions.ArbeidsgiverSkalGodkjenneFørVeilederException;
import no.nav.tag.tiltaksgjennomforing.exceptions.Feilkode;
import no.nav.tag.tiltaksgjennomforing.exceptions.KanIkkeEndreException;
import no.nav.tag.tiltaksgjennomforing.exceptions.SamtidigeEndringerException;
import no.nav.tag.tiltaksgjennomforing.utils.Now;
import org.junit.jupiter.api.Test;

public class AvtalepartTest {

    @Test
    public void endreAvtale__skal_feile_for_deltaker() {
        Avtale avtale = TestData.enAvtaleMedAltUtfylt();
        Deltaker deltaker = TestData.enDeltaker(avtale);
        assertThatThrownBy(() -> deltaker.endreAvtale(avtale.getSistEndret(), TestData.ingenEndring(), avtale, EnumSet.of(avtale.getTiltakstype()))).isInstanceOf(KanIkkeEndreException.class);
    }

    @Test
    public void godkjennForVeilederOgDeltaker__skal_feile_hvis_ag_ikke_har_godkjent() {
        Avtale avtale = TestData.enAvtaleMedAltUtfylt();
        Veileder veileder = TestData.enVeileder(avtale);
        GodkjentPaVegneGrunn godkjentPaVegneGrunn = TestData.enGodkjentPaVegneGrunn();
        assertThatThrownBy(() -> veileder.godkjennForVeilederOgDeltaker(godkjentPaVegneGrunn, avtale)).isInstanceOf(ArbeidsgiverSkalGodkjenneFørVeilederException.class);
    }

    @Test
    public void godkjennForVeileder__skal_feile_hvis_mentor_ikke_har_signert() {
        Avtale avtale = TestData.enMentorAvtaleUsignert();
        Arbeidsgiver arbeidsgiver = TestData.enArbeidsgiver(avtale);
        arbeidsgiver.godkjennAvtale(avtale.getSistEndret().plusMillis(60000), avtale);
        Deltaker deltaker = TestData.enDeltaker(avtale);
        deltaker.godkjennAvtale(avtale.getSistEndret().plusMillis(60000), avtale);
        Veileder veileder = TestData.enVeileder(avtale);
        assertFeilkode(Feilkode.MENTOR_MÅ_SIGNERE_TAUSHETSERKLÆRING,() -> veileder.godkjennAvtale(avtale.getSistEndret().plusMillis(60000), avtale));
    }

    @Test
    public void godkjennForVeilederOgDeltaker__skal_feile_hvis_mentor_ikke_har_signert() {
        Avtale avtale = TestData.enMentorAvtaleUsignert();
        Arbeidsgiver arbeidsgiver = TestData.enArbeidsgiver(avtale);
        arbeidsgiver.godkjennAvtale(avtale.getSistEndret().plusMillis(60000), avtale);
        Veileder veileder = TestData.enVeileder(avtale);
        assertFeilkode(Feilkode.MENTOR_MÅ_SIGNERE_TAUSHETSERKLÆRING,() -> veileder.godkjennForVeilederOgDeltaker(new GodkjentPaVegneGrunn(), avtale));
    }

    @Test
    public void godkjennForVeilederOgDeltaker__skal_fungere_for_veileder() {
        Avtale avtale = TestData.enAvtaleMedAltUtfylt();
        Arbeidsgiver arbeidsgiver = TestData.enArbeidsgiver(avtale);
        arbeidsgiver.godkjennForAvtalepart(avtale);
        Veileder veileder = TestData.enVeileder(avtale);
        GodkjentPaVegneGrunn godkjentPaVegneGrunn = TestData.enGodkjentPaVegneGrunn();
        veileder.godkjennForVeilederOgDeltaker(godkjentPaVegneGrunn, avtale);
        assertThat(avtale.erGodkjentAvDeltaker()).isTrue();
        assertThat(avtale.erGodkjentAvVeileder()).isTrue();
        assertThat(avtale.getGjeldendeInnhold().isGodkjentPaVegneAv()).isTrue();
    }

    @Test
    public void endreAvtale__skal_fungere_for_arbeidsgiver() {
        Avtale avtale = TestData.enAvtaleMedAltUtfylt();
        Arbeidsgiver arbeidsgiver = TestData.enArbeidsgiver(avtale);
        arbeidsgiver.endreAvtale(Now.instant(), TestData.ingenEndring(), avtale, EnumSet.of(avtale.getTiltakstype()));
    }

    @Test
    public void endreAvtale__skal_fungere_for_veileder() {
        Avtale avtale = TestData.enAvtaleMedAltUtfylt();
        Veileder veileder = TestData.enVeileder(avtale);
        veileder.endreAvtale(Now.instant(), TestData.ingenEndring(), avtale, EnumSet.of(avtale.getTiltakstype()));
    }

    @Test
    public void godkjennForAvtalepart__skal_ikke_fungere_hvis_versjon_er_feil() {
        Avtale avtale = TestData.enAvtaleMedAltUtfylt();
        Deltaker deltaker = TestData.enDeltaker(avtale);
        assertThatThrownBy(() -> deltaker.godkjennAvtale(avtale.getSistEndret().minusMillis(1), avtale)).isInstanceOf(SamtidigeEndringerException.class);
    }

    @Test
    public void godkjennForAvtalepart__skal_fungere_for_deltaker() {
        Avtale avtale = TestData.enAvtaleMedAltUtfylt();
        Deltaker deltaker = TestData.enDeltaker(avtale);
        deltaker.godkjennAvtale(avtale.getSistEndret(), avtale);
        assertThat(avtale.erGodkjentAvDeltaker()).isTrue();
        assertThat(avtale.erGodkjentAvArbeidsgiver()).isFalse();
        assertThat(avtale.erGodkjentAvVeileder()).isFalse();
    }

    @Test
    public void godkjennForAvtalepart__skal_fungere_for_arbeidsgiver() {
        Avtale avtale = TestData.enAvtaleMedAltUtfylt();
        Arbeidsgiver arbeidsgiver = TestData.enArbeidsgiver(avtale);
        arbeidsgiver.godkjennAvtale(avtale.getSistEndret(), avtale);
        assertThat(avtale.erGodkjentAvArbeidsgiver()).isTrue();
        assertThat(avtale.erGodkjentAvVeileder()).isFalse();
        assertThat(avtale.erGodkjentAvDeltaker()).isFalse();
    }

    @Test
    public void godkjennForAvtalepart__skal_fungere_for_veileder() {
        Avtale avtale = TestData.enAvtaleMedAltUtfylt();
        avtale.getGjeldendeInnhold().setGodkjentAvDeltaker(Now.localDateTime());
        avtale.getGjeldendeInnhold().setGodkjentAvArbeidsgiver(Now.localDateTime());
        Veileder veileder = TestData.enVeileder(avtale);
        veileder.godkjennAvtale(avtale.getSistEndret(), avtale);
        assertThat(avtale.erGodkjentAvArbeidsgiver()).isTrue();
    }

    @Test
    public void opphevGodkjenninger__veileder_skal_kunne_trekke_tilbake_egen_godkjenning() {
        Avtale avtale = TestData.enAvtaleMedAltUtfylt();
        avtale.getGjeldendeInnhold().setGodkjentAvVeileder(Now.localDateTime());
        Veileder veileder = TestData.enVeileder(avtale);
        veileder.opphevGodkjenninger(avtale);
        assertThat(avtale.erGodkjentAvVeileder()).isFalse();
    }

    @Test
    public void opphevGodkjenninger__feiler_hvis_alle_har_allerede_godkjent_og_avtale_er_inngått() {
        Avtale avtale = TestData.enAvtaleMedAltUtfylt();
        Arbeidsgiver arbeidsgiver = TestData.enArbeidsgiver(avtale);
        arbeidsgiver.godkjennForAvtalepart(avtale);
        Veileder veileder = TestData.enVeileder(avtale);
        GodkjentPaVegneGrunn godkjentPaVegneGrunn = TestData.enGodkjentPaVegneGrunn();
        veileder.godkjennForVeilederOgDeltaker(godkjentPaVegneGrunn, avtale);
        assertThat(avtale.erGodkjentAvDeltaker()).isTrue();
        assertThat(avtale.erGodkjentAvVeileder()).isTrue();
        assertThat(avtale.erGodkjentAvArbeidsgiver()).isTrue();
        assertThat(avtale.getGjeldendeInnhold().isGodkjentPaVegneAv()).isTrue();
        assertFeilkode(Feilkode.KAN_IKKE_OPPHEVE_GODKJENNINGER_VED_INNGAATT_AVTALE, () -> veileder.opphevGodkjenninger(avtale));
    }

    @Test
    public void opphevGodkjenninger__feiler_hvis_ingen_har_godkjent() {
        Avtale avtale = TestData.enAvtaleMedAltUtfylt();
        Veileder veileder = TestData.enVeileder(avtale);
        assertFeilkode(Feilkode.KAN_IKKE_OPPHEVE, () -> veileder.opphevGodkjenninger(avtale));
    }

    @Test
    public void opphevGodkjenninger__kan_ikke_utfores_flere_ganger_etter_hverandre() {
        Avtale avtale = TestData.enAvtaleMedAltUtfylt();
        Arbeidsgiver arbeidsgiver = TestData.enArbeidsgiver(avtale);
        arbeidsgiver.godkjennForAvtalepart(avtale);

        arbeidsgiver.opphevGodkjenninger(avtale);
        assertFeilkode(Feilkode.KAN_IKKE_OPPHEVE, () -> arbeidsgiver.opphevGodkjenninger(avtale));
    }
}