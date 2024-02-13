package no.nav.familie.tilbake.dokumentbestilling

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.equals.shouldNotBeEqual
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import no.nav.familie.kontrakter.felles.tilbakekreving.MottakerType
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.config.FeatureToggleService
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevmetadataUtil
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmottager.BRUKER
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmottager.MANUELL_BRUKER
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmottager.MANUELL_TILLEGGSMOTTAKER
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmottager.VERGE
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevsporingService
import no.nav.familie.tilbake.dokumentbestilling.felles.EksterneDataForBrevService
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.dokumentbestilling.felles.pdf.Brevdata
import no.nav.familie.tilbake.dokumentbestilling.felles.pdf.JournalføringService
import no.nav.familie.tilbake.dokumentbestilling.felles.pdf.PdfBrevService
import no.nav.familie.tilbake.dokumentbestilling.henleggelse.HenleggelsesbrevService
import no.nav.familie.tilbake.dokumentbestilling.henleggelse.SendHenleggelsesbrevTask
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.ManuellBrevmottakerRepository
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.domene.ManuellBrevmottaker
import no.nav.familie.tilbake.dokumentbestilling.vedtak.VedtaksbrevgunnlagService
import no.nav.familie.tilbake.integration.pdl.internal.Personinfo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

class DistribusjonshåndteringServiceTest {

    private val behandlingRepository: BehandlingRepository = mockk()
    private val fagsakRepository: FagsakRepository = mockk()
    private val manuelleBrevmottakerRepository: ManuellBrevmottakerRepository = mockk(relaxed = true)
    private val journalføringService: JournalføringService = mockk(relaxed = true)
    private val featureToggleService: FeatureToggleService = mockk()
    private val eksterneDataForBrevService: EksterneDataForBrevService = mockk()
    private val vedtaksbrevgrunnlagService: VedtaksbrevgunnlagService = mockk()

    private val pdfBrevService = spyk(
        PdfBrevService(
            journalføringService = journalføringService,
            tellerService = mockk(relaxed = true),
            taskService = mockk(relaxed = true),
        ),
    )
    private val brevmetadataUtil = BrevmetadataUtil(
        behandlingRepository = behandlingRepository,
        fagsakRepository = fagsakRepository,
        manuelleBrevmottakerRepository = manuelleBrevmottakerRepository,
        eksterneDataForBrevService = eksterneDataForBrevService,
        organisasjonService = mockk(),
        featureToggleService = featureToggleService,
    )
    private val distribusjonshåndteringService = DistribusjonshåndteringService(
        brevmetadataUtil = brevmetadataUtil,
        fagsakRepository = fagsakRepository,
        manuelleBrevmottakerRepository = manuelleBrevmottakerRepository,
        pdfBrevService = pdfBrevService,
        vedtaksbrevgrunnlagService = vedtaksbrevgrunnlagService,
        featureToggleService = featureToggleService,
    )
    private val brevsporingService: BrevsporingService = mockk()
    private val henleggelsesbrevService = HenleggelsesbrevService(
        behandlingRepository = behandlingRepository,
        brevsporingService = brevsporingService,
        fagsakRepository = fagsakRepository,
        eksterneDataForBrevService = eksterneDataForBrevService,
        pdfBrevService = pdfBrevService,
        organisasjonService = mockk(),
        distribusjonshåndteringService = distribusjonshåndteringService,
        brevmetadataUtil = brevmetadataUtil,
    )
    private val sendHenleggelsesbrevTask = SendHenleggelsesbrevTask(
        henleggelsesbrevService = henleggelsesbrevService,
        behandlingRepository = behandlingRepository,
        fagsakRepository = fagsakRepository,
        featureToggleService = featureToggleService,
    )

    private val behandling = Testdata.behandling
    private val fagsak = Testdata.fagsak
    private val personinfoBruker = Personinfo(fagsak.bruker.ident, LocalDate.now(), navn = "brukernavn")
    private val brukerAdresse = Adresseinfo(personinfoBruker.ident, personinfoBruker.navn)
    private val verge = behandling.aktivVerge!!
    private val vergeAdresse = Adresseinfo(verge.ident!!, verge.navn)

    @BeforeEach
    fun setUp() {
        every { behandlingRepository.findById(any()) } returns Optional.of(behandling)
        every { fagsakRepository.findById(any()) } returns Optional.of(fagsak)
        every { eksterneDataForBrevService.hentPerson(fagsak.bruker.ident, fagsak.fagsystem) } returns
            personinfoBruker
        every {
            eksterneDataForBrevService.hentAdresse(personinfoBruker, BRUKER, behandling.aktivVerge, any())
        } returns brukerAdresse
        every {
            eksterneDataForBrevService.hentAdresse(personinfoBruker, VERGE, behandling.aktivVerge, any())
        } returns vergeAdresse
        every { eksterneDataForBrevService.hentSaksbehandlernavn(any()) } returns behandling.ansvarligSaksbehandler
        every { eksterneDataForBrevService.hentPåloggetSaksbehandlernavnMedDefault(any()) } returns behandling.ansvarligSaksbehandler
        every { brevsporingService.finnSisteVarsel(any()) } returns Testdata.brevsporing
        every { featureToggleService.isEnabled(any()) } returns false
    }

    @Test
    fun `skal kun sende til bruker`() {
        val behandlingUtenVerge = behandling.copy(verger = emptySet())

        every { behandlingRepository.findById(any()) } returns Optional.of(behandlingUtenVerge)
        every {
            eksterneDataForBrevService.hentAdresse(personinfoBruker, BRUKER, behandlingUtenVerge.aktivVerge, any())
        } returns brukerAdresse

        val task = SendHenleggelsesbrevTask.opprettTask(behandling.id, fagsak.fagsystem, "fritekst")
        val brevdata = mutableListOf<Brevdata>()

        sendHenleggelsesbrevTask.doTask(task)

        verify {
            pdfBrevService.sendBrev(
                behandlingUtenVerge,
                fagsak,
                Brevtype.HENLEGGELSE,
                capture(brevdata),
            )
        }

        val metadata = brevdata.single().metadata

        metadata.mottageradresse shouldBeEqual brukerAdresse

        metadata.finnesVerge.shouldBeFalse()
        metadata.finnesAnnenMottaker.shouldBeFalse()

        metadata.annenMottakersNavn.isNullOrEmpty().shouldBeTrue()
        metadata.vergenavn.isNullOrEmpty().shouldBeTrue()
    }

    @Test
    fun `skal journalføre og sende brev med samme brødtekst til både manuell bruker og manuell tilleggsmottaker`() {
        val behandlingId = UUID.randomUUID()
        val behandlingMedManuelleBrevmottakere = behandling.copy(id = behandlingId, verger = emptySet())

        every { behandlingRepository.findById(behandlingId) } returns Optional.of(behandlingMedManuelleBrevmottakere)
        every { manuelleBrevmottakerRepository.findByBehandlingId(behandlingId) } returns listOf(
            ManuellBrevmottaker(
                type = MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE,
                behandlingId = behandlingId,
                navn = personinfoBruker.navn,
                adresselinje1 = "adresselinje1",
                postnummer = "postnummer",
                poststed = "poststed",
                landkode = "NO",
            ),
            ManuellBrevmottaker(
                type = MottakerType.VERGE,
                behandlingId = behandlingId,
                navn = verge.navn,
                ident = verge.ident,
            ),
        )

        val task = SendHenleggelsesbrevTask.opprettTask(behandlingId, fagsak.fagsystem, "fritekst")
        val brevdata = mutableListOf<Brevdata>()
        val eksternReferanseIdVedJournalføring = mutableListOf<String>()

        sendHenleggelsesbrevTask.doTask(task)

        verify(exactly = 2) {
            pdfBrevService.sendBrev(
                behandling = behandlingMedManuelleBrevmottakere,
                fagsak = fagsak,
                brevtype = Brevtype.HENLEGGELSE,
                data = capture(brevdata),
            )
            journalføringService.journalførUtgåendeBrev(
                behandling = behandlingMedManuelleBrevmottakere,
                fagsak = fagsak,
                dokumentkategori = any(),
                brevmetadata = any(),
                brevmottager = any(),
                vedleggPdf = any(),
                eksternReferanseId = capture(eksternReferanseIdVedJournalføring),
            )
        }

        eksternReferanseIdVedJournalføring.first() shouldNotBeEqual eksternReferanseIdVedJournalføring.last()
        eksternReferanseIdVedJournalføring.all {
            it.contains("manuell_bruker") || it.contains("manuell_tilleggsmottaker")
        }

        val brevdataTilBruker = brevdata.first { it.mottager == MANUELL_BRUKER }
        val brevdataTilManuellVerge = brevdata.first { it.mottager == MANUELL_TILLEGGSMOTTAKER }

        val (brødtekstTilManuellVerge, annenMottakerOppgittTilVerge) = brevdataTilManuellVerge.brevtekst
            .split("Brev med likt innhold er sendt til ")
        val (brødtekstTilBruker, annenMottakerOppgittTilBruker) = brevdataTilBruker.brevtekst
            .split("Brev med likt innhold er sendt til ")

        brødtekstTilManuellVerge shouldBeEqual brødtekstTilBruker

        annenMottakerOppgittTilBruker shouldBeEqual verge.navn
        annenMottakerOppgittTilVerge shouldBeEqual personinfoBruker.navn
    }
}
