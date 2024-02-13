package no.nav.familie.tilbake.dokumentbestilling.felles.task

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.VarselService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Base64
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = LagreVarselbrevsporingTask.TYPE,
    maxAntallFeil = 3,
    beskrivelse = "Lagrer varselbrev",
    triggerTidVedFeilISekunder = 60 * 5L,
)
class LagreVarselbrevsporingTask(private val varselService: VarselService) : AsyncTaskStep {

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun doTask(task: Task) {
        log.info("${this::class.simpleName} prosesserer med id=${task.id} og metadata ${task.metadata}")

        val varseltekstBase64: String = task.metadata.getProperty("fritekst")
        val varseltekst = Base64.getDecoder().decode(varseltekstBase64).decodeToString()
        val varselbeløp: Long = task.metadata.getProperty("varselbeløp").toLong()
        val behandlingId = UUID.fromString(task.payload)
        varselService.lagre(behandlingId, varseltekst, varselbeløp)
    }

    companion object {

        const val TYPE = "lagreVarselbrevsporing"
    }
}
