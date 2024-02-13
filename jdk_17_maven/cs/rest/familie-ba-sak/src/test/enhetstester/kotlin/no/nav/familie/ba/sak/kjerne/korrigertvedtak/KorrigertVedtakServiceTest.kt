package no.nav.familie.ba.sak.kjerne.korrigertvedtak

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.fail
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
internal class KorrigertVedtakServiceTest {

    @MockK
    private lateinit var korrigertVedtakRepository: KorrigertVedtakRepository

    @MockK
    private lateinit var loggService: LoggService

    @InjectMockKs
    private lateinit var korrigertVedtakService: KorrigertVedtakService

    @Test
    fun `finnAktivtKorrigertVedtakPåBehandling skal hente aktivt korrigert vedtak fra repository hvis det finnes`() {
        val behandling = lagBehandling()
        val korrigertVedtak = lagKorrigertVedtak(behandling)

        every { korrigertVedtakRepository.finnAktivtKorrigertVedtakPåBehandling(behandling.id) } returns korrigertVedtak

        val hentetKorrigertVedtak =
            korrigertVedtakService.finnAktivtKorrigertVedtakPåBehandling(behandling.id)
                ?: fail("korrigert vedtak ikke hentet riktig")

        MatcherAssert.assertThat(hentetKorrigertVedtak.behandling.id, CoreMatchers.`is`(behandling.id))
        MatcherAssert.assertThat(hentetKorrigertVedtak.aktiv, CoreMatchers.`is`(true))

        verify(exactly = 1) { korrigertVedtakRepository.finnAktivtKorrigertVedtakPåBehandling(behandling.id) }
    }

    @Test
    fun `lagreKorrigertVedtak skal lagre korrigert vedtak på behandling og logg på dette`() {
        val behandling = lagBehandling()
        val korrigertVedtak = lagKorrigertVedtak(behandling)

        every { korrigertVedtakRepository.finnAktivtKorrigertVedtakPåBehandling(behandling.id) } returns null
        every { korrigertVedtakRepository.save(korrigertVedtak) } returns korrigertVedtak
        every { loggService.opprettKorrigertVedtakLogg(behandling, any()) } returns Unit

        val lagretKorrigertVedtak =
            korrigertVedtakService.lagreKorrigertVedtak(korrigertVedtak)

        MatcherAssert.assertThat(lagretKorrigertVedtak.behandling.id, CoreMatchers.`is`(behandling.id))

        verify(exactly = 1) { korrigertVedtakRepository.finnAktivtKorrigertVedtakPåBehandling(behandling.id) }
        verify(exactly = 1) { korrigertVedtakRepository.save(korrigertVedtak) }
        verify(exactly = 1) {
            loggService.opprettKorrigertVedtakLogg(
                behandling,
                korrigertVedtak,
            )
        }
    }

    @Test
    fun `lagreKorrigertVedtak skal sette og lagre forrige korrigert vedtak til inaktivt hvis det finnes tidligere korrigering`() {
        val behandling = lagBehandling()
        val forrigeKorrigering = mockk<KorrigertVedtak>(relaxed = true)
        val korrigertVedtak = lagKorrigertVedtak(behandling, vedtaksdato = LocalDate.now().minusDays(3))

        every { korrigertVedtakRepository.finnAktivtKorrigertVedtakPåBehandling(any()) } returns forrigeKorrigering
        every { korrigertVedtakRepository.saveAndFlush(forrigeKorrigering) } returns korrigertVedtak
        every { korrigertVedtakRepository.save(korrigertVedtak) } returns korrigertVedtak
        every { loggService.opprettKorrigertVedtakLogg(any(), any()) } returns Unit

        korrigertVedtakService.lagreKorrigertVedtak(korrigertVedtak)

        verify(exactly = 1) { korrigertVedtakRepository.finnAktivtKorrigertVedtakPåBehandling(any()) }
        verify(exactly = 1) { forrigeKorrigering setProperty "aktiv" value false }
        verify(exactly = 1) { korrigertVedtakRepository.saveAndFlush(forrigeKorrigering) }
        verify(exactly = 1) { korrigertVedtakRepository.save(korrigertVedtak) }
    }

    @Test
    fun `settKorrigertVedtakPåBehandlingTilInaktiv skal sette korrigert vedtak til inaktivt hvis det finnes`() {
        val behandling = lagBehandling()
        val korrigertVedtak = mockk<KorrigertVedtak>(relaxed = true)

        every { korrigertVedtakRepository.finnAktivtKorrigertVedtakPåBehandling(any()) } returns korrigertVedtak
        every { loggService.opprettKorrigertVedtakLogg(any(), any()) } returns Unit

        korrigertVedtakService.settKorrigertVedtakPåBehandlingTilInaktiv(behandling)

        verify(exactly = 1) { korrigertVedtak setProperty "aktiv" value false }
        verify(exactly = 1) {
            loggService.opprettKorrigertVedtakLogg(
                any(),
                korrigertVedtak,
            )
        }
    }

    fun lagKorrigertVedtak(
        behandling: Behandling,
        vedtaksdato: LocalDate = LocalDate.now().minusDays(6),
        begrunnelse: String? = null,
        aktiv: Boolean = true,
    ) =
        KorrigertVedtak(
            behandling = behandling,
            vedtaksdato = vedtaksdato,
            begrunnelse = begrunnelse,
            aktiv = aktiv,
        )
}
