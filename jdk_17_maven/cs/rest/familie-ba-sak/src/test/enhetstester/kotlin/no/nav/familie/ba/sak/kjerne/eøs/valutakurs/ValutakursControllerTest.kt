package no.nav.familie.ba.sak.kjerne.eøs.valutakurs

import io.mockk.MockKException
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import io.mockk.verify
import no.nav.familie.ba.sak.config.tilAktør
import no.nav.familie.ba.sak.ekstern.restDomene.RestValutakurs
import no.nav.familie.ba.sak.ekstern.restDomene.UtfyltStatus
import no.nav.familie.ba.sak.ekstern.restDomene.tilValutakurs
import no.nav.familie.ba.sak.integrasjoner.ecb.ECBService
import no.nav.familie.ba.sak.kjerne.behandling.UtvidetBehandlingService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

@ExtendWith(MockKExtension::class)
class ValutakursControllerTest {

    @MockK
    private lateinit var valutakursService: ValutakursService

    @MockK
    private lateinit var personidentService: PersonidentService

    @MockK
    private lateinit var utvidetBehandlingService: UtvidetBehandlingService

    @MockK
    private lateinit var ecbService: ECBService

    @MockK
    private lateinit var tilgangService: TilgangService

    @InjectMockKs
    private lateinit var valutakursController: ValutakursController

    private val barnId = "12345678910"

    private val restValutakurs: RestValutakurs =
        RestValutakurs(1, YearMonth.of(2020, 1), null, listOf(barnId), null, null, null, UtfyltStatus.OK)

    @BeforeEach
    fun setup() {
        every { personidentService.hentAktør(any()) } returns tilAktør(barnId)
        every { valutakursService.hentValutakurs(any()) } returns restValutakurs.tilValutakurs(listOf(tilAktør(barnId)))
        every { ecbService.hentValutakurs(any(), any()) } returns BigDecimal.valueOf(0.95)
        justRun { tilgangService.validerTilgangTilBehandling(any(), any()) }
        justRun { tilgangService.verifiserHarTilgangTilHandling(any(), any()) }
        justRun { tilgangService.validerKanRedigereBehandling(any()) }
    }

    @Test
    fun `Test at valutakurs hentes fra ECB dersom dato og valuta er satt`() {
        val valutakursDato = LocalDate.of(2022, 1, 1)
        val valuta = "SEK"
        assertThrows<MockKException> {
            valutakursController.oppdaterValutakurs(
                1,
                restValutakurs.copy(valutakursdato = valutakursDato, valutakode = valuta),
            )
        }
        verify(exactly = 1) { ecbService.hentValutakurs("SEK", valutakursDato) }
        verify(exactly = 1) { valutakursService.oppdaterValutakurs(any(), any()) }
    }

    @Test
    fun `Test at valutakurs ikke hentes fra ECB dersom dato ikke er satt`() {
        val valutakursDato = LocalDate.of(2022, 1, 1)
        assertThrows<MockKException> {
            valutakursController.oppdaterValutakurs(
                1,
                restValutakurs.copy(valutakode = "SEK"),
            )
        }
        verify(exactly = 0) { ecbService.hentValutakurs("SEK", valutakursDato) }
        verify(exactly = 1) { valutakursService.oppdaterValutakurs(any(), any()) }
    }

    @Test
    fun `Test at valutakurs ikke hentes fra ECB dersom valuta ikke er satt`() {
        val valutakursDato = LocalDate.of(2022, 1, 1)
        assertThrows<MockKException> {
            valutakursController.oppdaterValutakurs(
                1,
                restValutakurs.copy(valutakursdato = valutakursDato),
            )
        }
        verify(exactly = 0) { ecbService.hentValutakurs("SEK", valutakursDato) }
        verify(exactly = 1) { valutakursService.oppdaterValutakurs(any(), any()) }
    }

    @Test
    fun `Test at valutakurs ikke hentes fra ECB dersom ISK og før 1 feb 2018`() {
        val valutakursDato = LocalDate.of(2018, 1, 31)
        assertThrows<MockKException> {
            valutakursController.oppdaterValutakurs(
                1,
                restValutakurs.copy(valutakursdato = valutakursDato, valutakode = "ISK"),
            )
        }
        verify(exactly = 0) { ecbService.hentValutakurs("ISK", valutakursDato) }
        verify(exactly = 1) { valutakursService.oppdaterValutakurs(any(), any()) }
    }

    @Test
    fun `Test at valutakurs hentes fra ECB dersom ISK og etter 1 feb 2018`() {
        val valutakursDato = LocalDate.of(2018, 2, 1)
        assertThrows<MockKException> {
            valutakursController.oppdaterValutakurs(
                1,
                restValutakurs.copy(valutakursdato = valutakursDato, valutakode = "ISK"),
            )
        }
        verify(exactly = 1) { ecbService.hentValutakurs("ISK", valutakursDato) }
        verify(exactly = 1) { valutakursService.oppdaterValutakurs(any(), any()) }
    }
}
