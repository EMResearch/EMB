package no.nav.familie.ba.sak.kjerne.brev

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class DokumentControllerTest(
    @Autowired
    private val dokumentService: DokumentService,
    @Autowired
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
) : AbstractSpringIntegrationTest() {

    private val mockDokumentGenereringService: DokumentGenereringService = mockk()
    private val mockDokumentService: DokumentService = mockk()
    private val vedtakService: VedtakService = mockk(relaxed = true)
    private val fagsakService: FagsakService = mockk()
    private val tilgangService: TilgangService = mockk(relaxed = true)
    val mockDokumentController =
        DokumentController(
            dokumentGenereringService = mockDokumentGenereringService,
            dokumentService = mockDokumentService,
            vedtakService = vedtakService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            fagsakService = fagsakService,
            tilgangService = tilgangService,
            persongrunnlagService = mockk(relaxed = true),
            arbeidsfordelingService = mockk(relaxed = true),
            utvidetBehandlingService = mockk(relaxed = true),
        )

    @Test
    @Tag("integration")
    fun `Test generer vedtaksbrev`() {
        every { vedtakService.hent(any()) } returns lagVedtak()
        every { mockDokumentGenereringService.genererBrevForVedtak(any()) } returns "pdf".toByteArray()

        val response = mockDokumentController.genererVedtaksbrev(1)
        assert(response.status == Ressurs.Status.SUKSESS)
    }

    @Test
    @Tag("integration")
    fun `Test hent pdf vedtak`() {
        every { vedtakService.hent(any()) } returns lagVedtak(stønadBrevPdF = "pdf".toByteArray())
        every { mockDokumentService.hentBrevForVedtak(any()) } returns Ressurs.success("pdf".toByteArray())

        val response = mockDokumentController.hentVedtaksbrev(1)
        assert(response.status == Ressurs.Status.SUKSESS)
    }

    @Test
    @Tag("integration")
    fun `Kast feil ved hent av vedtaksbrev når det ikke er generert brev`() {
        assertThrows<Feil> {
            dokumentService.hentBrevForVedtak(lagVedtak())
        }
    }
}
