package no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.historikkinnslag.Aktør
import no.nav.familie.kontrakter.felles.organisasjon.Organisasjon
import no.nav.familie.kontrakter.felles.tilbakekreving.ManuellAdresseInfo
import no.nav.familie.kontrakter.felles.tilbakekreving.MottakerType.DØDSBO
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.api.dto.ManuellBrevmottakerRequestDto
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.FagsakService
import no.nav.familie.tilbake.behandling.ValiderBrevmottakerService
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstilstand
import no.nav.familie.tilbake.behandlingskontroll.domain.Venteårsak
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.domene.ManuellBrevmottaker
import no.nav.familie.tilbake.historikkinnslag.HistorikkService
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.integration.familie.IntegrasjonerClient
import no.nav.familie.tilbake.integration.pdl.PdlClient
import no.nav.familie.tilbake.integration.pdl.internal.Personinfo
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class ManuellBrevmottakerServiceTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var manuellBrevmottakerRepository: ManuellBrevmottakerRepository
    private val mockHistorikkService: HistorikkService = mockk(relaxed = true)

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var behandlingskontrollService: BehandlingskontrollService

    @Autowired
    private lateinit var behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository

    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    @Autowired
    private lateinit var fagsakService: FagsakService

    @Autowired
    private lateinit var validerBrevmottakerService: ValiderBrevmottakerService

    private lateinit var behandling: Behandling
    private lateinit var manuellBrevmottakerService: ManuellBrevmottakerService
    private val opprettetTidspunktSlot = mutableListOf<LocalDateTime>()

    private val manuellBrevmottakerRequestDto = ManuellBrevmottakerRequestDto(
        type = DØDSBO,
        navn = "John Doe",
        manuellAdresseInfo = ManuellAdresseInfo(
            adresselinje1 = "test adresse1",
            adresselinje2 = "test adresse2",
            postnummer = "0000",
            poststed = "Oslo",
            landkode = "NO",
        ),
    )

    private val mockPdlClient: PdlClient = mockk()

    private val mockIntegrasjonerClient: IntegrasjonerClient = mockk()

    @BeforeEach
    fun init() {
        fagsakRepository.insert(Testdata.fagsak)
        behandling = behandlingRepository.insert(Testdata.behandling)

        manuellBrevmottakerService = ManuellBrevmottakerService(
            manuellBrevmottakerRepository = manuellBrevmottakerRepository,
            historikkService = mockHistorikkService,
            behandlingRepository = behandlingRepository,
            behandlingskontrollService = behandlingskontrollService,
            fagsakService = fagsakService,
            pdlClient = mockPdlClient,
            integrasjonerClient = mockIntegrasjonerClient,
            validerBrevmottakerService = validerBrevmottakerService,

        )

        every {
            mockHistorikkService.lagHistorikkinnslag(
                behandlingId = any(),
                historikkinnslagstype = any(),
                aktør = any(),
                opprettetTidspunkt = capture(opprettetTidspunktSlot),
                tittel = any(),
                beskrivelse = any(),
            )
        } just runs

        every { mockPdlClient.hentPersoninfo(any(), any()) } returns Personinfo("12345678901", LocalDate.MIN, "Eldar")
        every { mockIntegrasjonerClient.validerOrganisasjon(any()) } returns true
        every { mockIntegrasjonerClient.hentOrganisasjon("123456789") } returns
            Organisasjon("123456789", navn = "Organisasjon AS")
    }

    @AfterEach
    fun clearSlot() {
        opprettetTidspunktSlot.clear()
    }

    @Test
    fun `leggTilBrevmottaker skal legge til brevmottakere og oppdatere med oppdaterBrevmottaker`() {
        shouldNotThrow<RuntimeException> {
            manuellBrevmottakerService.leggTilBrevmottaker(behandling.id, manuellBrevmottakerRequestDto)
        }
        shouldNotThrow<RuntimeException> {
            manuellBrevmottakerService.leggTilBrevmottaker(
                behandling.id,
                manuellBrevmottakerRequestDto.copy(
                    navn = "Kari Nordmann",
                    manuellAdresseInfo = null,
                    personIdent = "12345678910",
                ),
            )
        }

        var manuellBrevmottakere = manuellBrevmottakerService.hentBrevmottakere(behandling.id)

        manuellBrevmottakere.shouldHaveSize(2)
        val dbManuellBrevmottaker = manuellBrevmottakere.filter { it.navn.equals("John Doe") }.first()
        assertEqualsManuellBrevmottaker(dbManuellBrevmottaker, manuellBrevmottakerRequestDto)

        verify(exactly = 2) {
            mockHistorikkService.lagHistorikkinnslag(
                behandlingId = behandling.id,
                historikkinnslagstype = TilbakekrevingHistorikkinnslagstype.BREVMOTTAKER_LAGT_TIL,
                aktør = Aktør.SAKSBEHANDLER,
                opprettetTidspunkt = or(opprettetTidspunktSlot[0], opprettetTidspunktSlot[1]),
                beskrivelse = any(),
                tittel = any(),
            )
        }

        val oppdatertManuellBrevmottaker = manuellBrevmottakerRequestDto.copy(
            manuellAdresseInfo = ManuellAdresseInfo(
                adresselinje1 = "ny",
                postnummer = "1111",
                poststed = "stavanger",
                landkode = "NO",
            ),
        )
        shouldNotThrow<RuntimeException> {
            manuellBrevmottakerService.oppdaterBrevmottaker(
                behandling.id,
                dbManuellBrevmottaker.id,
                oppdatertManuellBrevmottaker,
            )
        }

        manuellBrevmottakere = manuellBrevmottakerService.hentBrevmottakere(behandling.id)
        manuellBrevmottakere.shouldHaveSize(2)
        val dbOppdatertManuellBrevmottaker = manuellBrevmottakere.filter { it.navn.equals("John Doe") }.first()
        assertEqualsManuellBrevmottaker(dbOppdatertManuellBrevmottaker, oppdatertManuellBrevmottaker)
    }

    @Test
    fun `fjernBrevmottaker fjerner brevmottaker`() {
        shouldNotThrow<RuntimeException> {
            manuellBrevmottakerService.leggTilBrevmottaker(behandling.id, manuellBrevmottakerRequestDto)
        }
        shouldNotThrow<RuntimeException> {
            manuellBrevmottakerService.leggTilBrevmottaker(
                behandling.id,
                manuellBrevmottakerRequestDto.copy(navn = "Kari Nordmann"),
            )
        }

        val manuellBrevmottakere = manuellBrevmottakerService.hentBrevmottakere(behandling.id)

        manuellBrevmottakere.shouldHaveSize(2)
        val dbManuellBrevmottaker = manuellBrevmottakere.filter { it.navn.equals("John Doe") }.first()
        assertEqualsManuellBrevmottaker(dbManuellBrevmottaker, manuellBrevmottakerRequestDto)

        verify(exactly = 2) {
            mockHistorikkService.lagHistorikkinnslag(
                behandlingId = behandling.id,
                historikkinnslagstype = TilbakekrevingHistorikkinnslagstype.BREVMOTTAKER_LAGT_TIL,
                aktør = Aktør.SAKSBEHANDLER,
                opprettetTidspunkt = or(opprettetTidspunktSlot[0], opprettetTidspunktSlot[1]),
                beskrivelse = any(),
                tittel = any(),
            )
        }

        shouldNotThrow<RuntimeException> {
            manuellBrevmottakerService.fjernBrevmottaker(behandling.id, dbManuellBrevmottaker.id)
        }

        manuellBrevmottakerService.hentBrevmottakere(behandling.id).filter { it.navn.equals("John Doe") }
            .shouldBeEmpty()

        verify(exactly = 1) {
            mockHistorikkService.lagHistorikkinnslag(
                behandlingId = behandling.id,
                historikkinnslagstype = TilbakekrevingHistorikkinnslagstype.BREVMOTTAKER_FJERNET,
                aktør = Aktør.SAKSBEHANDLER,
                opprettetTidspunkt = opprettetTidspunktSlot[2],
                beskrivelse = any(),
                tittel = any(),
            )
        }
    }

    @Test
    fun `opprettBrevmottakerSteg skal opprette og autoutføre behandlingssteg BREVMOTTAKER`() {
        manuellBrevmottakerService.opprettBrevmottakerSteg(behandling.id)
        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        behandlingsstegstilstand.shouldHaveSingleElement {
            it.behandlingssteg == Behandlingssteg.BREVMOTTAKER &&
                it.behandlingsstegsstatus == Behandlingsstegstatus.AUTOUTFØRT
        }
    }

    @Test
    fun `opprettBrevmottakerSteg skal ikke opprette steg når behandling er avsluttet`() {
        behandlingRepository.update(behandling.copy(status = Behandlingsstatus.AVSLUTTET))

        val exception = shouldThrow<RuntimeException> { manuellBrevmottakerService.opprettBrevmottakerSteg(behandling.id) }
        exception.message shouldBe "Behandling med id=${behandling.id} er allerede ferdig behandlet."
    }

    @Test
    fun `opprettBrevmottakerSteg skal ikke opprette steg når behandling er på vent`() {
        val behandling = behandlingRepository.findByIdOrThrow(Testdata.behandling.id)
        lagBehandlingsstegstilstand(behandling.id, Behandlingssteg.FAKTA, Behandlingsstegstatus.KLAR)

        behandlingskontrollService.settBehandlingPåVent(
            behandling.id,
            Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG,
            LocalDate.now().plusWeeks(4),
        )

        val exception = shouldThrow<RuntimeException> { manuellBrevmottakerService.opprettBrevmottakerSteg(behandling.id) }
        exception.message shouldBe "Behandling med id=${behandling.id} er på vent."
    }

    @Test
    fun `fjernManuelleBrevmottakereOgTilbakeførSteg skal fjerne brevmottakere og tilbakeføre steget`() {
        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)
        lagBehandlingsstegstilstand(behandling.id, Behandlingssteg.FAKTA, Behandlingsstegstatus.KLAR)

        manuellBrevmottakerService.opprettBrevmottakerSteg(behandling.id)
        manuellBrevmottakerService.leggTilBrevmottaker(behandling.id, manuellBrevmottakerRequestDto)
        manuellBrevmottakerService.hentBrevmottakere(behandling.id).shouldHaveSize(1)

        manuellBrevmottakerService.fjernManuelleBrevmottakereOgTilbakeførSteg(behandling.id)
        manuellBrevmottakerService.hentBrevmottakere(behandling.id).shouldHaveSize(0)

        behandlingsstegstilstandRepository.findByBehandlingId(behandling.id).shouldHaveSingleElement {
            it.behandlingssteg == Behandlingssteg.BREVMOTTAKER &&
                it.behandlingsstegsstatus == Behandlingsstegstatus.TILBAKEFØRT
        }
    }

    @Test
    fun `skal hente og legge til navn fra registeroppslag når request inneholder identinformasjon`() {
        val requestMedPersonIdent = manuellBrevmottakerRequestDto.copy(
            personIdent = "12345678910",
            manuellAdresseInfo = null,
        )
        manuellBrevmottakerService.leggTilBrevmottaker(behandling.id, requestMedPersonIdent)

        var lagretMottaker = manuellBrevmottakerService.hentBrevmottakere(behandling.id).single()
        lagretMottaker.navn shouldBe mockPdlClient.hentPersoninfo("12345678910", Fagsystem.BA).navn

        val requestMedOrgnrUtenKontaktperson = manuellBrevmottakerRequestDto.copy(
            navn = " ",
            organisasjonsnummer = "123456789",
            manuellAdresseInfo = null,
        )
        manuellBrevmottakerService.oppdaterBrevmottaker(behandling.id, lagretMottaker.id, requestMedOrgnrUtenKontaktperson)

        lagretMottaker = manuellBrevmottakerService.hentBrevmottakere(behandling.id).single()
        lagretMottaker.navn shouldBe "Organisasjon AS"

        val requestMedOrgnrMedKontaktperson = manuellBrevmottakerRequestDto.copy(
            organisasjonsnummer = "123456789",
            manuellAdresseInfo = null,
        )
        manuellBrevmottakerService.oppdaterBrevmottaker(behandling.id, lagretMottaker.id, requestMedOrgnrMedKontaktperson)

        lagretMottaker = manuellBrevmottakerService.hentBrevmottakere(behandling.id).single()
        lagretMottaker.navn shouldBe "Organisasjon AS v/ ${manuellBrevmottakerRequestDto.navn}"
    }

    private fun assertEqualsManuellBrevmottaker(a: ManuellBrevmottaker, b: ManuellBrevmottakerRequestDto) {
        a.id.shouldNotBeNull()
        a.orgNr shouldBe b.organisasjonsnummer
        a.ident shouldBe b.personIdent
        a.type shouldBe b.type
        a.vergetype shouldBe b.vergetype
        a.adresselinje1 shouldBe b.manuellAdresseInfo?.adresselinje1
        a.adresselinje2 shouldBe b.manuellAdresseInfo?.adresselinje2
        a.postnummer shouldBe b.manuellAdresseInfo?.postnummer
        a.poststed shouldBe b.manuellAdresseInfo?.poststed
        a.landkode shouldBe b.manuellAdresseInfo?.landkode
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
}
