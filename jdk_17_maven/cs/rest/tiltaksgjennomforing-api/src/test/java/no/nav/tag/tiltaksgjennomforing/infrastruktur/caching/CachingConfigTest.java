package no.nav.tag.tiltaksgjennomforing.infrastruktur.caching;


import lombok.extern.slf4j.Slf4j;
import no.nav.tag.tiltaksgjennomforing.Miljø;
import no.nav.tag.tiltaksgjennomforing.autorisasjon.SlettemerkeProperties;
import no.nav.tag.tiltaksgjennomforing.autorisasjon.abac.TilgangskontrollService;
import no.nav.tag.tiltaksgjennomforing.avtale.*;
import no.nav.tag.tiltaksgjennomforing.enhet.*;
import no.nav.tag.tiltaksgjennomforing.featuretoggles.enhet.NavEnhet;
import no.nav.tag.tiltaksgjennomforing.persondata.PdlRespons;
import no.nav.tag.tiltaksgjennomforing.persondata.PersondataService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static no.nav.tag.tiltaksgjennomforing.infrastruktur.cache.EhCacheConfig.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;


@Slf4j
@SpringBootTest
@ActiveProfiles({ Miljø.LOCAL,  "wiremock" })
@ExtendWith(SpringExtension.class)
@DirtiesContext
public class CachingConfigTest {

    private final CacheManager cacheManager;
    private final VeilarbArenaClient veilarbArenaClient;
    private final Norg2Client norg2Client;
    private final PersondataService persondataService;

    public CachingConfigTest(
            @Autowired CacheManager cacheManager,
            @Autowired VeilarbArenaClient veilarbArenaClient,
            @Autowired Norg2Client norg2Client,
            @Autowired PersondataService persondataService
    ){
        this.cacheManager = cacheManager;
        this.veilarbArenaClient = veilarbArenaClient;
        this.norg2Client = norg2Client;
        this.persondataService = persondataService;
    }

    private  <T,K> T getCacheValue(String cacheName, K cacheKey, Class<T> clazz) {
        return (T) Objects.requireNonNull(Objects.requireNonNull(cacheManager.getCache(cacheName)).get(cacheKey)).get();
    }

    @Test
    public void sjekk_at_caching_fanger_opp_data_fra_arena_cache() {
        final NavEnhet oppfolgingNavEnhet = TestData.ENHET_OPPFØLGING;
        final String ETT_FNR_NR = "00000000000";
        final String ETT_FNR_NR2 = "11111111111";
        final String ETT_FNR_NR3 = "22127748067";

        Avtale avtale = TestData.enMidlertidigLonnstilskuddsjobbAvtale();
        avtale.setDeltakerFnr(new Fnr(ETT_FNR_NR));
        TestData.setGeoNavEnhet(avtale, oppfolgingNavEnhet);
        TestData.setOppfolgingNavEnhet(avtale, oppfolgingNavEnhet);

        veilarbArenaClient.HentOppfølgingsenhetFraCacheEllerArena(avtale.getDeltakerFnr().asString());
        veilarbArenaClient.HentOppfølgingsenhetFraCacheEllerArena(ETT_FNR_NR2);
        veilarbArenaClient.HentOppfølgingsenhetFraCacheEllerArena(ETT_FNR_NR2);
        veilarbArenaClient.HentOppfølgingsenhetFraCacheEllerArena(ETT_FNR_NR3);
        veilarbArenaClient.HentOppfølgingsenhetFraCacheEllerArena(ETT_FNR_NR2);

        Assertions.assertEquals("0906", getCacheValue(ARENA_CACHCE, ETT_FNR_NR, Oppfølgingsstatus.class).getOppfolgingsenhet());
        Assertions.assertEquals("0904", getCacheValue(ARENA_CACHCE, ETT_FNR_NR2, Oppfølgingsstatus.class).getOppfolgingsenhet());
        Assertions.assertEquals("0906", getCacheValue(ARENA_CACHCE, ETT_FNR_NR3, Oppfølgingsstatus.class).getOppfolgingsenhet());
    }

    @Test
    public void sjekk_at_caching_fanger_opp_data_fra_norgnavn_cache() {
        final NavEnhet oppfolgingNavEnhet = TestData.ENHET_OPPFØLGING;
        Avtale avtale = TestData.enMidlertidigLonnstilskuddsjobbAvtale();

        TestData.setOppfolgingNavEnhet(avtale, oppfolgingNavEnhet);

        Norg2OppfølgingResponse norg2OppfølgingResponse = norg2Client.hentOppfølgingsEnhetsnavnFraCacheNorg2(
                avtale.getEnhetOppfolging()
        );
        Norg2OppfølgingResponse norgnavnCacheForEnhet = getCacheValue(
                NORGNAVN_CACHE,
                avtale.getEnhetOppfolging(),
                Norg2OppfølgingResponse.class
        );

        Assertions.assertEquals("NAV Agder", norgnavnCacheForEnhet.getNavn());
        Assertions.assertEquals("1000", norgnavnCacheForEnhet.getEnhetNr());
        Assertions.assertEquals(norg2OppfølgingResponse.getNavn(), norgnavnCacheForEnhet.getNavn());
        Assertions.assertEquals(norg2OppfølgingResponse.getEnhetNr(), norgnavnCacheForEnhet.getEnhetNr());
    }

    @Test
    public void sjekk_at_caching_fanger_opp_data_fra_norggeoenhet_cache() {
        PdlRespons pdlRespons = TestData.enPdlrespons(false);
        Optional<String> optionalGeoEnhet = PersondataService.hentGeoLokasjonFraPdlRespons(pdlRespons);
        String geoEnhet = optionalGeoEnhet.get();

        Norg2GeoResponse norg2GeoResponse = norg2Client.hentGeoEnhetFraCacheEllerNorg2(geoEnhet);
        Norg2GeoResponse norggeoenhetCacheForGeoEnhet = getCacheValue(NORG_GEO_ENHET, geoEnhet, Norg2GeoResponse.class);

        Assertions.assertEquals("NAV St. Hanshaugen", norggeoenhetCacheForGeoEnhet.getNavn());
        Assertions.assertEquals("0313", norggeoenhetCacheForGeoEnhet.getEnhetNr());
        Assertions.assertEquals(norg2GeoResponse.getNavn(), norggeoenhetCacheForGeoEnhet.getNavn());
        Assertions.assertEquals(norg2GeoResponse.getEnhetNr(), norggeoenhetCacheForGeoEnhet.getEnhetNr());
    }

    @Test
    public void sjekk_at_caching_fanger_opp_data_fra_pdl_cache() {
        Fnr brukerFnr = new Fnr("00000000000");
        PdlRespons pdlRespons = persondataService.hentPersondataFraPdl(brukerFnr);

        PdlRespons pdlCache = getCacheValue(PDL_CACHE, brukerFnr, PdlRespons.class);

        Assertions.assertEquals("030104", persondataService.hentGeoLokasjonFraPdlRespons(pdlCache).get());
        Assertions.assertEquals("3", pdlCache.getData().getHentGeografiskTilknytning().getRegel());
        Assertions.assertEquals("Donald", persondataService.hentNavnFraPdlRespons(pdlCache).getFornavn());
        Assertions.assertEquals("Duck", persondataService.hentNavnFraPdlRespons(pdlCache).getEtternavn());
        Assertions.assertEquals(
                persondataService.hentGeoLokasjonFraPdlRespons(pdlRespons).get(),
                persondataService.hentGeoLokasjonFraPdlRespons(pdlCache).get()
        );
        Assertions.assertEquals(
                pdlRespons.getData().getHentGeografiskTilknytning().getRegel(),
                pdlCache.getData().getHentGeografiskTilknytning().getRegel()
        );
        Assertions.assertEquals(
                persondataService.hentNavnFraPdlRespons(pdlRespons).getFornavn(),
                persondataService.hentNavnFraPdlRespons(pdlCache).getFornavn()
        );
        Assertions.assertEquals(
                persondataService.hentNavnFraPdlRespons(pdlRespons).getEtternavn(),
                persondataService.hentNavnFraPdlRespons(pdlCache).getEtternavn()
        );
    }

    @Test
    public void vertifisere_at_caching_fungerer_for_endreAvtale_av_veileder() {
        final NavEnhet oppfolgingNavEnhet = TestData.ENHET_OPPFØLGING;
        final String GEO_LOKASJON_FRA_PDL_MAPPING = "030104";
        Avtale avtale = TestData.enMidlertidigLonnstilskuddsjobbAvtale();
        TestData.setGeoNavEnhet(avtale, oppfolgingNavEnhet);
        TestData.setOppfolgingNavEnhet(avtale, oppfolgingNavEnhet);

        final TilgangskontrollService mockTilgangskontrollService = mock(TilgangskontrollService.class);

        Veileder veileder = new Veileder(
                avtale.getVeilederNavIdent(),
                mockTilgangskontrollService,
                persondataService,
                norg2Client,
                Set.of(new NavEnhet(avtale.getEnhetOppfolging(), avtale.getEnhetsnavnOppfolging())),
                new SlettemerkeProperties(),
                false,
                veilarbArenaClient
        );

        lenient().when(mockTilgangskontrollService.harSkrivetilgangTilKandidat(
                eq(veileder),
                eq(avtale.getDeltakerFnr())
        )).thenReturn(true, true, true);

        veileder.endreAvtale(
                Instant.now(),
                TestData.endringPåAlleLønnstilskuddFelter(),
                avtale,
                TestData.avtalerMedTilskuddsperioder
        );

        veileder.endreAvtale(
                Instant.now(),
                TestData.endringPåAlleLønnstilskuddFelter(),
                avtale,
                TestData.avtalerMedTilskuddsperioder
        );

        Norg2OppfølgingResponse norgnavnCacheForEnhet = getCacheValue(
                NORGNAVN_CACHE,
                avtale.getEnhetOppfolging(),
                Norg2OppfølgingResponse.class
        );
        Norg2GeoResponse norggeoenhetCacheForGeoEnhet = getCacheValue(
                NORG_GEO_ENHET,
                GEO_LOKASJON_FRA_PDL_MAPPING,
                Norg2GeoResponse.class
        );
        PdlRespons pdlCache = getCacheValue(PDL_CACHE, avtale.getDeltakerFnr(), PdlRespons.class);
        Oppfølgingsstatus arenaCache = getCacheValue(ARENA_CACHCE, avtale.getDeltakerFnr().asString(), Oppfølgingsstatus.class);

        Assertions.assertEquals("NAV St. Hanshaugen", norggeoenhetCacheForGeoEnhet.getNavn());
        Assertions.assertEquals("0313", norggeoenhetCacheForGeoEnhet.getEnhetNr());

        Assertions.assertEquals("NAV Agder", norgnavnCacheForEnhet.getNavn());
        Assertions.assertEquals("1000", norgnavnCacheForEnhet.getEnhetNr());

        Assertions.assertEquals("030104", persondataService.hentGeoLokasjonFraPdlRespons(pdlCache).get());
        Assertions.assertEquals("3", pdlCache.getData().getHentGeografiskTilknytning().getRegel());
        Assertions.assertEquals("Donald", persondataService.hentNavnFraPdlRespons(pdlCache).getFornavn());
        Assertions.assertEquals("Duck", persondataService.hentNavnFraPdlRespons(pdlCache).getEtternavn());

        Assertions.assertEquals("0906", arenaCache.getOppfolgingsenhet());
    }
}
