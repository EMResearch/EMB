package no.nav.familie.ba.sak.kjerne.fagsak

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.nyOrdinærBehandling
import no.nav.familie.ba.sak.common.randomAktør
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.ClientMocks
import no.nav.familie.ba.sak.config.DatabaseCleanupService
import no.nav.familie.ba.sak.config.tilAktør
import no.nav.familie.ba.sak.ekstern.restDomene.FagsakDeltagerRolle
import no.nav.familie.ba.sak.ekstern.restDomene.InstitusjonInfo
import no.nav.familie.ba.sak.ekstern.restDomene.RestSøkParam
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.skyggesak.SkyggesakRepository
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.institusjon.InstitusjonService
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import no.nav.familie.ba.sak.kjerne.personident.Personident
import no.nav.familie.ba.sak.kjerne.personident.PersonidentRepository
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.util.BrukerContextUtil
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.log.mdc.MDCConstants
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Pageable

class FagsakControllerTest(
    @Autowired
    private val fagsakService: FagsakService,

    @Autowired
    private val fagsakController: FagsakController,

    @Autowired
    private val behandlingService: BehandlingService,

    @Autowired
    private val mockIntegrasjonClient: IntegrasjonClient,

    @Autowired
    private val persongrunnlagService: PersongrunnlagService,

    @Autowired
    private val mockPersonidentService: PersonidentService,

    @Autowired
    private val aktørIdRepository: AktørIdRepository,

    @Autowired
    private val personidentRepository: PersonidentRepository,

    @Autowired
    private val databaseCleanupService: DatabaseCleanupService,

    @Autowired
    private val skyggesakRepository: SkyggesakRepository,

    @Autowired
    private val institusjonService: InstitusjonService,
) : AbstractSpringIntegrationTest() {

    @BeforeEach
    fun init() {
        MDC.put(MDCConstants.MDC_CALL_ID, "00001111")
        BrukerContextUtil.mockBrukerContext(SikkerhetContext.SYSTEM_FORKORTELSE)
        databaseCleanupService.truncate()
    }

    @AfterEach
    fun tearDown() {
        BrukerContextUtil.clearBrukerContext()
    }

    @Test
    @Tag("integration")
    fun `Skal opprette fagsak av type NORMAL`() {
        val fnr = randomFnr()

        fagsakController.hentEllerOpprettFagsak(FagsakRequest(personIdent = fnr))
        val fagsak = fagsakService.hentNormalFagsak(tilAktør(fnr))
        assertEquals(fnr, fagsak?.aktør?.aktivFødselsnummer())
        assertEquals(FagsakType.NORMAL, fagsak?.type)
        assertNull(fagsak?.institusjon)
    }

    @Test
    @Tag("integration")
    fun `Skal opprette skyggesak i Sak`() {
        val fnr = randomFnr()

        val fagsak = fagsakController.hentEllerOpprettFagsak(FagsakRequest(personIdent = fnr)).body?.data

        val skyggesak = skyggesakRepository.finnSkyggesakerKlareForSending(Pageable.unpaged())
        assertEquals(1, skyggesak.filter { it.fagsakId == fagsak?.id }.size)
    }

    @Test
    @Tag("integration")
    fun `Skal returnere eksisterende fagsak på person som allerede finnes`() {
        val fnr = randomFnr()

        val nyRestFagsak = fagsakController.hentEllerOpprettFagsak(FagsakRequest(personIdent = fnr))
        assertEquals(Ressurs.Status.SUKSESS, nyRestFagsak.body?.status)
        assertEquals(fnr, fagsakService.hentNormalFagsak(tilAktør(fnr))?.aktør?.aktivFødselsnummer())

        val eksisterendeRestFagsak = fagsakController.hentEllerOpprettFagsak(
            FagsakRequest(
                personIdent = fnr,
            ),
        )
        assertEquals(Ressurs.Status.SUKSESS, eksisterendeRestFagsak.body?.status)
        assertEquals(eksisterendeRestFagsak.body!!.data!!.id, nyRestFagsak.body!!.data!!.id)
    }

    @Test
    @Tag("integration")
    fun `Skal returnere eksisterende fagsak på person som allerede finnes med gammel ident`() {
        val fnr = randomFnr()
        val nyttFnr = randomFnr()
        val aktørId = randomAktør().aktørId

        // Får ikke mockPersonopplysningerService til å virke riktig derfor oppdateres db direkte.
        val aktør = aktørIdRepository.save(Aktør(aktørId))
        personidentRepository.save(Personident(fødselsnummer = fnr, aktør = aktør, aktiv = true))

        val nyRestFagsak = fagsakController.hentEllerOpprettFagsak(FagsakRequest(personIdent = fnr))
        assertEquals(Ressurs.Status.SUKSESS, nyRestFagsak.body?.status)
        assertEquals(fnr, fagsakService.hentNormalFagsak(aktør)?.aktør?.aktivFødselsnummer())

        personidentRepository.save(
            personidentRepository.getReferenceById(fnr).also { it.aktiv = false },
        )
        personidentRepository.save(Personident(fødselsnummer = nyttFnr, aktør = aktør, aktiv = true))

        val eksisterendeRestFagsak = fagsakController.hentEllerOpprettFagsak(
            FagsakRequest(
                personIdent = nyttFnr,
            ),
        )
        assertEquals(Ressurs.Status.SUKSESS, eksisterendeRestFagsak.body?.status)
        assertEquals(eksisterendeRestFagsak.body!!.data!!.id, nyRestFagsak.body!!.data!!.id)
        assertEquals(nyttFnr, eksisterendeRestFagsak.body!!.data?.søkerFødselsnummer)
    }

    @Test
    @Tag("integration")
    fun `Skal returnere eksisterende fagsak på person som allerede finnes basert på aktørid`() {
        val aktørId = randomAktør()
        val fagsakRequest = FagsakRequest(personIdent = aktørId.aktivFødselsnummer())
        val nyRestFagsak = fagsakController.hentEllerOpprettFagsak(
            fagsakRequest,
        )
        assertEquals(Ressurs.Status.SUKSESS, nyRestFagsak.body?.status)

        val eksisterendeRestFagsak = fagsakController.hentEllerOpprettFagsak(fagsakRequest)
        assertEquals(Ressurs.Status.SUKSESS, eksisterendeRestFagsak.body?.status)
        assertEquals(eksisterendeRestFagsak.body!!.data!!.id, nyRestFagsak.body!!.data!!.id)
    }

    @Test
    fun `Skal oppgi person med fagsak som fagsakdeltaker`() {
        val personAktør = mockPersonidentService.hentAktør(randomFnr())

        fagsakService.hentEllerOpprettFagsak(personAktør.aktivFødselsnummer())
            .also { fagsakService.oppdaterStatus(it, FagsakStatus.LØPENDE) }

        fagsakController.oppgiFagsakdeltagere(RestSøkParam(personAktør.aktivFødselsnummer(), emptyList())).apply {
            assertEquals(personAktør.aktivFødselsnummer(), body!!.data!!.first().ident)
            assertEquals(FagsakDeltagerRolle.FORELDER, body!!.data!!.first().rolle)
        }
    }

    @Test
    fun `Skal oppgi det første barnet i listen som fagsakdeltaker`() {
        val personAktør = mockPersonidentService.hentOgLagreAktør(randomFnr(), true)
        val søkerAktør = mockPersonidentService.hentOgLagreAktør(ClientMocks.søkerFnr[0], true)
        val barnaAktør = mockPersonidentService.hentOgLagreAktørIder(ClientMocks.barnFnr.toList().subList(0, 1), true)

        val fagsak = fagsakService.hentEllerOpprettFagsak(søkerAktør.aktivFødselsnummer())

        val behandling =
            behandlingService.opprettBehandling(
                nyOrdinærBehandling(
                    søkersIdent = ClientMocks.søkerFnr[0],
                    fagsakId = fagsak.id,
                ),
            )
        persongrunnlagService.hentOgLagreSøkerOgBarnINyttGrunnlag(
            personAktør,
            barnaAktør,
            behandling,
            Målform.NB,
        )

        fagsakController.oppgiFagsakdeltagere(
            RestSøkParam(
                personAktør.aktivFødselsnummer(),
                ClientMocks.barnFnr.toList(),
            ),
        )
            .apply {
                assertEquals(ClientMocks.barnFnr.toList().subList(0, 1), body!!.data!!.map { it.ident })
                assertEquals(listOf(FagsakDeltagerRolle.BARN), body!!.data!!.map { it.rolle })
            }
    }

    @Test
    @Tag("integration")
    fun `Skal få valideringsfeil ved oppretting av fagsak av type INSTITUSJON uten FagsakInstitusjon satt`() {
        val fnr = randomFnr()

        val exception = assertThrows<FunksjonellFeil> {
            fagsakController.hentEllerOpprettFagsak(
                FagsakRequest(
                    personIdent = fnr,
                    fagsakType = FagsakType.INSTITUSJON,
                ),
            )
        }
        val fagsaker = fagsakService.hentMinimalFagsakerForPerson(tilAktør(fnr))
        assert(fagsaker.status == Ressurs.Status.FEILET)
        assertEquals("Mangler påkrevd variabel orgnummer for institusjon", exception.message)
    }

    @Test
    @Tag("integration")
    fun `Skal opprette fagsak av type INSTITUSJON hvor FagsakInstitusjon er satt`() {
        val fnr = randomFnr()

        fagsakController.hentEllerOpprettFagsak(
            FagsakRequest(
                personIdent = fnr,
                fagsakType = FagsakType.INSTITUSJON,
                institusjon = InstitusjonInfo("orgnr", "tss-id"),
            ),
        )
        val fagsakerRessurs = fagsakService.hentMinimalFagsakerForPerson(tilAktør(fnr))
        assert(fagsakerRessurs.status == Ressurs.Status.SUKSESS)
        val fagsaker = fagsakerRessurs.data!!
        assert(fagsaker.isNotEmpty()) { "Fagsak skulle ha blitt opprettet" }
        assertEquals(fnr, fagsaker[0].søkerFødselsnummer)
        assertEquals(FagsakType.INSTITUSJON, fagsaker[0].fagsakType)
        assertNotNull(fagsaker[0].institusjon)
        assertEquals("orgnr", fagsaker[0].institusjon?.orgNummer)
        assertEquals("tss-id", fagsaker[0].institusjon?.tssEksternId)
    }
}
