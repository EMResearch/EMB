package no.nav.tag.tiltaksgjennomforing.okonomi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import no.nav.tag.tiltaksgjennomforing.Miljø;
import no.nav.tag.tiltaksgjennomforing.exceptions.KontoregisterFantIkkeBedriftFeilException;
import no.nav.tag.tiltaksgjennomforing.exceptions.KontoregisterFeilException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles({ Miljø.LOCAL, "wiremock"})
@DirtiesContext
public class KontoregisterServiceImplTest {
    @Autowired
    private KontoregisterService KontoregisterService;

    @Test
    public void hentKontonummer__skal_returnere_verdi_fra_kall() {
        String kontonummerTilbake = KontoregisterService.hentKontonummer("990983666");
        assertThat(kontonummerTilbake).isEqualTo("10000008162");
    }

    @Test
    public void hentKontonummer__skal_returnere_fant_ikke_bedrift_feilmelding() {
        assertThatThrownBy(() ->  KontoregisterService.hentKontonummer("111234567"))
            .isInstanceOf(KontoregisterFantIkkeBedriftFeilException.class);
    }

    @Test
    public void hentKontonummer__skal_returnere_feilmelding() {
        assertThatThrownBy(() ->  KontoregisterService.hentKontonummer("777333333"))
            .isInstanceOf(KontoregisterFeilException.class);
    }
}
