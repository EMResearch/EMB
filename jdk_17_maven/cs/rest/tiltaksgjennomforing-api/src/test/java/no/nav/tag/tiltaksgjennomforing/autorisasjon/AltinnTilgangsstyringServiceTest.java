package no.nav.tag.tiltaksgjennomforing.autorisasjon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import no.nav.arbeidsgiver.altinnrettigheter.proxy.klient.model.AltinnReportee;
import no.nav.tag.tiltaksgjennomforing.Miljø;
import no.nav.tag.tiltaksgjennomforing.autorisasjon.altinntilgangsstyring.AltinnTilgangsstyringProperties;
import no.nav.tag.tiltaksgjennomforing.autorisasjon.altinntilgangsstyring.AltinnTilgangsstyringService;
import no.nav.tag.tiltaksgjennomforing.avtale.BedriftNr;
import no.nav.tag.tiltaksgjennomforing.avtale.Fnr;
import no.nav.tag.tiltaksgjennomforing.avtale.Tiltakstype;
import no.nav.tag.tiltaksgjennomforing.exceptions.AltinnFeilException;
import no.nav.tag.tiltaksgjennomforing.exceptions.TiltaksgjennomforingException;
import no.nav.tag.tiltaksgjennomforing.featuretoggles.FeatureToggleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles({ Miljø.LOCAL, "wiremock" })
@DirtiesContext
public class AltinnTilgangsstyringServiceTest {
    @Autowired
    private AltinnTilgangsstyringService altinnTilgangsstyringService;

    @MockBean
    private TokenUtils tokenUtils;

    @MockBean
    private FeatureToggleService featureToggleService;

    @BeforeEach
    public void setUp() {
        // when(tokenUtils.hentSelvbetjeningToken()).thenReturn("token");
        when(featureToggleService.isEnabled(anyString())).thenReturn(false);
    }

    @Test
    public void hentOrganisasjoner__gyldig_fnr_en_bedrift_på_hvert_tiltak() {
        Fnr fnr = new Fnr("10000000000");
        Map<BedriftNr, Collection<Tiltakstype>> tilganger = altinnTilgangsstyringService.hentTilganger(fnr, () -> "");
        Set<AltinnReportee> organisasjoner = altinnTilgangsstyringService.hentAltinnOrganisasjoner(fnr, () -> "");

        // Alt som finnes i tilganger-mappet skal også finnes i organisasjoner-settet
        assertThat(organisasjoner).extracting(org -> new BedriftNr(org.getOrganizationNumber())).containsAll(tilganger.keySet());

        // Sjekk at uvesentilg tilgang er med i organisasjoner
        assertThat(organisasjoner).extracting(AltinnReportee::getOrganizationNumber).contains("980712306", "910825555");


        // Parents skal ikke være i tilgang-map
        assertThat(tilganger).doesNotContainKeys(new BedriftNr("910825550"), new BedriftNr("910825555"));

        // Virksomheter skal være i tilgang-map

        assertThat(tilganger.get(new BedriftNr("999999999"))).containsOnly(Tiltakstype.ARBEIDSTRENING, Tiltakstype.MIDLERTIDIG_LONNSTILSKUDD, Tiltakstype.VARIG_LONNSTILSKUDD, Tiltakstype.SOMMERJOBB, Tiltakstype.MENTOR, Tiltakstype.INKLUDERINGSTILSKUDD);

        assertThat(tilganger.get(new BedriftNr("910712314"))).containsOnly(Tiltakstype.MIDLERTIDIG_LONNSTILSKUDD);
        assertThat(tilganger.get(new BedriftNr("910712306"))).containsOnly(Tiltakstype.VARIG_LONNSTILSKUDD);

        // Ingen tilganger på ingen tiltak
        assertThat(tilganger).doesNotContainKeys(new BedriftNr("980712306"), new BedriftNr("980825560"));

    }

    @Test
    public void hentOrganisasjoner__tilgang_bare_for_arbeidstrening() {
        Fnr fnr = new Fnr("20000000000");
        Map<BedriftNr, Collection<Tiltakstype>> tilganger = altinnTilgangsstyringService.hentTilganger(fnr, () -> "");
        Set<AltinnReportee> organisasjoner = altinnTilgangsstyringService.hentAltinnOrganisasjoner(fnr, () -> "");

        // Alt som finnes i tilganger-mappet skal også finnes i organisasjoner-settet
        assertThat(organisasjoner).extracting(org -> new BedriftNr(org.getOrganizationNumber())).containsAll(tilganger.keySet());

        // Parents skal ikke være i tilgang-map
        assertThat(tilganger).doesNotContainKey(new BedriftNr("910825555"));

        // Virksomheter skal være i tilgang-map
        assertThat(tilganger.get(new BedriftNr("999999999"))).containsOnly(Tiltakstype.ARBEIDSTRENING, Tiltakstype.MIDLERTIDIG_LONNSTILSKUDD, Tiltakstype.VARIG_LONNSTILSKUDD, Tiltakstype.SOMMERJOBB, Tiltakstype.MENTOR, Tiltakstype.INKLUDERINGSTILSKUDD); // TODO: Tilgangsstyring skal skille på midlertidig lønnstilskudd og sommerjobb

    }

    @Test
    public void hentOrganisasjoner__ingen_tilgang() {
        Fnr fnr = new Fnr("09000000000");
        Map<BedriftNr, Collection<Tiltakstype>> tilganger = altinnTilgangsstyringService.hentTilganger(fnr, () -> "");
        Set<AltinnReportee> organisasjoner = altinnTilgangsstyringService.hentAltinnOrganisasjoner(fnr, () -> "");

        assertThat(organisasjoner).isEmpty();
        assertThat(tilganger).isEmpty();
    }

    @Test
    public void hentTilganger__midlertidig_feil_gir_feilkode() {
        assertThatThrownBy(() -> altinnTilgangsstyringService.hentTilganger(new Fnr("31000000000"), () -> "")).isExactlyInstanceOf(AltinnFeilException.class);
    }

    @Test
    public void manglende_serviceCode_skal_kaste_feil() {
        AltinnTilgangsstyringProperties altinnTilgangsstyringProperties = new AltinnTilgangsstyringProperties();
        assertThatThrownBy(() -> new AltinnTilgangsstyringService(altinnTilgangsstyringProperties, tokenUtils, "tiltaksgjennomforing-api")).isExactlyInstanceOf(TiltaksgjennomforingException.class);
    }
}
