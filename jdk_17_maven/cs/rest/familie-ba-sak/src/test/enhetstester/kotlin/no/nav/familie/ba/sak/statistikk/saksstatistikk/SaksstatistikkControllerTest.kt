package no.nav.familie.ba.sak.statistikk.saksstatistikk

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagring
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagringRepository
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagringType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class SaksstatistikkControllerTest {

    val saksstatistikkMellomlagringRepository: SaksstatistikkMellomlagringRepository = mockk()
    val saksstatistikkService: SaksstatistikkService = mockk()

    val controller: SaksstatistikkController =
        SaksstatistikkController(saksstatistikkService, saksstatistikkMellomlagringRepository)

    @BeforeEach
    fun init() {
    }

    @Test
    fun `Skal lagre saksstatistikk sak til repository med sendttidspunkt`() {
        val request = SaksstatistikkController.SaksstatistikkSendtRequest(
            offset = 45635,
            type = SaksstatistikkMellomlagringType.SAK,
            sendtTidspunkt = LocalDateTime.now(),
            json = """{"sakId": 123456789, "versjon": "1.0", "funksjonellId": "aaa-bbb-ccc"}""",
        )

        val slot = slot<SaksstatistikkMellomlagring>()
        every { saksstatistikkMellomlagringRepository.saveAndFlush(capture(slot)) } returns mockk()
        controller.registrerSendtFraStatistikk(request)

        assertThat(slot.captured.offsetVerdiOnPrem).isEqualTo(request.offset)
        assertThat(slot.captured.type).isEqualTo(SaksstatistikkMellomlagringType.SAK)
        assertThat(slot.captured.sendtTidspunkt).isCloseTo(
            LocalDateTime.now(),
            within(10, ChronoUnit.SECONDS),
        )
        assertThat(slot.captured.funksjonellId).isEqualTo("aaa-bbb-ccc")
        assertThat(slot.captured.typeId).isEqualTo(123456789)
        assertThat(slot.captured.kontraktVersjon).isEqualTo("1.0")
    }

    @Test
    fun `Skal lagre saksstatistikk behandling til repository med sendttidspunkt`() {
        val request = SaksstatistikkController.SaksstatistikkSendtRequest(
            offset = 45635,
            type = SaksstatistikkMellomlagringType.BEHANDLING,
            sendtTidspunkt = LocalDateTime.now(),
            json = """{"behandlingId": 123456789, "versjon": "1.0", "funksjonellId": "aaa-bbb-ccc"}""",
        )

        val slot = slot<SaksstatistikkMellomlagring>()
        every { saksstatistikkMellomlagringRepository.saveAndFlush(capture(slot)) } returns mockk()

        controller.registrerSendtFraStatistikk(request)

        assertThat(slot.captured.offsetVerdiOnPrem).isEqualTo(request.offset)
        assertThat(slot.captured.type).isEqualTo(SaksstatistikkMellomlagringType.BEHANDLING)
        assertThat(slot.captured.sendtTidspunkt).isCloseTo(
            LocalDateTime.now(),
            within(10, ChronoUnit.SECONDS),
        )
        assertThat(slot.captured.funksjonellId).isEqualTo("aaa-bbb-ccc")
        assertThat(slot.captured.typeId).isEqualTo(123456789)
        assertThat(slot.captured.kontraktVersjon).isEqualTo("1.0")
    }
}
