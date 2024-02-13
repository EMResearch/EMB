package no.nav.tag.tiltaksgjennomforing.featuretoggles.enhet;

import io.getunleash.UnleashContext;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;

import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static no.nav.tag.tiltaksgjennomforing.featuretoggles.enhet.ByEnhetStrategy.PARAM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

public class ByEnhetStrategyTest {

    private AxsysService axsysService = mock(AxsysService.class);
    private UnleashContext unleashContext = UnleashContext.builder().userId("X123456").build();

    @Test
    public void skal_være_disablet_hvis_innlogget_med_fødselsnummer() {
        assertThat(new ByEnhetStrategy(axsysService).isEnabled(Map.of(PARAM, "1234"), UnleashContext.builder().userId("00000000000").build())).isEqualTo(false);
    }

    @Test
    public void skal_være_disablet_hvis_det_toggle_evalueres_uten_kontekst() {
        assertThat(new ByEnhetStrategy(axsysService).isEnabled(Map.of(PARAM, "1234"))).isEqualTo(false);
    }
    
    @Test
    public void skal_være_disablet_hvis_det_ikke_finnes_bruker_i_konteksten() {
        assertThat(new ByEnhetStrategy(axsysService).isEnabled(Map.of(PARAM, "1234"), UnleashContext.builder().build())).isFalse();
    }
    
    @Test
    public void skal_være_disablet_hvis_det_ikke_finnes_definerte_enheter() {
        assertThat(new ByEnhetStrategy(axsysService).isEnabled(emptyMap(), unleashContext)).isFalse();
        assertThat(new ByEnhetStrategy(axsysService).isEnabled(singletonMap(PARAM, null), unleashContext)).isFalse(); //Map.of() tåler ikke null
        assertThat(new ByEnhetStrategy(axsysService).isEnabled(Map.of(PARAM, ""), unleashContext)).isFalse();
    }

    @Test
    public void skal_være_disablet_hvis_bruker_ikke_har_definerte_enheter() {
        assertThat(new ByEnhetStrategy(axsysService).isEnabled(Map.of(PARAM, "1234"), unleashContext)).isFalse();
    }

    @Test
    public void skal_være_disablet_hvis_bruker_har_definerte_enheter_men_ingen_er_i_listen() {
        when(axsysService.hentEnheterNavAnsattHarTilgangTil(any())).thenReturn(newArrayList(new NavEnhet("1111", "Bergen"), new NavEnhet("2222", "Stavanger")));
        assertThat(new ByEnhetStrategy(axsysService).isEnabled(Map.of(PARAM, "1234"), unleashContext)).isFalse();
    }
    
    @Test
    public void skal_være_enablet_hvis_bruker_har_definert_enhet() {
        when(axsysService.hentEnheterNavAnsattHarTilgangTil(any())).thenReturn(newArrayList(new NavEnhet("1234", "Lillehammer")));
        assertThat(new ByEnhetStrategy(axsysService).isEnabled(Map.of(PARAM, "1234"), unleashContext)).isTrue();
    }

    @Test
    public void skal_være_enablet_hvis_en_av_brukers_enheter_er_i_listen() {
        when(axsysService.hentEnheterNavAnsattHarTilgangTil(any())).thenReturn(newArrayList(new NavEnhet("1111", "Bergen"), new NavEnhet("1234", "Lillehammer")));
        assertThat(new ByEnhetStrategy(axsysService).isEnabled(Map.of(PARAM, "1234,5678"), unleashContext)).isTrue();
    }
    
    @Test
    public void skal_kaste_exception_hvis_axsys_kaster_exception() {
        when(axsysService.hentEnheterNavAnsattHarTilgangTil(any())).thenThrow(new RestClientException("mock exception"));
        assertThatThrownBy(() -> new ByEnhetStrategy(axsysService).isEnabled(Map.of(PARAM, "1234"), unleashContext)).isInstanceOf(RestClientException.class);
    }
}
