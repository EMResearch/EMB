package no.nav.familie.tilbake.kravgrunnlag

import jakarta.jms.TextMessage
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.kravgrunnlag.task.BehandleKravgrunnlagTask
import no.nav.familie.tilbake.kravgrunnlag.task.BehandleStatusmeldingTask
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.jms.annotation.JmsListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Properties
import java.util.UUID

@Service
@Profile("!e2e & !integrasjonstest")
class KravgrunnlagMottaker(private val taskService: TaskService) {

    private val log = LoggerFactory.getLogger(this::class.java)
    private val secureLog = LoggerFactory.getLogger("secureLogger")

    @Transactional
    @JmsListener(destination = "\${oppdrag.mq.kravgrunnlag}", containerFactory = "jmsListenerContainerFactory")
    fun mottaMeldingFraOppdrag(melding: TextMessage) {
        val meldingFraOppdrag = melding.text as String

        log.info("Mottatt melding fra oppdrag")
        secureLog.info(meldingFraOppdrag)
        if (meldingFraOppdrag.contains(Constants.kravgrunnlagXmlRootElement)) {
            taskService.save(
                Task(
                    type = BehandleKravgrunnlagTask.TYPE,
                    payload = meldingFraOppdrag,
                    properties = Properties().apply {
                        this["callId"] = UUID.randomUUID()
                    },
                ),
            )
        } else {
            taskService.save(
                Task(
                    type = BehandleStatusmeldingTask.TYPE,
                    payload = meldingFraOppdrag,
                    properties = Properties().apply {
                        this["callId"] = UUID.randomUUID()
                    },
                ),
            )
        }
    }
}
