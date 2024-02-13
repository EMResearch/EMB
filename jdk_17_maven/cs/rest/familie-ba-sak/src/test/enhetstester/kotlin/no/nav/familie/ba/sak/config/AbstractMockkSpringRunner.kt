package no.nav.familie.ba.sak.config

import io.mockk.isMockKMock
import io.mockk.unmockkAll
import no.nav.familie.ba.sak.common.LocalDateService
import no.nav.familie.ba.sak.integrasjoner.ef.EfSakRestClient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.FamilieIntegrasjonerTilgangskontrollClient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdBarnetrygdClient
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdBarnetrygdClientMock
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlIdentRestClient
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiKlient
import no.nav.familie.ba.sak.kjerne.tilbakekreving.TilbakekrevingKlient
import no.nav.familie.ba.sak.task.OpprettTaskService
import no.nav.familie.ba.sak.task.TaskRepositoryTestConfig
import no.nav.familie.unleash.UnleashService
import org.junit.jupiter.api.BeforeEach
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.CacheManager
import org.springframework.context.ConfigurableApplicationContext

abstract class AbstractMockkSpringRunner {
    /**
     * Tjenester vi mocker ved bruk av every
     */
    @Autowired
    private lateinit var mockPersonopplysningerService: PersonopplysningerService

    @Autowired
    private lateinit var mockPdlIdentRestClient: PdlIdentRestClient

    @Autowired
    private lateinit var mockIntegrasjonClient: IntegrasjonClient

    @Autowired
    private lateinit var mockFamilieIntegrasjonerTilgangskontrollClient: FamilieIntegrasjonerTilgangskontrollClient

    @Autowired
    private lateinit var mockEfSakRestClient: EfSakRestClient

    @Autowired
    private lateinit var mockØkonomiKlient: ØkonomiKlient

    @Autowired
    private lateinit var mockFeatureToggleService: FeatureToggleService

    @Autowired
    private lateinit var mockUnleashService: UnleashService

    @Autowired
    private lateinit var mockTilbakekrevingKlient: TilbakekrevingKlient

    @Autowired
    private lateinit var mockLocalDateService: LocalDateService

    @Autowired
    private lateinit var mockInfotrygdBarnetrygdClient: InfotrygdBarnetrygdClient

    @Autowired
    private lateinit var mockTaskRepository: TaskRepositoryWrapper

    @Autowired
    private lateinit var mockOpprettTaskService: OpprettTaskService

    @Autowired
    private lateinit var applicationContext: ConfigurableApplicationContext

    /**
     * Cachemanagere
     */
    @Autowired
    private lateinit var defaultCacheManager: CacheManager

    @Autowired
    @Qualifier("dailyCache")
    private lateinit var dailyCacheManager: CacheManager

    @Autowired
    @Qualifier("shortCache")
    private lateinit var shortCacheManager: CacheManager

    @BeforeEach
    fun reset() {
        clearCaches()
        clearMocks()
    }

    private fun clearMocks() {
        unmockkAll()
        if (isMockKMock(mockPersonopplysningerService)) {
            ClientMocks.clearPdlMocks(mockPersonopplysningerService)
        }

        if (isMockKMock(mockPdlIdentRestClient)) {
            ClientMocks.clearPdlIdentRestClient(mockPdlIdentRestClient)
        }

        IntegrasjonClientMock.clearIntegrasjonMocks(mockIntegrasjonClient)
        IntegrasjonClientMock.clearMockFamilieIntegrasjonerTilgangskontrollClient(
            mockFamilieIntegrasjonerTilgangskontrollClient,
        )

        if (isMockKMock(mockFeatureToggleService)) {
            ClientMocks.clearFeatureToggleMocks(mockFeatureToggleService)
        }

        if (isMockKMock(mockUnleashService)) {
            ClientMocks.clearUnleashServiceMocks(mockUnleashService)
        }

        if (isMockKMock(mockEfSakRestClient)) {
            EfSakRestClientMock.clearEfSakRestMocks(mockEfSakRestClient)
        }

        if (isMockKMock(mockØkonomiKlient)) {
            ØkonomiTestConfig.clearØkonomiMocks(mockØkonomiKlient)
        }

        if (isMockKMock(mockTilbakekrevingKlient)) {
            TilbakekrevingKlientTestConfig.clearTilbakekrevingKlientMocks(mockTilbakekrevingKlient)
        }

        if (isMockKMock(mockLocalDateService)) {
            LocalDateServiceTestConfig.clearLocalDateServiceMocks(mockLocalDateService)
        }

        if (isMockKMock(mockInfotrygdBarnetrygdClient)) {
            InfotrygdBarnetrygdClientMock.clearInfotrygdBarnetrygdMocks(mockInfotrygdBarnetrygdClient)
        }

        if (isMockKMock(mockTaskRepository)) {
            TaskRepositoryTestConfig.clearMockTaskRepository(mockTaskRepository)
        }

        if (isMockKMock(mockOpprettTaskService)) {
            TaskRepositoryTestConfig.clearMockTaskService(mockOpprettTaskService)
        }

        MDC.put("callId", "callId")
    }

    private fun clearCaches() {
        listOf(defaultCacheManager, shortCacheManager, dailyCacheManager).forEach {
            it.cacheNames.mapNotNull { cacheName -> it.getCache(cacheName) }
                .forEach { cache -> cache.clear() }
        }
    }
}
