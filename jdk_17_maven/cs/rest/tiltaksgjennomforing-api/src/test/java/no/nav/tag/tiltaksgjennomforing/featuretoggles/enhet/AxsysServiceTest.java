package no.nav.tag.tiltaksgjennomforing.featuretoggles.enhet;

import no.nav.tag.tiltaksgjennomforing.Miljø;
import no.nav.tag.tiltaksgjennomforing.avtale.NavIdent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles({ Miljø.LOCAL, "wiremock" })
@DirtiesContext
public class AxsysServiceTest {
    @Autowired
    private AxsysService axsysService;

    @Test
    public void hentEnheter__returnerer_riktige_enheter() {
        List<NavEnhet> enheter = axsysService.hentEnheterNavAnsattHarTilgangTil(new NavIdent("X123456"));
        assertThat(enheter).containsOnly(new NavEnhet("0906", "NAV Storebyen"), new NavEnhet("0904", "NAV Lillebyen"));
    }

    @Test
    public void hentEnheter__ugyldig_ident_skal_ikke_ha_enheter() {
        List<NavEnhet> enheter = axsysService.hentEnheterNavAnsattHarTilgangTil(new NavIdent("X999999"));
        assertThat(enheter).isEmpty();
    }

    @Test
    public void enheter__inneholder_hentetEnheter() {
        List<NavEnhet> enheter = axsysService.hentEnheterNavAnsattHarTilgangTil(new NavIdent("X123456"));
        List<NavEnhet> pilotEnheter = Collections.singletonList(new NavEnhet("0906", "NAV Storebyen"));
        assertThat(pilotEnheter).containsAnyElementsOf(enheter);
    }

}
