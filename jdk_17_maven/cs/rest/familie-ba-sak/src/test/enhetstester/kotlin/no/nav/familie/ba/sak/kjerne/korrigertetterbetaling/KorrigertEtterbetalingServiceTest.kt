package no.nav.familie.ba.sak.kjerne.korrigertetterbetaling

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.fail
import org.hamcrest.CoreMatchers.`is` as Is

@ExtendWith(MockKExtension::class)
internal class KorrigertEtterbetalingServiceTest {

    @MockK
    private lateinit var korrigertEtterbetalingRepository: KorrigertEtterbetalingRepository

    @MockK
    private lateinit var loggService: LoggService

    @InjectMockKs
    private lateinit var korrigertEtterbetalingService: KorrigertEtterbetalingService

    @Test
    fun `finnAktivtKorrigeringPåBehandling skal hente aktivt korrigering fra repository hvis det finnes`() {
        val behandling = lagBehandling()
        val korrigertEtterbetaling = lagKorrigertEtterbetaling(behandling)

        every { korrigertEtterbetalingRepository.finnAktivtKorrigeringPåBehandling(behandling.id) } returns korrigertEtterbetaling

        val hentetKorrigertEtterbetaling =
            korrigertEtterbetalingService.finnAktivtKorrigeringPåBehandling(behandling.id)
                ?: fail("etterbetaling korrigering ikke hentet riktig")

        assertThat(hentetKorrigertEtterbetaling.behandling.id, Is(behandling.id))
        assertThat(hentetKorrigertEtterbetaling.aktiv, Is(true))

        verify(exactly = 1) { korrigertEtterbetalingRepository.finnAktivtKorrigeringPåBehandling(behandling.id) }
    }

    @Test
    fun `finnAlleKorrigeringerPåBehandling skal hente alle korrigering fra repository hvis de finnes`() {
        val behandling = lagBehandling()
        val korrigertEtterbetaling = lagKorrigertEtterbetaling(behandling)

        every { korrigertEtterbetalingRepository.finnAlleKorrigeringerPåBehandling(behandling.id) } returns listOf(
            korrigertEtterbetaling,
            korrigertEtterbetaling,
        )

        val hentetKorrigertEtterbetaling =
            korrigertEtterbetalingService.finnAlleKorrigeringerPåBehandling(behandling.id)

        assertThat(hentetKorrigertEtterbetaling.size, Is(2))

        verify(exactly = 1) { korrigertEtterbetalingRepository.finnAlleKorrigeringerPåBehandling(behandling.id) }
    }

    @Test
    fun `lagreKorrigertEtterbetaling skal lagre korrigering på behandling og logg på dette`() {
        val behandling = lagBehandling()
        val korrigertEtterbetaling = lagKorrigertEtterbetaling(behandling)

        every { korrigertEtterbetalingRepository.finnAktivtKorrigeringPåBehandling(behandling.id) } returns null
        every { korrigertEtterbetalingRepository.save(korrigertEtterbetaling) } returns korrigertEtterbetaling
        every { loggService.opprettKorrigertEtterbetalingLogg(behandling, any()) } returns Unit

        val lagretKorrigertEtterbetaling =
            korrigertEtterbetalingService.lagreKorrigertEtterbetaling(korrigertEtterbetaling)

        assertThat(lagretKorrigertEtterbetaling.behandling.id, Is(behandling.id))

        verify(exactly = 1) { korrigertEtterbetalingRepository.finnAktivtKorrigeringPåBehandling(behandling.id) }
        verify(exactly = 1) { korrigertEtterbetalingRepository.save(korrigertEtterbetaling) }
        verify(exactly = 1) {
            loggService.opprettKorrigertEtterbetalingLogg(
                behandling,
                korrigertEtterbetaling,
            )
        }
    }

    @Test
    fun `lagreKorrigertEtterbetaling skal sette og lagre forrige korrigering til inaktivt hvis det finnes tidligere korrigering`() {
        val behandling = lagBehandling()
        val forrigeKorrigering = mockk<KorrigertEtterbetaling>(relaxed = true)
        val korrigertEtterbetaling = lagKorrigertEtterbetaling(behandling)

        every { korrigertEtterbetalingRepository.finnAktivtKorrigeringPåBehandling(any()) } returns forrigeKorrigering
        every { korrigertEtterbetalingRepository.saveAndFlush(forrigeKorrigering) } returns korrigertEtterbetaling
        every { korrigertEtterbetalingRepository.save(korrigertEtterbetaling) } returns korrigertEtterbetaling
        every { loggService.opprettKorrigertEtterbetalingLogg(any(), any()) } returns Unit

        korrigertEtterbetalingService.lagreKorrigertEtterbetaling(korrigertEtterbetaling)

        verify(exactly = 1) { korrigertEtterbetalingRepository.finnAktivtKorrigeringPåBehandling(any()) }
        verify(exactly = 1) { forrigeKorrigering setProperty "aktiv" value false }
        verify(exactly = 1) { korrigertEtterbetalingRepository.saveAndFlush(forrigeKorrigering) }
        verify(exactly = 1) { korrigertEtterbetalingRepository.save(korrigertEtterbetaling) }
    }

    @Test
    fun `settKorrigeringPåBehandlingTilInaktiv skal sette korrigering til inaktivt hvis det finnes`() {
        val behandling = lagBehandling()
        val korrigertEtterbetaling = mockk<KorrigertEtterbetaling>(relaxed = true)

        every { korrigertEtterbetalingRepository.finnAktivtKorrigeringPåBehandling(any()) } returns korrigertEtterbetaling
        every { loggService.opprettKorrigertEtterbetalingLogg(any(), any()) } returns Unit

        korrigertEtterbetalingService.settKorrigeringPåBehandlingTilInaktiv(behandling)

        verify(exactly = 1) { korrigertEtterbetaling setProperty "aktiv" value false }
        verify(exactly = 1) {
            loggService.opprettKorrigertEtterbetalingLogg(
                any(),
                korrigertEtterbetaling,
            )
        }
    }
}

fun lagKorrigertEtterbetaling(
    behandling: Behandling,
    årsak: KorrigertEtterbetalingÅrsak = KorrigertEtterbetalingÅrsak.FEIL_TIDLIGERE_UTBETALT_BELØP,
    begrunnelse: String? = null,
    beløp: Int = 2000,
    aktiv: Boolean = true,
) =
    KorrigertEtterbetaling(
        behandling = behandling,
        årsak = årsak,
        begrunnelse = begrunnelse,
        aktiv = aktiv,
        beløp = beløp,
    )
