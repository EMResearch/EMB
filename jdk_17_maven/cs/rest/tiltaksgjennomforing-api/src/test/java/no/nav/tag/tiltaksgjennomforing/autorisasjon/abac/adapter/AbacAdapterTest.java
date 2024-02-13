package no.nav.tag.tiltaksgjennomforing.autorisasjon.abac.adapter;

import com.github.tomakehurst.wiremock.WireMockServer;
import no.nav.tag.tiltaksgjennomforing.IntegrasjonerMockServer;
import no.nav.tag.tiltaksgjennomforing.Miljø;
import no.nav.tag.tiltaksgjennomforing.avtale.Fnr;
import no.nav.tag.tiltaksgjennomforing.avtale.NavIdent;
import no.nav.tag.tiltaksgjennomforing.infrastruktur.cache.EhCacheConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles({ Miljø.LOCAL, "wiremock" })
@DirtiesContext
public class AbacAdapterTest {
    private final AbacAdapter abacAdapter;
    private final EhCacheCacheManager cacheManager;
    private final WireMockServer mockServer;

    public AbacAdapterTest(
            @Autowired AbacAdapter abacAdapter,
            @Autowired IntegrasjonerMockServer mockServerService,
            @Autowired EhCacheCacheManager ehCacheCacheManager) {
        this.abacAdapter = abacAdapter;
        this.cacheManager = ehCacheCacheManager;
        this.mockServer = mockServerService.getServer();
    }

    @BeforeEach
    public void setup() {
        cacheManager.getCache(EhCacheConfig.ABAC_CACHE).clear();
    }

    @Test
    public void skal_teste_at_Abac_ikke_gi_lese_tilgang_på_På_Gitt_Bruker_Og_Veileder() {
        NavIdent veilederIdent = new NavIdent("F142226");
        Fnr deltakerFnr = new Fnr("01118023456");

        boolean utfall = abacAdapter.harSkriveTilgang(veilederIdent.asString(), deltakerFnr.asString());

        assertFalse(utfall);
    }

    @Test
    public void skal_teste_abac_feiler_gir_false() {
        NavIdent veilederIdent = new NavIdent("F142226");
        Fnr deltakerFnr = new Fnr("11111111111");

        boolean utfall = abacAdapter.harSkriveTilgang(veilederIdent.asString(), deltakerFnr.asString());
        assertFalse(utfall);
    }

    @Test
    public void skal_teste_at_Abac_gi_lese_tilgang_på_Gitt_Bruker_Og_Veileder() {
        NavIdent veilederIdent = new NavIdent("F142226");
        Fnr deltakerFnr = new Fnr("07098142678");

        boolean utfall = abacAdapter.harSkriveTilgang(veilederIdent.asString(), deltakerFnr.asString());
        assertTrue(utfall);
    }

    @Test
    public void skal_teste_at_Abac_ikke_gir_tilgang_til_feil_person_fra_cache() {
        NavIdent veilederIdent = new NavIdent("F142226");
        Fnr deltakerFnr = new Fnr("01118023456");

        boolean harIkkeTilgang = abacAdapter.harSkriveTilgang(veilederIdent.asString(), deltakerFnr.asString());

        assertFalse(harIkkeTilgang);

        NavIdent veilederSkalHaTilgang = new NavIdent("X142226");
        boolean harTilgang = abacAdapter.harSkriveTilgang(veilederSkalHaTilgang.asString(), deltakerFnr.asString());

        assertTrue(harTilgang);

    }

    @Test
    public void bekreft_antall_ganger_endepunkter_blir_kalt_ved_abac() {
        NavIdent veilederIdent = new NavIdent("F142226");

        Fnr første_deltakerFnr = new Fnr("07098142678");
        Fnr andre_deltakerFnr = new Fnr("01118023456");
        mockServer.resetAll();

        boolean tilgang_navId_F142226_og_fnr_07098142678 = abacAdapter.harSkriveTilgang(veilederIdent.asString(), første_deltakerFnr.asString());
        boolean tilgang_navId_F142226_og_fnr_07098142678_response2 = abacAdapter.harSkriveTilgang(veilederIdent.asString(), første_deltakerFnr.asString());

        mockServer.verify(exactly(1), postRequestedFor(urlEqualTo("/abac")));
        mockServer.resetAll();

        boolean tilgang_navId_F142226_og_fnr_11111111111 = abacAdapter.harSkriveTilgang(veilederIdent.asString(), andre_deltakerFnr.asString());
        boolean tilgang_navId_F142226_og_fnr_11111111111_response2 = abacAdapter.harSkriveTilgang(veilederIdent.asString(), andre_deltakerFnr.asString());

        mockServer.verify(exactly(1), postRequestedFor(urlEqualTo("/abac")));

        assertTrue(tilgang_navId_F142226_og_fnr_07098142678);
        assertTrue(tilgang_navId_F142226_og_fnr_07098142678_response2);

        assertFalse(tilgang_navId_F142226_og_fnr_11111111111);
        assertFalse(tilgang_navId_F142226_og_fnr_11111111111_response2);
    }

    @Test
    public void ikke_cache_ved_feil_fra_abac() {
        NavIdent veilederIdent = new NavIdent("F142226");
        Fnr deltakerFnr = new Fnr("11111111111");
        mockServer.resetAll();

        boolean tilgang_navId_F142226_og_fnr_07098142678 = abacAdapter.harSkriveTilgang(veilederIdent.asString(), deltakerFnr.asString());
        boolean tilgang_navId_F142226_og_fnr_07098142678_response2 = abacAdapter.harSkriveTilgang(veilederIdent.asString(), deltakerFnr.asString());

        mockServer.verify(exactly(2), postRequestedFor(urlEqualTo("/abac")));

        assertFalse(tilgang_navId_F142226_og_fnr_07098142678);
        assertFalse(tilgang_navId_F142226_og_fnr_07098142678_response2);
    }
}