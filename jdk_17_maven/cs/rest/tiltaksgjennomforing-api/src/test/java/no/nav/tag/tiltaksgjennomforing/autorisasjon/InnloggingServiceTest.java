package no.nav.tag.tiltaksgjennomforing.autorisasjon;

import static no.nav.tag.tiltaksgjennomforing.AssertFeilkode.assertFeilkode;
import static no.nav.tag.tiltaksgjennomforing.avtale.TestData.ENHET_OPPFØLGING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import no.nav.tag.tiltaksgjennomforing.autorisasjon.TokenUtils.Issuer;
import no.nav.tag.tiltaksgjennomforing.autorisasjon.altinntilgangsstyring.AltinnTilgangsstyringService;
import no.nav.tag.tiltaksgjennomforing.autorisasjon.altinntilgangsstyring.ArbeidsgiverTokenStrategyFactoryImpl;
import no.nav.tag.tiltaksgjennomforing.avtale.Avtalerolle;
import no.nav.tag.tiltaksgjennomforing.avtale.Fnr;
import no.nav.tag.tiltaksgjennomforing.avtale.TestData;
import no.nav.tag.tiltaksgjennomforing.exceptions.Feilkode;
import no.nav.tag.tiltaksgjennomforing.exceptions.TilgangskontrollException;
import no.nav.tag.tiltaksgjennomforing.featuretoggles.enhet.AxsysService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class InnloggingServiceTest {

    @InjectMocks
    private InnloggingService innloggingService;

    @Mock
    private TokenUtils tokenUtils;

    @Mock
    private AltinnTilgangsstyringService altinnTilgangsstyringService;

    @Mock
    private AxsysService axsysService;

    @Mock
    private SystembrukerProperties systembrukerProperties;

    @Mock
    private BeslutterAdGruppeProperties beslutterAdGruppeProperties;

    @Mock
    private ArbeidsgiverTokenStrategyFactoryImpl arbeidsgiverTokenStrategyFactory;


    @Test
    public void hentInnloggetBruker__er_selvbetjeningbruker() {
        InnloggetDeltaker selvbetjeningBruker = TestData.enInnloggetDeltaker();
        værInnloggetDeltaker(selvbetjeningBruker);
        assertThat(innloggingService.hentInnloggetBruker(Avtalerolle.DELTAKER)).isEqualTo(selvbetjeningBruker);
    }

    @Test
    public void hentInnloggetBruker__er_selvbetjeningbruker_mentor() {
        InnloggetMentor selvbetjeningMentor = TestData.enInnloggetMentor();
        værInnloggetMentor(selvbetjeningMentor);
        assertThat(innloggingService.hentInnloggetBruker(Avtalerolle.MENTOR)).isEqualTo(selvbetjeningMentor);
    }

    @Test
    public void hentInnloggetBruker__selvbetjeningbruker_type_arbeidsgiver_skal_hente_organisasjoner() {
        InnloggetArbeidsgiver selvbetjeningBruker = new InnloggetArbeidsgiver(new Fnr("11111111111"), Set.of(), Map.of());
        when(altinnTilgangsstyringService.hentTilganger(eq((Fnr) selvbetjeningBruker.getIdentifikator()), any())).thenReturn(Map.of());
        when(altinnTilgangsstyringService.hentAltinnOrganisasjoner(eq((Fnr) selvbetjeningBruker.getIdentifikator()), any())).thenReturn(Set.of());
        værInnloggetArbeidsgiver(selvbetjeningBruker);

       when(arbeidsgiverTokenStrategyFactory.create(Issuer.ISSUER_TOKENX)).thenReturn(() -> "");

        assertThat(innloggingService.hentInnloggetBruker(Optional.of(Avtalerolle.ARBEIDSGIVER).get())).isEqualTo(selvbetjeningBruker);
        verify(altinnTilgangsstyringService).hentTilganger(eq((Fnr) selvbetjeningBruker.getIdentifikator()), any());
        verify(altinnTilgangsstyringService).hentAltinnOrganisasjoner(eq((Fnr) selvbetjeningBruker.getIdentifikator()), any());
    }

    @Test
    public void hentInnloggetBruker__er_nav_ansatt_og_har_enhet() {
        InnloggetVeileder navAnsatt = TestData.enInnloggetVeileder();
        when(axsysService.hentEnheterNavAnsattHarTilgangTil(any())).thenReturn(List.of(ENHET_OPPFØLGING));
        værInnloggetVeileder(navAnsatt);

        assertThat(innloggingService.hentInnloggetBruker(Avtalerolle.VEILEDER)).isEqualTo(navAnsatt);
    }

    @Test
    public void hentInnloggetBruker__er_nav_ansatt_og_beslutter() {
        InnloggetBeslutter navAnsatt = TestData.enInnloggetBeslutter();
        when(axsysService.hentEnheterNavAnsattHarTilgangTil(any())).thenReturn(List.of(ENHET_OPPFØLGING));
        værInnloggetBeslutter(navAnsatt);
        assertThat(innloggingService.hentInnloggetBruker(Avtalerolle.BESLUTTER)).isEqualTo(navAnsatt);
    }

    @Test
    public void hentInnloggetNavAnsatt__er_selvbetjeningbruker() {
        værInnloggetArbeidsgiver(TestData.enInnloggetArbeidsgiver());
        assertFeilkode(Feilkode.UGYLDIG_KOMBINASJON_AV_ISSUER_OG_ROLLE, innloggingService::hentInnloggetVeileder);
    }

    @Test
    public void hentInnloggetBruker__er_uinnlogget() {
        when(tokenUtils.hentBrukerOgIssuer()).thenReturn(Optional.empty());
        assertThatThrownBy(innloggingService::hentInnloggetVeileder).isExactlyInstanceOf(TilgangskontrollException.class);
    }

    @Test
    public void avviser_selvbetjeningBruker_som_systemBruker() {
        værInnloggetArbeidsgiver(TestData.enInnloggetArbeidsgiver());
        assertThatThrownBy(innloggingService::validerSystembruker).isExactlyInstanceOf(TilgangskontrollException.class);
    }

    @Test
    public void avviser_navAnsatt_som_systemBruker() {
        værInnloggetVeileder(TestData.enInnloggetVeileder());
        assertThatThrownBy(innloggingService::validerSystembruker).isExactlyInstanceOf(TilgangskontrollException.class);
    }

    @Test
    public void avviser_ukjent_systemBruker() {
        værInnloggetSystem("ukjent");
        assertThatThrownBy(innloggingService::validerSystembruker).isExactlyInstanceOf(TilgangskontrollException.class);
    }

    @Test
    public void godtar_forventet_systemBruker() {
        værInnloggetSystem("forventet");
        innloggingService.validerSystembruker();
    }

    private void værInnloggetSystem(String systemId) {
        when(tokenUtils.hentBrukerOgIssuer()).thenReturn(Optional.of(new TokenUtils.BrukerOgIssuer(Issuer.ISSUER_SYSTEM, systemId)));
        when(systembrukerProperties.getId()).thenReturn("forventet");
    }

    private void værInnloggetDeltaker(InnloggetDeltaker bruker) {
        when(tokenUtils.hentBrukerOgIssuer())
                .thenReturn(Optional.of(new TokenUtils.BrukerOgIssuer(Issuer.ISSUER_TOKENX, bruker.getIdentifikator().asString())));
    }

    private void værInnloggetMentor(InnloggetMentor mentor) {
        when(tokenUtils.hentBrukerOgIssuer())
                .thenReturn(Optional.of(new TokenUtils.BrukerOgIssuer(Issuer.ISSUER_TOKENX, mentor.getIdentifikator().asString())));
    }

    private void værInnloggetArbeidsgiver(InnloggetArbeidsgiver bruker) {
        when(tokenUtils.hentBrukerOgIssuer())
                .thenReturn(Optional.of(new TokenUtils.BrukerOgIssuer(Issuer.ISSUER_TOKENX, bruker.getIdentifikator().asString())));
    }

    private void værInnloggetVeileder(InnloggetVeileder navAnsatt) {
        when(tokenUtils.hentBrukerOgIssuer())
                .thenReturn(Optional.of(new TokenUtils.BrukerOgIssuer(Issuer.ISSUER_AAD, navAnsatt.getIdentifikator().asString())));
    }

    private void værInnloggetBeslutter(InnloggetBeslutter navAnsatt) {
        when(tokenUtils.harAdGruppe(any())).thenReturn(true);
        when(tokenUtils.hentBrukerOgIssuer())
                .thenReturn(Optional.of(new TokenUtils.BrukerOgIssuer(Issuer.ISSUER_AAD, navAnsatt.getIdentifikator().asString())));
    }


}
