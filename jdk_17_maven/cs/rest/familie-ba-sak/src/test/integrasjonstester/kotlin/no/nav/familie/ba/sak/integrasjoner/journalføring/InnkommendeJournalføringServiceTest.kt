package no.nav.familie.ba.sak.integrasjoner.journalføring

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.ekstern.restDomene.InstitusjonInfo
import no.nav.familie.ba.sak.ekstern.restDomene.NavnOgIdent
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.DbJournalpostType
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.JournalføringRepository
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.Sakstype
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingSøknadsinfoRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingSøknadsinfoService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.verdikjedetester.lagMockRestJournalføring
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class InnkommendeJournalføringServiceTest(

    @Autowired
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,

    @Autowired
    private val behandlingSøknadsinfoService: BehandlingSøknadsinfoService,

    @Autowired
    private val fagsakService: FagsakService,

    @Autowired
    private val personidentService: PersonidentService,

    @Autowired
    private val innkommendeJournalføringService: InnkommendeJournalføringService,

    @Autowired
    private val journalføringRepository: JournalføringRepository,

    @Autowired
    private val behandlingSøknadsinfoRepository: BehandlingSøknadsinfoRepository,

) : AbstractSpringIntegrationTest() {

    @Test
    fun `lagrer journalpostreferanse til behandling og fagsak til journalpost`() {
        val søkerFnr = randomFnr()
        val søkerAktør = personidentService.hentAktør(søkerFnr)

        val fagsak = fagsakService.hentEllerOpprettFagsak(søkerAktør.aktivFødselsnummer())
        val behandling = behandlingHentOgPersisterService.lagreEllerOppdater(lagBehandling(fagsak))

        val (sak, behandlinger) = innkommendeJournalføringService
            .lagreJournalpostOgKnyttFagsakTilJournalpost(listOf(behandling.id.toString()), "12345")

        val journalposter = journalføringRepository.findByBehandlingId(behandlingId = behandling.id)

        assertEquals(1, journalposter.size)
        assertEquals(DbJournalpostType.I, journalposter.first().type)
        assertEquals(fagsak.id.toString(), sak.fagsakId)
        assertEquals(1, behandlinger.size)
    }

    @Test
    fun `ferdigstill skal oppdatere journalpost med GENERELL_SAKSTYPE hvis knyttTilFagsak er false`() {
        val søkerFnr = randomFnr()
        val søkerAktør = personidentService.hentAktør(søkerFnr)

        val fagsak = fagsakService.hentEllerOpprettFagsak(søkerAktør.aktivFødselsnummer())
        behandlingHentOgPersisterService.lagreEllerOppdater(lagBehandling(fagsak))

        val (sak, behandlinger) = innkommendeJournalføringService
            .lagreJournalpostOgKnyttFagsakTilJournalpost(listOf(), "12345")

        assertNull(sak.fagsakId)
        assertEquals(Sakstype.GENERELL_SAK.type, sak.sakstype)
        assertEquals(0, behandlinger.size)
    }

    @Test
    fun `journalfør skal opprette en førstegangsbehandling fra journalføring og lagre ned søknadsinfo`() {
        val søkerFnr = randomFnr()
        val request = lagMockRestJournalføring(bruker = NavnOgIdent("Mock", søkerFnr))
        val fagsakId = innkommendeJournalføringService.journalfør(request, "123", "mockEnhet", "1")

        val behandling = behandlingHentOgPersisterService.finnAktivForFagsak(fagsakId.toLong())
        assertNotNull(behandling)
        assertEquals(request.nyBehandlingstype, behandling!!.type)
        assertEquals(request.nyBehandlingsårsak, behandling.opprettetÅrsak)

        val søknadMottattDato = behandlingSøknadsinfoService.hentSøknadMottattDato(behandling.id)
        assertNotNull(søknadMottattDato)
        assertEquals(request.datoMottatt!!.toLocalDate(), søknadMottattDato!!.toLocalDate())

        val søknadsinfo = behandlingSøknadsinfoRepository.findByBehandlingId(behandling.id).single()
        assertEquals(true, søknadsinfo.erDigital)
    }

    @Test
    fun `journalfør skal lagre ned søknadsinfo tilknyttet en tidligere behandling`() {
        val søkerFnr = randomFnr()
        val førsteSøknad = lagMockRestJournalføring(bruker = NavnOgIdent("Mock", søkerFnr))
        val fagsakId = innkommendeJournalføringService.journalfør(førsteSøknad, "123", "mockEnhet", "1")

        val behandling = behandlingHentOgPersisterService.finnAktivForFagsak(fagsakId.toLong())

        val nySøknad2DagerSenere = førsteSøknad.copy(
            datoMottatt = førsteSøknad.datoMottatt!!.plusDays(2),
            opprettOgKnyttTilNyBehandling = false,
            tilknyttedeBehandlingIder = listOf(behandling!!.id.toString()),
        )

        innkommendeJournalføringService.journalfør(nySøknad2DagerSenere, "124", "mockEnhet", "2")

        val søknadsinfo = behandlingSøknadsinfoRepository.findByBehandlingId(behandling.id)
        assertEquals(2, søknadsinfo.size)

        val søknadMottattDato = behandlingSøknadsinfoService.hentSøknadMottattDato(behandling.id)
        assertEquals(førsteSøknad.datoMottatt!!.toLocalDate(), søknadMottattDato!!.toLocalDate())
    }

    @Test
    fun `journalfør skal opprette behandling på fagsak som har BARN som eier hvis enslig mindreårig eller institusjon`() {
        val request = lagMockRestJournalføring(bruker = NavnOgIdent("Mock", randomFnr()))
            .copy(fagsakType = FagsakType.BARN_ENSLIG_MINDREÅRIG)
        val fagsakId = innkommendeJournalføringService.journalfør(request, "123", "mockEnhet", "1")
        val behandling = behandlingHentOgPersisterService.finnAktivForFagsak(fagsakId.toLong())

        assertNotNull(behandling)
        assertEquals(FagsakType.BARN_ENSLIG_MINDREÅRIG, behandling!!.fagsak.type)

        val request2 = lagMockRestJournalføring(bruker = NavnOgIdent("Mock", randomFnr()))
            .copy(fagsakType = FagsakType.INSTITUSJON, institusjon = InstitusjonInfo("orgnr", tssEksternId = "tss"))
        val fagsakId2 = innkommendeJournalføringService.journalfør(request2, "1234", "mockEnhet", "2")
        val behandling2 = behandlingHentOgPersisterService.finnAktivForFagsak(fagsakId2.toLong())

        assertNotNull(behandling2)
        assertEquals(FagsakType.INSTITUSJON, behandling2!!.fagsak.type)
    }

    @Test
    fun `journalfør skal ikke opprette en førstegangsbehandling fra journalføring med manglende mottatt dato`() {
        val søkerFnr = randomFnr()
        val request = lagMockRestJournalføring(bruker = NavnOgIdent("Mock", søkerFnr)).copy(datoMottatt = null)

        val exception = assertThrows<RuntimeException> {
            innkommendeJournalføringService.journalfør(
                request,
                "123",
                "mockEnhet",
                "1",
            )
        }
        assertEquals("Du må sette søknads mottatt dato før du kan fortsette videre", exception.message)
    }
}
