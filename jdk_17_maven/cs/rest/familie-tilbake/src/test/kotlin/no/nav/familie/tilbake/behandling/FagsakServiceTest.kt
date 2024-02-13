package no.nav.familie.tilbake.behandling

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.Språkkode
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.behandling.domain.Behandlingstype
import no.nav.familie.tilbake.behandling.domain.Bruker
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandling.domain.Institusjon
import no.nav.familie.tilbake.behandling.task.OpprettBehandlingManueltTask
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.integration.pdl.internal.Kjønn
import no.nav.familie.tilbake.kravgrunnlag.ØkonomiXmlMottattRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.exchange
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.util.UriComponentsBuilder
import java.time.LocalDate
import java.util.Properties
import java.util.UUID

internal class FagsakServiceTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var økonomiXmlMottattRepository: ØkonomiXmlMottattRepository

    @Autowired
    private lateinit var taskService: TaskService

    @Autowired
    private lateinit var fagsakService: FagsakService

    @Test
    fun test() {
        headers.setBearerAuth(lokalTestToken())
        val uriHentSaksnummer = UriComponentsBuilder.fromHttpUrl(localhost("/api/fagsystem/EF/fagsak/123456/v1")).toUriString()

        val response: ResponseEntity<Ressurs<Map<String, String>>> = restTemplate.exchange(
            uriHentSaksnummer,
            HttpMethod.GET,
            HttpEntity<String>(headers),
        )

        println(response)
    }

    @Test
    fun `hentFagsak skal hente fagsak for barnetrygd`() {
        val eksternFagsakId = UUID.randomUUID().toString()
        val behandling = opprettBehandling(Ytelsestype.BARNETRYGD, eksternFagsakId)

        val fagsakDto = fagsakService.hentFagsak(Fagsystem.BA, eksternFagsakId)

        fagsakDto.eksternFagsakId shouldBe eksternFagsakId
        fagsakDto.språkkode shouldBe Språkkode.NB
        fagsakDto.ytelsestype shouldBe Ytelsestype.BARNETRYGD
        fagsakDto.fagsystem shouldBe Fagsystem.BA
        fagsakDto.institusjon shouldBe null

        val brukerDto = fagsakDto.bruker
        brukerDto.personIdent shouldBe "32132132111"
        brukerDto.navn shouldBe "testverdi"
        brukerDto.kjønn shouldBe Kjønn.MANN
        brukerDto.fødselsdato shouldBe LocalDate.now().minusYears(20)
        brukerDto.dødsdato shouldBe null

        val behandlinger = fagsakDto.behandlinger
        behandlinger.size shouldBe 1
        val behandlingsoppsummeringtDto = behandlinger.toList()[0]
        behandlingsoppsummeringtDto.behandlingId shouldBe behandling.id
        behandlingsoppsummeringtDto.eksternBrukId shouldBe behandling.eksternBrukId
        behandlingsoppsummeringtDto.status shouldBe behandling.status
        behandlingsoppsummeringtDto.type shouldBe behandling.type
    }

    @Test
    fun `hentFagsak skal hente fagsak for død person`() {
        val eksternFagsakId = UUID.randomUUID().toString()
        val behandling = opprettBehandling(Ytelsestype.BARNETRYGD, eksternFagsakId, "doed1234")

        val fagsakDto = fagsakService.hentFagsak(Fagsystem.BA, eksternFagsakId)

        fagsakDto.eksternFagsakId shouldBe eksternFagsakId
        fagsakDto.språkkode shouldBe Språkkode.NB
        fagsakDto.ytelsestype shouldBe Ytelsestype.BARNETRYGD
        fagsakDto.fagsystem shouldBe Fagsystem.BA

        val brukerDto = fagsakDto.bruker
        brukerDto.personIdent shouldBe "doed1234"
        brukerDto.navn shouldBe "testverdi"
        brukerDto.kjønn shouldBe Kjønn.MANN
        brukerDto.fødselsdato shouldBe LocalDate.now().minusYears(20)
        brukerDto.dødsdato shouldBe LocalDate.of(2022, 4, 1)

        val behandlinger = fagsakDto.behandlinger
        behandlinger.size shouldBe 1
        val behandlingsoppsummeringtDto = behandlinger.toList()[0]
        behandlingsoppsummeringtDto.behandlingId shouldBe behandling.id
        behandlingsoppsummeringtDto.eksternBrukId shouldBe behandling.eksternBrukId
        behandlingsoppsummeringtDto.status shouldBe behandling.status
        behandlingsoppsummeringtDto.type shouldBe behandling.type
    }

    @Test
    fun `hentFagsak skal hente og oppdatere fagsak for barnetrygd`() {
        val eksternFagsakId = UUID.randomUUID().toString()
        opprettBehandling(Ytelsestype.BARNETRYGD, eksternFagsakId, "12345678910")

        // Antar mock PDL client returnerer 32132132111
        // første kall mot PDL får differanse på ident og kaster endretPersonIdentPublisher event
        fagsakService.hentFagsak(Fagsystem.BA, eksternFagsakId)
        val fagsakDto = fagsakService.hentFagsak(Fagsystem.BA, eksternFagsakId)

        fagsakDto.eksternFagsakId shouldBe eksternFagsakId
        fagsakDto.språkkode shouldBe Språkkode.NB
        fagsakDto.ytelsestype shouldBe Ytelsestype.BARNETRYGD
        fagsakDto.fagsystem shouldBe Fagsystem.BA

        val brukerDto = fagsakDto.bruker
        brukerDto.personIdent shouldBe "12345678910"
        brukerDto.navn shouldBe "testverdi"
        brukerDto.kjønn shouldBe Kjønn.MANN
        brukerDto.fødselsdato shouldBe LocalDate.now().minusYears(20)
    }

    @Test
    fun `hentFagsak skal hente fagsak for barnetrygd med institusjon`() {
        val eksternFagsakId = UUID.randomUUID().toString()
        val behandling = opprettBehandling(
            ytelsestype = Ytelsestype.BARNETRYGD,
            eksternFagsakId = eksternFagsakId,
            institusjon = Institusjon(organisasjonsnummer = "998765432"),
        )

        val fagsakDto = fagsakService.hentFagsak(Fagsystem.BA, eksternFagsakId)

        fagsakDto.eksternFagsakId shouldBe eksternFagsakId
        fagsakDto.språkkode shouldBe Språkkode.NB
        fagsakDto.ytelsestype shouldBe Ytelsestype.BARNETRYGD
        fagsakDto.fagsystem shouldBe Fagsystem.BA
        fagsakDto.institusjon shouldNotBe null
        fagsakDto.institusjon!!.organisasjonsnummer shouldBe "998765432"
        fagsakDto.institusjon!!.navn shouldBe "Testinstitusjon"

        val brukerDto = fagsakDto.bruker
        brukerDto.personIdent shouldBe "32132132111"
        brukerDto.navn shouldBe "testverdi"
        brukerDto.kjønn shouldBe Kjønn.MANN
        brukerDto.fødselsdato shouldBe LocalDate.now().minusYears(20)
        brukerDto.dødsdato shouldBe null

        val behandlinger = fagsakDto.behandlinger
        behandlinger.size shouldBe 1
        val behandlingsoppsummeringtDto = behandlinger.toList()[0]
        behandlingsoppsummeringtDto.behandlingId shouldBe behandling.id
        behandlingsoppsummeringtDto.eksternBrukId shouldBe behandling.eksternBrukId
        behandlingsoppsummeringtDto.status shouldBe behandling.status
        behandlingsoppsummeringtDto.type shouldBe behandling.type
    }

    @Test
    fun `hentFagsak skal ikke hente fagsak for barnetrygd når det ikke finnes`() {
        val eksternFagsakId = UUID.randomUUID().toString()
        val exception = shouldThrow<RuntimeException> { fagsakService.hentFagsak(Fagsystem.BA, eksternFagsakId) }
        exception.message shouldBe "Fagsak finnes ikke for Barnetrygd og $eksternFagsakId"
    }

    @Test
    fun `finnesÅpenTilbakekrevingsbehandling skal returnere false om fagsak ikke finnes`() {
        val finnesBehandling = fagsakService.finnesÅpenTilbakekrevingsbehandling(Fagsystem.BA, UUID.randomUUID().toString())
        finnesBehandling.finnesÅpenBehandling.shouldBeFalse()
    }

    @Test
    fun `finnesÅpenTilbakekrevingsbehandling skal returnere false om behandling er avsluttet`() {
        val eksternFagsakId = UUID.randomUUID().toString()
        var behandling = opprettBehandling(Ytelsestype.BARNETRYGD, eksternFagsakId)
        behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandlingRepository.update(behandling.copy(status = Behandlingsstatus.AVSLUTTET))

        val finnesBehandling = fagsakService.finnesÅpenTilbakekrevingsbehandling(Fagsystem.BA, eksternFagsakId)
        finnesBehandling.finnesÅpenBehandling.shouldBeFalse()
    }

    @Test
    fun `finnesÅpenTilbakekrevingsbehandling skal returnere true om det finnes en åpen behandling`() {
        val eksternFagsakId = UUID.randomUUID().toString()
        opprettBehandling(Ytelsestype.BARNETRYGD, eksternFagsakId)

        val finnesBehandling = fagsakService.finnesÅpenTilbakekrevingsbehandling(Fagsystem.BA, eksternFagsakId)
        finnesBehandling.finnesÅpenBehandling.shouldBeTrue()
    }

    @Test
    fun `kanBehandlingOpprettesManuelt skal returnere false når det finnes en åpen tilbakekrevingsbehandling`() {
        val eksternFagsakId = UUID.randomUUID().toString()
        opprettBehandling(Ytelsestype.BARNETRYGD, eksternFagsakId)

        val respons = fagsakService.kanBehandlingOpprettesManuelt(eksternFagsakId, Ytelsestype.BARNETRYGD)
        respons.kanBehandlingOpprettes.shouldBeFalse()
        respons.kravgrunnlagsreferanse.shouldBeNull()
        respons.melding shouldBe "Det finnes allerede en åpen tilbakekrevingsbehandling. Den ligger i saksoversikten."
    }

    @Test
    fun `kanBehandlingOpprettesManuelt skal returnere false når det ikke finnes et frakoblet kravgrunnlag`() {
        val respons = fagsakService.kanBehandlingOpprettesManuelt(UUID.randomUUID().toString(), Ytelsestype.BARNETRYGD)
        respons.kanBehandlingOpprettes.shouldBeFalse()
        respons.kravgrunnlagsreferanse.shouldBeNull()
        respons.melding shouldBe "Det finnes ingen feilutbetaling på saken, så du kan ikke opprette tilbakekrevingsbehandling."
    }

    @Test
    fun `kanBehandlingOpprettesManuelt skal returnere false når det allerede finnes en opprettelse request`() {
        val mottattXml = Testdata.økonomiXmlMottatt
        økonomiXmlMottattRepository.insert(mottattXml)

        val properties = Properties()
        properties["eksternFagsakId"] = mottattXml.eksternFagsakId
        properties["ytelsestype"] = Ytelsestype.BARNETRYGD.kode
        properties["eksternId"] = mottattXml.referanse
        taskService.save(Task(type = OpprettBehandlingManueltTask.TYPE, properties = properties, payload = ""))

        val respons = fagsakService.kanBehandlingOpprettesManuelt(mottattXml.eksternFagsakId, Ytelsestype.BARNETRYGD)
        respons.kanBehandlingOpprettes.shouldBeFalse()
        respons.kravgrunnlagsreferanse.shouldBeNull()
        respons.melding shouldBe "Det finnes allerede en forespørsel om å opprette tilbakekrevingsbehandling. " +
            "Behandlingen vil snart bli tilgjengelig i saksoversikten. Dersom den ikke dukker opp, " +
            "ta kontakt med brukerstøtte for å rapportere feilen."
    }

    @Test
    fun `kanBehandlingOpprettesManuelt skal returnere true når det finnes et frakoblet grunnlag`() {
        val mottattXml = Testdata.økonomiXmlMottatt
        økonomiXmlMottattRepository.insert(mottattXml)

        val respons = fagsakService.kanBehandlingOpprettesManuelt(mottattXml.eksternFagsakId, Ytelsestype.BARNETRYGD)
        respons.kanBehandlingOpprettes.shouldBeTrue()
        respons.kravgrunnlagsreferanse shouldBe mottattXml.referanse
        respons.melding shouldBe "Det er mulig å opprette behandling manuelt."
    }

    @Nested
    inner class HentVedtakForFagsak {

        @Test
        internal fun `skal returnere tom liste hvis det ikke finnes noen vedtak for fagsak`() {
            assertThat(fagsakService.hentVedtakForFagsak(Fagsystem.EF, UUID.randomUUID().toString()))
                .isEmpty()
        }
    }

    private fun opprettBehandling(
        ytelsestype: Ytelsestype,
        eksternFagsakId: String,
        personIdent: String = "32132132111",
        institusjon: Institusjon? = null,
    ): Behandling {
        val fagsak = Fagsak(
            eksternFagsakId = eksternFagsakId,
            bruker = Bruker(personIdent, Språkkode.NB),
            ytelsestype = ytelsestype,
            fagsystem = FagsystemUtil.hentFagsystemFraYtelsestype(ytelsestype),
            institusjon = institusjon,
        )
        fagsakRepository.insert(fagsak)

        val behandling = Behandling(
            fagsakId = fagsak.id,
            type = Behandlingstype.TILBAKEKREVING,
            ansvarligSaksbehandler = Constants.BRUKER_ID_VEDTAKSLØSNINGEN,
            behandlendeEnhet = "8020",
            behandlendeEnhetsNavn = "Oslo",
            manueltOpprettet = false,
        )
        behandlingRepository.insert(behandling)
        return behandling
    }
}
