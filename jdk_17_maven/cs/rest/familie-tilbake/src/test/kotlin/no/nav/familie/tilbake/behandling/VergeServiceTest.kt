package no.nav.familie.tilbake.behandling

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.kontrakter.felles.Applikasjon
import no.nav.familie.kontrakter.felles.historikkinnslag.Aktør
import no.nav.familie.kontrakter.felles.tilbakekreving.Vergetype
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.api.dto.VergeDto
import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.behandling.event.EndretPersonIdentEventPublisher
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstilstand
import no.nav.familie.tilbake.behandlingskontroll.domain.Venteårsak
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.historikkinnslag.HistorikkTaskService
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.integration.familie.IntegrasjonerClient
import no.nav.familie.tilbake.integration.pdl.PdlClient
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.person.PersonService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.UUID

internal class VergeServiceTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository

    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    @Autowired
    private lateinit var behandlingskontrollService: BehandlingskontrollService

    @Autowired
    private lateinit var integrasjonerClient: IntegrasjonerClient

    @Autowired
    private lateinit var personService: PersonService

    private lateinit var vergeService: VergeService

    private val historikkTaskService: HistorikkTaskService = mockk(relaxed = true)

    private val vergeDto = VergeDto(
        orgNr = "987654321",
        type = Vergetype.ADVOKAT,
        navn = "Stor Herlig Straff",
        begrunnelse = "Det var nødvendig",
    )

    @BeforeEach
    fun setUp() {
        fagsakRepository.insert(Testdata.fagsak)
        behandlingRepository.insert(Testdata.behandling)
        vergeService = VergeService(
            behandlingRepository,
            fagsakRepository,
            historikkTaskService,
            behandlingskontrollService,
            integrasjonerClient,
            personService,
        )
        clearAllMocks(answers = false)
    }

    @Test
    fun `lagreVerge skal lagre verge i basen`() {
        vergeService.lagreVerge(Testdata.behandling.id, vergeDto)

        val behandling = behandlingRepository.findByIdOrThrow(Testdata.behandling.id)
        val verge = behandling.aktivVerge!!
        verge.aktiv shouldBe true
        verge.orgNr shouldBe "987654321"
        verge.type shouldBe Vergetype.ADVOKAT
        verge.navn shouldBe "Stor Herlig Straff"
        verge.kilde shouldBe Applikasjon.FAMILIE_TILBAKE.name
        verge.begrunnelse shouldBe "Det var nødvendig"
    }

    @Test
    fun `lagreVerge skal deaktivere eksisterende verger i basen`() {
        val behandlingFørOppdatering = behandlingRepository.findByIdOrThrow(Testdata.behandling.id)
        val gammelVerge = behandlingFørOppdatering.aktivVerge!!

        vergeService.lagreVerge(Testdata.behandling.id, vergeDto)

        val behandling = behandlingRepository.findByIdOrThrow(Testdata.behandling.id)
        val deaktivertVerge = behandling.verger.first { !it.aktiv }
        deaktivertVerge.id shouldBe gammelVerge.id
    }

    @Test
    fun `lagreVerge skal kalle historikkTaskService for å opprette historikkTask`() {
        vergeService.lagreVerge(Testdata.behandling.id, vergeDto)

        val behandling = behandlingRepository.findByIdOrThrow(Testdata.behandling.id)

        verify {
            historikkTaskService.lagHistorikkTask(
                behandling.id,
                TilbakekrevingHistorikkinnslagstype.VERGE_OPPRETTET,
                Aktør.SAKSBEHANDLER,
            )
        }
    }

    @Test
    fun `lagreVerge skal ikke lagre verge når organisasjonen er tom`() {
        val vergeDto = vergeDto.copy(orgNr = null, ident = "123")
        val exception = shouldThrow<RuntimeException> { vergeService.lagreVerge(Testdata.behandling.id, vergeDto) }
        exception.message shouldBe "orgNr kan ikke være null for ${Vergetype.ADVOKAT}"
    }

    @Test
    fun `lagreVerge skal ikke lagre verge når organisasjonen ikke er gyldig`() {
        val mockIntegrasjonerClient = mockk<IntegrasjonerClient>()
        val vergeService = VergeService(
            behandlingRepository,
            fagsakRepository,
            historikkTaskService,
            behandlingskontrollService,
            mockIntegrasjonerClient,
            personService,
        )

        every { mockIntegrasjonerClient.validerOrganisasjon(any()) } returns false

        val exception = shouldThrow<RuntimeException> { vergeService.lagreVerge(Testdata.behandling.id, vergeDto) }
        exception.message shouldBe "Organisasjon ${vergeDto.orgNr} er ikke gyldig"
    }

    @Test
    fun `lagreVerge skal ikke lagre verge når ident er tom`() {
        val vergeDto = vergeDto.copy(type = Vergetype.VERGE_FOR_BARN)
        val exception = shouldThrow<RuntimeException> { vergeService.lagreVerge(Testdata.behandling.id, vergeDto) }
        exception.message shouldBe "ident kan ikke være null for ${vergeDto.type}"
    }

    @Test
    fun `lagreVerge skal ikke lagre verge når personen ikke finnes i PDL`() {
        val mockPdlClient = mockk<PdlClient>()
        val mockEndretPersonIdentEventPublisher: EndretPersonIdentEventPublisher = mockk()
        val personService = PersonService(mockPdlClient, fagsakRepository, mockEndretPersonIdentEventPublisher)
        val vergeService = VergeService(
            behandlingRepository,
            fagsakRepository,
            historikkTaskService,
            behandlingskontrollService,
            integrasjonerClient,
            personService,
        )

        every { mockPdlClient.hentPersoninfo(any(), any()) } throws Feil(message = "Feil ved oppslag på person")

        val vergeDto = VergeDto(ident = "123", type = Vergetype.VERGE_FOR_BARN, navn = "testverdi", begrunnelse = "testverdi")
        val exception = shouldThrow<RuntimeException> { vergeService.lagreVerge(Testdata.behandling.id, vergeDto) }
        exception.message shouldBe "Feil ved oppslag på person"
    }

    @Test
    fun `fjernVerge skal deaktivere verge i basen hvis det finnes aktiv verge`() {
        val behandlingFørOppdatering = behandlingRepository.findByIdOrThrow(Testdata.behandling.id)
        val gammelVerge = behandlingFørOppdatering.aktivVerge!!

        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)

        lagBehandlingsstegstilstand(behandlingFørOppdatering.id, Behandlingssteg.VARSEL, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(behandlingFørOppdatering.id, Behandlingssteg.GRUNNLAG, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(behandlingFørOppdatering.id, Behandlingssteg.VERGE, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(behandlingFørOppdatering.id, Behandlingssteg.FAKTA, Behandlingsstegstatus.KLAR)

        vergeService.fjernVerge(Testdata.behandling.id)

        val behandling = behandlingRepository.findByIdOrThrow(Testdata.behandling.id)
        val deaktivertVerge = behandling.verger.first()
        deaktivertVerge.id shouldBe gammelVerge.id
        deaktivertVerge.aktiv shouldBe false

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.VARSEL, Behandlingsstegstatus.UTFØRT)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.GRUNNLAG, Behandlingsstegstatus.UTFØRT)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.VERGE, Behandlingsstegstatus.TILBAKEFØRT)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.FAKTA, Behandlingsstegstatus.KLAR)
    }

    @Test
    fun `fjernVerge skal tilbakeføre verge steg når behandling er på vilkårsvurdering steg og verge fjernet`() {
        val behandlingFørOppdatering = behandlingRepository.findByIdOrThrow(Testdata.behandling.id)
        val gammelVerge = behandlingFørOppdatering.aktivVerge
        gammelVerge.shouldNotBeNull()

        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)

        lagBehandlingsstegstilstand(behandlingFørOppdatering.id, Behandlingssteg.VARSEL, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(behandlingFørOppdatering.id, Behandlingssteg.VERGE, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(behandlingFørOppdatering.id, Behandlingssteg.FAKTA, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(behandlingFørOppdatering.id, Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.KLAR)

        vergeService.fjernVerge(Testdata.behandling.id)

        behandlingRepository.findByIdOrThrow(behandlingFørOppdatering.id).harVerge.shouldBeFalse()
        verify {
            historikkTaskService.lagHistorikkTask(
                behandlingFørOppdatering.id,
                TilbakekrevingHistorikkinnslagstype.VERGE_FJERNET,
                Aktør.SAKSBEHANDLER,
            )
        }
        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandlingFørOppdatering.id)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.VARSEL, Behandlingsstegstatus.UTFØRT)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.VERGE, Behandlingsstegstatus.TILBAKEFØRT)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.FAKTA, Behandlingsstegstatus.UTFØRT)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.KLAR)
    }

    @Test
    fun `fjernVerge skal tilbakeføre verge steg og fortsette til fakta når behandling er på verge steg og verge fjernet`() {
        val behandlingFørOppdatering = behandlingRepository.findByIdOrThrow(Testdata.behandling.id)
        val behandlingUtenVerge = behandlingRepository.update(behandlingFørOppdatering.copy(verger = emptySet()))

        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)

        lagBehandlingsstegstilstand(behandlingUtenVerge.id, Behandlingssteg.VARSEL, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(behandlingUtenVerge.id, Behandlingssteg.GRUNNLAG, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(behandlingUtenVerge.id, Behandlingssteg.VERGE, Behandlingsstegstatus.KLAR)

        vergeService.fjernVerge(behandlingUtenVerge.id)

        behandlingUtenVerge.harVerge.shouldBeFalse()
        verify(exactly = 0) { historikkTaskService.lagHistorikkTask(any(), any(), any(), any()) }

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandlingUtenVerge.id)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.VARSEL, Behandlingsstegstatus.UTFØRT)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.GRUNNLAG, Behandlingsstegstatus.UTFØRT)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.VERGE, Behandlingsstegstatus.TILBAKEFØRT)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.FAKTA, Behandlingsstegstatus.KLAR)
    }

    @Test
    fun `opprettVergeSteg skal opprette verge steg når behandling er på vilkårsvurdering steg`() {
        val behandling = behandlingRepository.findByIdOrThrow(Testdata.behandling.id)
        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)

        lagBehandlingsstegstilstand(behandling.id, Behandlingssteg.FAKTA, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(behandling.id, Behandlingssteg.FORELDELSE, Behandlingsstegstatus.AUTOUTFØRT)
        lagBehandlingsstegstilstand(behandling.id, Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.KLAR)

        vergeService.opprettVergeSteg(behandling.id)
        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.FAKTA, Behandlingsstegstatus.TILBAKEFØRT)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.FORELDELSE, Behandlingsstegstatus.TILBAKEFØRT)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.TILBAKEFØRT)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.VERGE, Behandlingsstegstatus.KLAR)
    }

    @Test
    fun `opprettVergeSteg skal ikke opprette verge steg når behandling er avsluttet`() {
        val behandling = behandlingRepository.findByIdOrThrow(Testdata.behandling.id)
        behandlingRepository.update(behandling.copy(status = Behandlingsstatus.AVSLUTTET))

        val exception = shouldThrow<RuntimeException> { vergeService.opprettVergeSteg(behandling.id) }
        exception.message shouldBe "Behandling med id=${behandling.id} er allerede ferdig behandlet."
    }

    @Test
    fun `opprettVergeSteg skal ikke opprette verge steg når behandling er på vent`() {
        val behandling = behandlingRepository.findByIdOrThrow(Testdata.behandling.id)
        lagBehandlingsstegstilstand(behandling.id, Behandlingssteg.FAKTA, Behandlingsstegstatus.KLAR)
        behandlingskontrollService.settBehandlingPåVent(
            behandling.id,
            Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG,
            LocalDate.now().plusWeeks(4),
        )

        val exception = shouldThrow<RuntimeException> { vergeService.opprettVergeSteg(behandling.id) }
        exception.message shouldBe "Behandling med id=${behandling.id} er på vent."
    }

    @Test
    fun `hentVerge skal returnere lagret verge data`() {
        val aktivVerge = Testdata.behandling.aktivVerge
        aktivVerge.shouldNotBeNull()

        val respons = vergeService.hentVerge(Testdata.behandling.id)

        respons.shouldNotBeNull()
        respons.begrunnelse shouldBe aktivVerge.begrunnelse
        respons.type shouldBe aktivVerge.type
        respons.ident shouldBe aktivVerge.ident
        respons.navn shouldBe aktivVerge.navn
        respons.orgNr shouldBe aktivVerge.orgNr
    }

    private fun lagBehandlingsstegstilstand(
        behandlingId: UUID,
        behandlingssteg: Behandlingssteg,
        behandlingsstegstatus: Behandlingsstegstatus,
    ) {
        behandlingsstegstilstandRepository.insert(
            Behandlingsstegstilstand(
                behandlingId = behandlingId,
                behandlingssteg = behandlingssteg,
                behandlingsstegsstatus = behandlingsstegstatus,
            ),
        )
    }

    private fun assertBehandlingssteg(
        behandlingsstegstilstand: List<Behandlingsstegstilstand>,
        behandlingssteg: Behandlingssteg,
        behandlingsstegstatus: Behandlingsstegstatus,
    ) {
        behandlingsstegstilstand.shouldHaveSingleElement {
            behandlingssteg == it.behandlingssteg &&
                behandlingsstegstatus == it.behandlingsstegsstatus
        }
    }
}
