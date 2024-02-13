package no.nav.familie.tilbake.behandling.batch

import no.nav.familie.leader.LeaderClient
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.PropertyName
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.data.domain.Pageable
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Properties

@Service
class AutomatiskSaksbehandlingBatch(
    private val automatiskSaksbehandlingService: AutomatiskSaksbehandlingService,
    private val fagsakRepository: FagsakRepository,
    private val taskService: TaskService,
    private val environment: Environment,
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Scheduled(cron = "\${CRON_AUTOMATISK_SAKSBEHANDLING}")
    @Transactional
    fun behandleAutomatisk() {
        if (LeaderClient.isLeader() != true && !environment.activeProfiles.any {
                it.contains("local") ||
                    it.contains("integrasjonstest")
            }
        ) {
            return
        }
        logger.info("Starter AutomatiskSaksbehandlingBatch..")

        logger.info("Henter alle behandlinger som kan behandle automatisk.")
        val behandlinger = automatiskSaksbehandlingService.hentAlleBehandlingerSomKanBehandleAutomatisk()
        logger.info("Det finnes ${behandlinger.size} behandlinger som kan behandles automatisk")

        if (behandlinger.isNotEmpty()) {
            val alleFeiledeTasker = taskService.finnTasksMedStatus(
                listOf(
                    Status.FEILET,
                    Status.PLUKKET,
                    Status.KLAR_TIL_PLUKK,
                ),
                Pageable.unpaged(),
            )
            behandlinger.forEach {
                val finnesTask = alleFeiledeTasker.any { task ->
                    task.type == AutomatiskSaksbehandlingTask.TYPE && task.payload == it.id.toString()
                }
                if (!finnesTask) {
                    val fagsystem = fagsakRepository.findByIdOrThrow(it.fagsakId).fagsystem
                    taskService.save(
                        Task(
                            type = AutomatiskSaksbehandlingTask.TYPE,
                            payload = it.id.toString(),
                            properties = Properties().apply {
                                setProperty(
                                    PropertyName.FAGSYSTEM,
                                    fagsystem.name,
                                )
                            },
                        ),
                    )
                } else {
                    logger.info("Det finnes allerede en feilet AutomatiskSaksbehandlingTask for samme behandlingId=${it.id}")
                }
            }
        }
        logger.info("Stopper AutomatiskSaksbehandlingBatch..")
    }
}
