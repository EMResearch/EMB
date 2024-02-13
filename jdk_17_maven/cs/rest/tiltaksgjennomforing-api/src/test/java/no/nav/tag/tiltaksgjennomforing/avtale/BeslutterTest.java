package no.nav.tag.tiltaksgjennomforing.avtale;

import static no.nav.tag.tiltaksgjennomforing.AssertFeilkode.assertFeilkode;
import static no.nav.tag.tiltaksgjennomforing.avtale.TestData.avtalerMedTilskuddsperioder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import no.nav.tag.tiltaksgjennomforing.autorisasjon.SlettemerkeProperties;
import no.nav.tag.tiltaksgjennomforing.autorisasjon.abac.TilgangskontrollService;
import no.nav.tag.tiltaksgjennomforing.enhet.Norg2Client;
import no.nav.tag.tiltaksgjennomforing.enhet.VeilarbArenaClient;
import no.nav.tag.tiltaksgjennomforing.exceptions.Feilkode;
import no.nav.tag.tiltaksgjennomforing.featuretoggles.enhet.AxsysService;
import no.nav.tag.tiltaksgjennomforing.featuretoggles.enhet.NavEnhet;
import no.nav.tag.tiltaksgjennomforing.persondata.PersondataService;

import org.junit.jupiter.api.Test;

class BeslutterTest {

    private TilgangskontrollService tilgangskontrollService = mock(TilgangskontrollService.class);
    private AvtaleRepository avtaleRepository = mock(AvtaleRepository.class);
    private AxsysService axsysService = mock(AxsysService.class);
    private Norg2Client norg2Client = mock(Norg2Client.class);

    /*@Test
    public void hentAlleAvtalerMedMuligTilgang__hent_ingen_GODKJENTE_når_avtaler_har_gjeldende_tilskuddsperiodestatus_ubehandlet() {

        // GITT
        Avtale avtale = TestData.enMidlertidigLonnstilskuddAvtaleMedAltUtfylt();
        TilskuddPeriode tilskuddPeriode = new TilskuddPeriode();
        tilskuddPeriode.setStatus(TilskuddPeriodeStatus.GODKJENT);
        tilskuddPeriode.setBeløp(1200);
        tilskuddPeriode.setAvtale(avtale);
        avtale.setTilskuddPeriode(new TreeSet<>(List.of(tilskuddPeriode)));
        Beslutter beslutter = new Beslutter(new NavIdent("J987654"), tilgangskontrollService, axsysService, norg2Client);
        Integer plussDato = ((int) ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.now().plusMonths(3)));
        AvtalePredicate avtalePredicate = new AvtalePredicate();
        avtalePredicate.setTilskuddPeriodeStatus(TilskuddPeriodeStatus.UBEHANDLET);
        Pageable pageable = PageRequest.of(0, 100);

        Page<AvtaleMinimal> avtalerMedTilskuddsperioder = avtaleRepository
                .finnGodkjenteAvtalerMedTilskuddsperiodestatusOgNavEnheterUbehandletMinimal(TilskuddPeriodeStatus.UBEHANDLET.name(), Set.of(TestData.ENHET_OPPFØLGING.getVerdi(), plussDato, Set.of(Tiltakstype.MIDLERTIDIG_LONNSTILSKUDD.name(), "", pageable);


        // NÅR
        when(axsysService.hentEnheterNavAnsattHarTilgangTil(beslutter.getIdentifikator())).thenReturn(List.of(TestData.ENHET_OPPFØLGING));
        when(avtaleRepository
                .finnGodkjenteAvtalerMedTilskuddsperiodestatusOgNavEnheterUbehandletMinimal(TilskuddPeriodeStatus.UBEHANDLET.name(), Set.of(TestData.ENHET_OPPFØLGING.getVerdi()), plussDato, Set.of(Tiltakstype.MIDLERTIDIG_LONNSTILSKUDD.name()), "", pageable))
                .thenReturn(new PageImpl<AvtaleMinimal>(List.of(avtale)));
        List<Avtale> avtaler = beslutter.hentAlleAvtalerMedMuligTilgang(avtaleRepository, avtalePredicate);

        assertThat(avtaler).isEmpty();
    }*/

    @Test
    public void toggle_godkjent_for_etterregistrering() {

        //GITT
        Avtale avtale = Avtale.veilederOppretterAvtale(new OpprettAvtale(TestData.etFodselsnummer(), TestData.etBedriftNr(), Tiltakstype.MIDLERTIDIG_LONNSTILSKUDD), TestData.enNavIdent());
        EndreAvtale endreAvtale = TestData.endringPåAlleLønnstilskuddFelter();
        endreAvtale.setStartDato(LocalDate.of(2021, 12, 12));
        endreAvtale.setSluttDato(LocalDate.of(2021, 12, 1).plusYears(1));
        Beslutter beslutter = TestData.enBeslutter(avtale);

        // NÅR
        beslutter.setOmAvtalenKanEtterregistreres(avtale);
        assertThat(avtale.isGodkjentForEtterregistrering()).isTrue();

        beslutter.setOmAvtalenKanEtterregistreres(avtale);
        assertThat(avtale.isGodkjentForEtterregistrering()).isFalse();
    }

    @Test
    public void kan_ikke_godkjenne_periode_på_enhet_som_ikke_finnes() {
        Avtale avtale = TestData.enMidlertidigLonnstilskuddAvtaleMedAltUtfylt();
        Arbeidsgiver arbeidsgiver = TestData.enArbeidsgiver(avtale);

        // Gi veileder tilgang til deltaker
        TilgangskontrollService tilgangskontrollService = mock(TilgangskontrollService.class);

        Veileder veileder = new Veileder(
                avtale.getVeilederNavIdent(),
                tilgangskontrollService,
                mock(PersondataService.class),
                mock(Norg2Client.class),
                Set.of(new NavEnhet("4802", "Trysil")),
                mock(SlettemerkeProperties.class),
                false,
                mock(VeilarbArenaClient.class));

        when(tilgangskontrollService.harSkrivetilgangTilKandidat(eq(veileder), any(Fnr.class)))
                .thenReturn(true);

        avtale.endreAvtale(
                Instant.now(),
                TestData.endringPåAlleLønnstilskuddFelter(),
                Avtalerolle.VEILEDER,
                avtalerMedTilskuddsperioder
        );
        arbeidsgiver.godkjennAvtale(Instant.now(), avtale);
        veileder.godkjennForVeilederOgDeltaker(TestData.enGodkjentPaVegneGrunn(), avtale);
        assertThat(avtale.erAvtaleInngått()).isFalse();
        Beslutter beslutter = TestData.enBeslutter(avtale);
        assertFeilkode(
                Feilkode.ENHET_FINNES_IKKE,
                () -> beslutter.godkjennTilskuddsperiode(avtale, "9999")
        );
        assertThat(avtale.erAvtaleInngått()).isFalse();
    }
}