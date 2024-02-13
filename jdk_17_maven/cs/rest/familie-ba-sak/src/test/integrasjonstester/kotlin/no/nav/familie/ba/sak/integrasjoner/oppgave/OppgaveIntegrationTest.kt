package no.nav.familie.ba.sak.integrasjoner.oppgave

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.ClientMocks
import no.nav.familie.ba.sak.config.DatabaseCleanupService
import no.nav.familie.ba.sak.integrasjoner.oppgave.domene.OppgaveRepository
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class OppgaveIntegrationTest : AbstractSpringIntegrationTest() {

    @Autowired
    private lateinit var fagsakService: FagsakService

    @Autowired
    private lateinit var behandlingService: BehandlingService

    @Autowired
    private lateinit var oppgaveService: OppgaveService

    @Autowired
    private lateinit var oppgaveRepository: OppgaveRepository

    @Autowired
    private lateinit var personidentService: PersonidentService

    @Autowired
    private lateinit var personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository

    @Autowired
    private lateinit var databaseCleanupService: DatabaseCleanupService

    @BeforeEach
    fun setUp() {
        databaseCleanupService.truncate()
    }

    @Test
    fun `Skal opprette oppgave og ferdigstille oppgave for behandling`() {
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(SØKER_FNR)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val barnAktør = personidentService.hentOgLagreAktørIder(listOf(BARN_FNR), true)
        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(
            behandling.id,
            SØKER_FNR,
            listOf(BARN_FNR),
            søkerAktør = fagsak.aktør,
            barnAktør = barnAktør,
        )

        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        val godkjenneVedtakOppgaveId =
            oppgaveService.opprettOppgave(behandling.id, Oppgavetype.GodkjenneVedtak, LocalDate.now())

        val opprettetOppgave =
            oppgaveRepository.findByOppgavetypeAndBehandlingAndIkkeFerdigstilt(Oppgavetype.GodkjenneVedtak, behandling)

        Assertions.assertNotNull(opprettetOppgave)
        Assertions.assertEquals(Oppgavetype.GodkjenneVedtak, opprettetOppgave!!.type)
        Assertions.assertEquals(behandling.id, opprettetOppgave.behandling.id)
        Assertions.assertEquals(behandling.status, opprettetOppgave.behandling.status)
        Assertions.assertEquals(
            behandling.behandlingStegTilstand.first().behandlingSteg,
            opprettetOppgave.behandling.behandlingStegTilstand.first().behandlingSteg,
        )
        Assertions.assertEquals(
            behandling.behandlingStegTilstand.first().behandlingStegStatus,
            opprettetOppgave.behandling.behandlingStegTilstand.first().behandlingStegStatus,
        )
        Assertions.assertFalse(opprettetOppgave.erFerdigstilt)
        Assertions.assertEquals(godkjenneVedtakOppgaveId, opprettetOppgave.gsakId)

        oppgaveService.ferdigstillOppgaver(behandling.id, Oppgavetype.GodkjenneVedtak)

        Assertions.assertNull(
            oppgaveRepository.findByOppgavetypeAndBehandlingAndIkkeFerdigstilt(
                Oppgavetype.GodkjenneVedtak,
                behandling,
            ),
        )
    }

    @Test
    fun `Skal logge feil ved opprettelse av oppgave på type som ikke er ferdigstilt`() {
        val logger: Logger = LoggerFactory.getLogger(OppgaveService::class.java) as Logger

        val listAppender: ListAppender<ILoggingEvent> = initLoggingEventListAppender()
        logger.addAppender(listAppender)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(SØKER_FNR)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val barnAktør = personidentService.hentOgLagreAktørIder(listOf(BARN_FNR), true)
        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(
            behandling.id,
            SØKER_FNR,
            listOf(BARN_FNR),
            søkerAktør = fagsak.aktør,
            barnAktør = barnAktør,
        )

        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        oppgaveService.opprettOppgave(behandling.id, Oppgavetype.GodkjenneVedtak, LocalDate.now())
        oppgaveService.opprettOppgave(behandling.id, Oppgavetype.GodkjenneVedtak, LocalDate.now())

        val loggingEvents = listAppender.list

        assertThat(loggingEvents)
            .extracting<String, RuntimeException> { obj: ILoggingEvent -> obj.formattedMessage }
            .anyMatch { message -> message.contains("Fant eksisterende oppgave med samme oppgavetype") }
    }

    @Test
    fun `Skal fjerne behandlesAvApplikasjon på liste med oppgaver som finnes i ba-ak`() {
        databaseCleanupService.truncate()
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(SØKER_FNR)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val barnAktør = personidentService.hentOgLagreAktørIder(listOf(BARN_FNR), true)
        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(
            behandling.id,
            SØKER_FNR,
            listOf(BARN_FNR),
            søkerAktør = fagsak.aktør,
            barnAktør = barnAktør,
        )

        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        val oppgave1 =
            oppgaveService.opprettOppgave(behandling.id, Oppgavetype.GodkjenneVedtak, LocalDate.now()).toLong()

        val response = oppgaveService.fjernBehandlesAvApplikasjon(listOf(oppgave1, 123456L))
        assertThat(response.toList()).hasSize(1).containsOnly(oppgave1)
    }

    protected fun initLoggingEventListAppender(): ListAppender<ILoggingEvent> {
        val listAppender = ListAppender<ILoggingEvent>()
        listAppender.start()
        return listAppender
    }

    companion object {
        private val SØKER_FNR = randomFnr()
        private val BARN_FNR = ClientMocks.barnFnr[0]
    }
}
