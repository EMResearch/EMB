package no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner

import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.clearAllCaches
import no.nav.familie.ba.sak.config.IntegrasjonClientMock.Companion.mockSjekkTilgang
import no.nav.familie.ba.sak.util.BrukerContextUtil.testWithBrukerContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.cache.concurrent.ConcurrentMapCacheManager

class FamilieIntegrasjonerTilgangskontrollServiceTest {

    private val client = mockk<FamilieIntegrasjonerTilgangskontrollClient>()

    private val cacheManager = ConcurrentMapCacheManager()

    private val service = FamilieIntegrasjonerTilgangskontrollService(client, cacheManager, mockk())

    private val slot = mutableListOf<List<String>>()

    @BeforeEach
    fun setUp() {
        slot.clear()
        cacheManager.clearAllCaches()
    }

    @Test
    fun `har tilgang skal cacheas`() {
        client.mockSjekkTilgang(true, slot)

        assertThat(testWithBrukerContext { service.sjekkTilgangTilPerson("1") }.harTilgang).isTrue
        assertThat(testWithBrukerContext { service.sjekkTilgangTilPerson("1") }.harTilgang).isTrue
        verify(exactly = 1) { client.sjekkTilgangTilPersoner(any()) }
    }

    @Test
    fun `har ikke tilgang skal cacheas`() {
        client.mockSjekkTilgang(false, slot)

        assertThat(testWithBrukerContext { service.sjekkTilgangTilPerson("1") }.harTilgang).isFalse
        assertThat(testWithBrukerContext { service.sjekkTilgangTilPerson("1") }.harTilgang).isFalse
        verify(exactly = 1) { client.sjekkTilgangTilPersoner(any()) }
    }

    @Test
    fun `cachear per saksbehandlere`() {
        client.mockSjekkTilgang(false, slot)

        // Systemcontext
        service.sjekkTilgangTilPerson("1")
        val kall1 = testWithBrukerContext("saksbehandler1") { service.sjekkTilgangTilPerson("1") }
        val kall2 = testWithBrukerContext("saksbehandler2") { service.sjekkTilgangTilPerson("1") }
        assertThat(kall1.harTilgang).isFalse
        assertThat(kall2.harTilgang).isFalse
        verify(exactly = 3) { client.sjekkTilgangTilPersoner(any()) }
    }

    @Test
    fun `tilgangskontrollerer unike identer`() {
        client.mockSjekkTilgang(false, slot)

        testWithBrukerContext("saksbehandler1") { service.sjekkTilgangTilPersoner(listOf("1", "1")) }

        verify(exactly = 1) { client.sjekkTilgangTilPersoner(listOf("1")) }
    }

    @Test
    fun `skal ikke hente identer som allerede finnes i cachen`() {
        val tilgang = mapOf("1" to false, "2" to true, "3" to false)
        client.mockSjekkTilgang(tilgang, slot)

        testWithBrukerContext { service.sjekkTilgangTilPerson("1") }
        val sjekkTilgangTilPersoner = testWithBrukerContext { service.sjekkTilgangTilPersoner(listOf("2", "1", "3")) }
        testWithBrukerContext { service.sjekkTilgangTilPersoner(listOf("2", "1", "3")) }
        testWithBrukerContext { service.sjekkTilgangTilPersoner(listOf("3", "3", "3")) }

        assertThat(sjekkTilgangTilPersoner.all { it.key == it.value.personIdent })
        assertThat(sjekkTilgangTilPersoner.map { it.key to it.value.harTilgang }).containsExactlyInAnyOrderElementsOf(
            tilgang.entries.map { Pair(it.key, it.value) }.toList(),
        )

        verify(exactly = 2) { client.sjekkTilgangTilPersoner(any()) }

        val forventetFørsteKall = listOf("1")
        val forventetAndreKall = listOf("2", "3")
        assertThat(slot).containsExactlyElementsOf(listOf(forventetFørsteKall, forventetAndreKall))
    }
}
