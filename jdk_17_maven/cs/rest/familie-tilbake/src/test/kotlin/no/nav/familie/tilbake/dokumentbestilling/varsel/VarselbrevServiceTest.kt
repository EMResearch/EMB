package no.nav.familie.tilbake.dokumentbestilling.varsel

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.Språkkode
import no.nav.familie.kontrakter.felles.tilbakekreving.FeilutbetaltePerioderDto
import no.nav.familie.kontrakter.felles.tilbakekreving.ForhåndsvisVarselbrevRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.Periode
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Verge
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.dokumentbestilling.DistribusjonshåndteringService
import no.nav.familie.tilbake.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.dokumentbestilling.felles.EksterneDataForBrevService
import no.nav.familie.tilbake.dokumentbestilling.felles.pdf.PdfBrevService
import no.nav.familie.tilbake.integration.pdl.internal.Personinfo
import no.nav.familie.tilbake.pdfgen.validering.PdfaValidator
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

internal class VarselbrevServiceTest : OppslagSpringRunnerTest() {

    private val fagsakRepository: FagsakRepository = mockk()
    private val eksterneDataForBrevService: EksterneDataForBrevService = mockk(relaxed = true)
    private val distribusjonshåndteringService: DistribusjonshåndteringService = mockk()

    @Autowired
    private lateinit var pdfBrevService: PdfBrevService

    @Autowired
    private lateinit var varselbrevUtil: VarselbrevUtil

    private lateinit var varselbrevService: VarselbrevService

    @BeforeEach
    fun init() {
        varselbrevService = VarselbrevService(
            fagsakRepository,
            eksterneDataForBrevService,
            pdfBrevService,
            varselbrevUtil,
            distribusjonshåndteringService,
        )

        val personinfo = Personinfo("28056325874", LocalDate.now(), "Fiona")

        every { eksterneDataForBrevService.hentPerson(Testdata.fagsak.bruker.ident, any()) }.returns(personinfo)
        every {
            eksterneDataForBrevService.hentAdresse(any(), any(), any<Verge>(), any())
        }.returns(Adresseinfo("12345678901", "Test"))
    }

    @Test
    fun hentForhåndsvisningVarselbrev() {
        val forhåndsvisVarselbrevRequest =
            ForhåndsvisVarselbrevRequest(
                "Dette er et varsel!",
                Ytelsestype.BARNETRYGD,
                "1570",
                "Bodø",
                "321321",
                Språkkode.NN,
                LocalDate.now(),
                FeilutbetaltePerioderDto(
                    157468,
                    listOf(
                        Periode(
                            LocalDate.of(2020, 5, 4),
                            LocalDate.now(),
                        ),
                    ),
                ),
                Fagsystem.EF,
                "321654",
                Testdata.fagsak.bruker.ident,
                null,
                fagsystemsbehandlingId = "123",
            )

        val bytes = varselbrevService.hentForhåndsvisningVarselbrev(forhåndsvisVarselbrevRequest)
//        File("test.pdf").writeBytes(bytes)

        PdfaValidator.validatePdf(bytes)
    }
}
