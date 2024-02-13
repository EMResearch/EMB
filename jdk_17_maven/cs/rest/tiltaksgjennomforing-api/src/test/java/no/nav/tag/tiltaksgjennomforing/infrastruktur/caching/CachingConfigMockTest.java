package no.nav.tag.tiltaksgjennomforing.infrastruktur.caching;


import no.nav.tag.tiltaksgjennomforing.autorisasjon.SlettemerkeProperties;
import no.nav.tag.tiltaksgjennomforing.autorisasjon.abac.TilgangskontrollService;
import no.nav.tag.tiltaksgjennomforing.autorisasjon.abac.adapter.AbacAdapter;
import no.nav.tag.tiltaksgjennomforing.avtale.*;
import no.nav.tag.tiltaksgjennomforing.enhet.*;
import no.nav.tag.tiltaksgjennomforing.featuretoggles.enhet.NavEnhet;
import no.nav.tag.tiltaksgjennomforing.infrastruktur.cache.EhCacheConfig;
import no.nav.tag.tiltaksgjennomforing.persondata.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.AopTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.mockito.Mockito.*;

@ActiveProfiles("cache-test")
@ContextConfiguration
@ExtendWith(SpringExtension.class)
public class CachingConfigMockTest {

    private TilgangskontrollService mockTilgangskontrollService;
    private PersondataService mockPersondataService;
    private Norg2Client mockNorg2Client;
    private VeilarbArenaClient mockVeilarbArenaClient;

    @Autowired
    private TilgangskontrollService tilgangskontrollService;
    @Autowired
    private PersondataService persondataService;
    @Autowired
    private Norg2Client norg2Client;
    @Autowired
    private VeilarbArenaClient veilarbArenaClient;

    private Avtale avtale = TestData.enMidlertidigLonnstilskuddsjobbAvtale();
    private final PdlRespons FØRSTE_PDL_RESPONSE = TestData.enPdlrespons(false);
    private PdlRespons ANDRE_PDL_RESPONSE = new PdlRespons(
            new Data(
                    new HentPerson(null, new Navn[]{new Navn("ola", "", "Norman")}),
                    null,
                    new HentGeografiskTilknytning("", "030202", "", "3")
            )
    );
    private final Norg2OppfølgingResponse FØRSTE_NORG2_OPPFØLGNING_RESPONSE = new Norg2OppfølgingResponse(
            1,
            "1000",
            "NAV Agder"
    );
    private final Norg2OppfølgingResponse ANDRE_NORG2_OPPFØLGNING_RESPONSE = new Norg2OppfølgingResponse(
            2,
            "1001",
            "NAV Agder2"
    );
    private final Norg2GeoResponse FØRSTE_NORG2_GEO_RESPONSE = new Norg2GeoResponse(
            "NAV St. Hanshaugen",
            "0313"
    );
    private final Norg2GeoResponse ANDRE_NORG2_GEO_RESPONSE = new Norg2GeoResponse(
            "NAV St. Hanshaugen2",
            "0314"
    );
    private final Oppfølgingsstatus FØRSTE_OPPFØLGNING_ENHET_ARENA = new Oppfølgingsstatus(
            Formidlingsgruppe.ARBEIDSSOKER,
            Kvalifiseringsgruppe.SITUASJONSBESTEMT_INNSATS,
            "0906"
    );
    private final Oppfølgingsstatus ANDRE_OPPFØLGNING_ENHET_ARENA = new Oppfølgingsstatus(
            Formidlingsgruppe.ARBEIDSSOKER,
            Kvalifiseringsgruppe.VARIG_TILPASSET_INNSATS,
            "0904"
    );


    @EnableCaching
    @Configuration
    @Profile("cache-test")
    public static class CachingTestConfig {

        @Bean
        public TilgangskontrollService tilgangskontrollServiceMockImplementation() {
            return mock(TilgangskontrollService.class);
        }

        @Bean
        public PersondataService persondataServiceMockImplementation() {
            return mock(PersondataService.class);
        }

        @Bean
        public Norg2Client norg2ClientMockImplementation() {
            return mock(Norg2Client.class);
        }

        @Bean
        public VeilarbArenaClient veilarbArenaClientMockImplementation() {
            return mock(VeilarbArenaClient.class);
        }

        @Bean
        public AbacAdapter abacAdapterMockImplementation() { return  mock(AbacAdapter.class); }

        @Bean
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(
                    EhCacheConfig.ARENA_CACHCE,
                    EhCacheConfig.PDL_CACHE,
                    EhCacheConfig.NORGNAVN_CACHE,
                    EhCacheConfig.NORG_GEO_ENHET,
                    EhCacheConfig.AXSYS_CACHE,
                    EhCacheConfig.ABAC_CACHE
            );
        }
    }

    @BeforeEach
    public void setUp() {
        final NavEnhet oppfolgingNavEnhet = TestData.ENHET_OPPFØLGING;

        TestData.setGeoNavEnhet(avtale, oppfolgingNavEnhet);
        TestData.setOppfolgingNavEnhet(avtale, oppfolgingNavEnhet);

        mockTilgangskontrollService = AopTestUtils.getTargetObject(tilgangskontrollService);
        mockPersondataService = AopTestUtils.getTargetObject(persondataService);
        mockNorg2Client = AopTestUtils.getTargetObject(norg2Client);
        mockVeilarbArenaClient = AopTestUtils.getTargetObject(veilarbArenaClient);

        lenient().when(mockTilgangskontrollService.harSkrivetilgangTilKandidat(
                any(),
                eq(avtale.getDeltakerFnr())
        )).thenReturn(true, true, true);

        when(mockPersondataService.hentPersondataFraPdl(avtale.getDeltakerFnr())).thenReturn(FØRSTE_PDL_RESPONSE, ANDRE_PDL_RESPONSE);
        when(mockNorg2Client.hentOppfølgingsEnhetsnavnFraCacheNorg2(any())).thenReturn(
                FØRSTE_NORG2_OPPFØLGNING_RESPONSE,
                ANDRE_NORG2_OPPFØLGNING_RESPONSE
        );
        when(mockNorg2Client.hentGeoEnhetFraCacheEllerNorg2(any())).thenReturn(FØRSTE_NORG2_GEO_RESPONSE, ANDRE_NORG2_GEO_RESPONSE);
        when(mockVeilarbArenaClient.HentOppfølgingsenhetFraCacheEllerArena(avtale.getDeltakerFnr().asString())).thenReturn(
                FØRSTE_OPPFØLGNING_ENHET_ARENA,
                ANDRE_OPPFØLGNING_ENHET_ARENA
        );
    }

    @Test
    public void bekreft_antall_ganger_Cacheable_endepunkter_blir_kalt_ved_norg2Client_oppfølgingsEnhetsnavn() {
        Norg2OppfølgingResponse norg2OppfølgingResponse = norg2Client.hentOppfølgingsEnhetsnavnFraCacheNorg2(
                avtale.getEnhetOppfolging()
        );
        Norg2OppfølgingResponse norg2OppfølgingResponse2 = norg2Client.hentOppfølgingsEnhetsnavnFraCacheNorg2(
                avtale.getEnhetOppfolging()
        );

        Assertions.assertEquals(FØRSTE_NORG2_OPPFØLGNING_RESPONSE.getEnhetId(), norg2OppfølgingResponse.getEnhetId());
        Assertions.assertEquals(FØRSTE_NORG2_OPPFØLGNING_RESPONSE.getEnhetNr(), norg2OppfølgingResponse.getEnhetNr());
        Assertions.assertEquals(FØRSTE_NORG2_OPPFØLGNING_RESPONSE.getNavn(), norg2OppfølgingResponse.getNavn());

        Assertions.assertEquals(FØRSTE_NORG2_OPPFØLGNING_RESPONSE.getEnhetId(), norg2OppfølgingResponse2.getEnhetId());
        Assertions.assertEquals(FØRSTE_NORG2_OPPFØLGNING_RESPONSE.getEnhetNr(), norg2OppfølgingResponse2.getEnhetNr());
        Assertions.assertEquals(FØRSTE_NORG2_OPPFØLGNING_RESPONSE.getNavn(), norg2OppfølgingResponse2.getNavn());

        /** Blir kalt 2 ganger. Andre iterasjon så treffer vi cache response istedenfor endepunkt */
        verify(mockNorg2Client, times(1)).hentOppfølgingsEnhetsnavnFraCacheNorg2(avtale.getEnhetOppfolging());
    }

    @Test
    public void bekreft_antall_ganger_Cacheable_endepunkter_blir_kalt_ved_norg2Client_geoEnhet() {
        Optional<String> optionalGeoEnhet = PersondataService.hentGeoLokasjonFraPdlRespons(FØRSTE_PDL_RESPONSE);
        String geoEnhet = optionalGeoEnhet.get();

        Norg2GeoResponse norg2GeoResponse = norg2Client.hentGeoEnhetFraCacheEllerNorg2(geoEnhet);
        Norg2GeoResponse norg2GeoResponse2 = norg2Client.hentGeoEnhetFraCacheEllerNorg2(geoEnhet);

        Assertions.assertEquals(FØRSTE_NORG2_GEO_RESPONSE.getNavn(), norg2GeoResponse.getNavn());
        Assertions.assertEquals(FØRSTE_NORG2_GEO_RESPONSE.getEnhetNr(), norg2GeoResponse.getEnhetNr());

        Assertions.assertEquals(FØRSTE_NORG2_GEO_RESPONSE.getNavn(), norg2GeoResponse2.getNavn());
        Assertions.assertEquals(FØRSTE_NORG2_GEO_RESPONSE.getEnhetNr(), norg2GeoResponse2.getEnhetNr());

        /** Blir kalt 2 ganger. Andre iterasjon så treffer vi cache response istedenfor endepunkt */
        verify(mockNorg2Client, times(1)).hentGeoEnhetFraCacheEllerNorg2(geoEnhet);
    }

    @Test
    public void bekreft_antall_ganger_Cacheable_endepunkter_blir_kalt_ved_arena() {
        Oppfølgingsstatus arenaResponse = veilarbArenaClient.HentOppfølgingsenhetFraCacheEllerArena(avtale.getDeltakerFnr().asString());
        Oppfølgingsstatus arenaResponse2 = veilarbArenaClient.HentOppfølgingsenhetFraCacheEllerArena(avtale.getDeltakerFnr().asString());

        Assertions.assertEquals(FØRSTE_OPPFØLGNING_ENHET_ARENA, arenaResponse);
        Assertions.assertEquals(FØRSTE_OPPFØLGNING_ENHET_ARENA, arenaResponse2);

        /** Blir kalt 2 ganger. Andre iterasjon så treffer vi cache response istedenfor endepunkt */
        verify(mockVeilarbArenaClient, times(1)).HentOppfølgingsenhetFraCacheEllerArena(avtale.getDeltakerFnr().asString());
    }

    @Test
    public void bekreft_antall_ganger_Cacheable_endepunkter_blir_kalt_ved_pdl() {
        PdlRespons pdlRespons = persondataService.hentPersondataFraPdl(avtale.getDeltakerFnr());
        PdlRespons pdlRespons2 = persondataService.hentPersondataFraPdl(avtale.getDeltakerFnr());

        Assertions.assertEquals(
                persondataService.hentGeoLokasjonFraPdlRespons(FØRSTE_PDL_RESPONSE).get(),
                persondataService.hentGeoLokasjonFraPdlRespons(pdlRespons).get()
        );
        Assertions.assertEquals(
                persondataService.hentGeoLokasjonFraPdlRespons(FØRSTE_PDL_RESPONSE).get(),
                persondataService.hentGeoLokasjonFraPdlRespons(pdlRespons2).get()
        );


        Assertions.assertEquals(
                FØRSTE_PDL_RESPONSE.getData().getHentGeografiskTilknytning().getRegel(),
                pdlRespons.getData().getHentGeografiskTilknytning().getRegel()
        );
        Assertions.assertEquals(
                FØRSTE_PDL_RESPONSE.getData().getHentGeografiskTilknytning().getRegel(),
                pdlRespons2.getData().getHentGeografiskTilknytning().getRegel()
        );

        Assertions.assertEquals(
                persondataService.hentNavnFraPdlRespons(FØRSTE_PDL_RESPONSE).getFornavn(),
                persondataService.hentNavnFraPdlRespons(pdlRespons).getFornavn()
        );
        Assertions.assertEquals(
                persondataService.hentNavnFraPdlRespons(FØRSTE_PDL_RESPONSE).getFornavn(),
                persondataService.hentNavnFraPdlRespons(pdlRespons2).getFornavn()
        );

        Assertions.assertEquals(
                persondataService.hentNavnFraPdlRespons(FØRSTE_PDL_RESPONSE).getEtternavn(),
                persondataService.hentNavnFraPdlRespons(pdlRespons).getEtternavn()
        );
        Assertions.assertEquals(
                persondataService.hentNavnFraPdlRespons(FØRSTE_PDL_RESPONSE).getEtternavn(),
                persondataService.hentNavnFraPdlRespons(pdlRespons2).getEtternavn()
        );

        /** Blir kalt 2 ganger. Andre iterasjon så treffer vi cache response istedenfor endepunkt */
        verify(mockPersondataService, times(1)).hentPersondataFraPdl(avtale.getDeltakerFnr());
    }

    @Test
    public void bekreft_antall_ganger_Cacheable_endepunkter_blir_kalt_ved_endreAvtale() {
        Optional<String> optionalGeoEnhet = PersondataService.hentGeoLokasjonFraPdlRespons(FØRSTE_PDL_RESPONSE);
        String geoEnhet = optionalGeoEnhet.get();

        Veileder veileder = new Veileder(
                avtale.getVeilederNavIdent(),
                tilgangskontrollService,
                persondataService,
                norg2Client,
                Set.of(new NavEnhet(avtale.getEnhetOppfolging(), avtale.getEnhetsnavnOppfolging())),
                new SlettemerkeProperties(),
                false,
                veilarbArenaClient
        );

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

        /** Blir kalt 2 ganger. Andre iterasjon så treffer vi cache response istedenfor endepunkt */
        verify(mockNorg2Client, times(1)).hentOppfølgingsEnhetsnavnFraCacheNorg2(avtale.getEnhetOppfolging());
        verify(mockNorg2Client, times(1)).hentGeoEnhetFraCacheEllerNorg2(geoEnhet);
        verify(mockPersondataService, times(1)).hentPersondataFraPdl(avtale.getDeltakerFnr());
        verify(mockVeilarbArenaClient, times(1)).HentOppfølgingsenhetFraCacheEllerArena(avtale.getDeltakerFnr().asString());
    }
}
