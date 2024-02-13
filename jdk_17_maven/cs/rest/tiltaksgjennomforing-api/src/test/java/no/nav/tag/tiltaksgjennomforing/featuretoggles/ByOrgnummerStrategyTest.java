package no.nav.tag.tiltaksgjennomforing.featuretoggles;

import io.getunleash.UnleashContext;
import no.nav.arbeidsgiver.altinnrettigheter.proxy.klient.model.AltinnReportee;
import no.nav.tag.tiltaksgjennomforing.autorisasjon.altinntilgangsstyring.AltinnTilgangsstyringService;
import no.nav.tag.tiltaksgjennomforing.avtale.Fnr;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ByOrgnummerStrategyTest {

    private UnleashContext unleashContext = UnleashContext.builder().userId("12345678901").build();

    @Mock
    AltinnTilgangsstyringService altinnTilgangsstyringService;

    @Test
    public void skal_være_enablet_hvis_bruker_tilhører_organisasjon() {
        Fnr fnr = new Fnr("12345678901");
        Set<AltinnReportee> orgSet = new HashSet<>();
        orgSet.add(new AltinnReportee("", "AS", null, "999999999", "", ""));

        when(altinnTilgangsstyringService.hentAltinnOrganisasjoner(eq(fnr), any())).thenReturn(orgSet);
        assertThat(new ByOrgnummerStrategy(altinnTilgangsstyringService).isEnabled(Map.of(ByOrgnummerStrategy.UNLEASH_PARAMETER_ORGNUMRE, "999999999"), unleashContext)).isTrue();
    }

    @Test
    public void skal_være_disablet_hvis_bruker_ikke_tilhører_organisasjon() {
        Fnr fnr = new Fnr("12345678901");
        Set<AltinnReportee> orgSet = new HashSet<>();
        orgSet.add(new AltinnReportee("", "AS", null, "999999998", "", ""));

        when(altinnTilgangsstyringService.hentAltinnOrganisasjoner(fnr, () -> "")).thenReturn(orgSet);
        assertThat(new ByOrgnummerStrategy(altinnTilgangsstyringService).isEnabled(Map.of(ByOrgnummerStrategy.UNLEASH_PARAMETER_ORGNUMRE, "999999999"), unleashContext)).isFalse();
    }

    @Test
    public void navIdent_skal_returnere_false() {
        UnleashContext unleashContext = UnleashContext.builder().userId("J154200").build();
        Set<AltinnReportee> orgSet = new HashSet<>();
        orgSet.add(new AltinnReportee("", "AS", null, "999999998", "", ""));
        assertThat(new ByOrgnummerStrategy(altinnTilgangsstyringService).isEnabled(Map.of(ByOrgnummerStrategy.UNLEASH_PARAMETER_ORGNUMRE, "999999999"), unleashContext)).isFalse();
        verify(altinnTilgangsstyringService, never()).hentAltinnOrganisasjoner(any(), any());
    }

    @Test
    public void byOrgnummmer_strategy_håndterer_flere_orgnummer() {
        Fnr fnr = new Fnr("12345678901");
        Set<AltinnReportee> orgSet = new HashSet<>();
        orgSet.add(new AltinnReportee("", "AS", null, "999999999", "", ""));

        when(altinnTilgangsstyringService.hentAltinnOrganisasjoner(eq(fnr), any())).thenReturn(orgSet);
        assertThat(new ByOrgnummerStrategy(altinnTilgangsstyringService).isEnabled(Map.of(ByOrgnummerStrategy.UNLEASH_PARAMETER_ORGNUMRE, "910825526,999999999"), unleashContext)).isTrue();
    }

    @Test
    public void skal_være_disablet_hvis_feil_ved_oppslag_i_altinn() {
        Fnr fnr = new Fnr("12345678901");
        Set<AltinnReportee> orgSet = new HashSet<>();
        orgSet.add(new AltinnReportee("", "AS", null, "999999998", "", ""));

        when(altinnTilgangsstyringService.hentAltinnOrganisasjoner(fnr, () -> "")).thenThrow(RuntimeException.class);
        assertThat(new ByOrgnummerStrategy(altinnTilgangsstyringService).isEnabled(Map.of(ByOrgnummerStrategy.UNLEASH_PARAMETER_ORGNUMRE, "999999999"), unleashContext)).isFalse();
    }

}
