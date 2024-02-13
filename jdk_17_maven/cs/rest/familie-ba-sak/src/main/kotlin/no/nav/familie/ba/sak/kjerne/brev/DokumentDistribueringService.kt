package no.nav.familie.ba.sak.kjerne.brev

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.task.DistribuerDokumentDTO
import no.nav.familie.ba.sak.task.DistribuerDokumentPåJournalpostIdTask
import no.nav.familie.http.client.RessursException
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class DokumentDistribueringService(
    private val taskService: TaskService,
    private val integrasjonClient: IntegrasjonClient,
    private val loggService: LoggService,
) {

    fun prøvDistribuerBrevOgLoggHendelse(
        distribuerDokumentDTO: DistribuerDokumentDTO,
        loggBehandlerRolle: BehandlerRolle,
    ) = try {
        distribuerBrevOgLoggHendelse(distribuerDokumentDTO, loggBehandlerRolle)
    } catch (ressursException: RessursException) {
        val journalpostId = distribuerDokumentDTO.journalpostId
        val behandlingId = distribuerDokumentDTO.behandlingId

        logger.info(
            "Klarte ikke å distribuere brev til journalpost $journalpostId på behandling $behandlingId. " +
                "Httpstatus ${ressursException.httpStatus}",
        )
        secureLogger.info(
            "Klarte ikke å distribuere brev til journalpost $journalpostId på behandling $behandlingId.\n" +
                "Httpstatus: ${ressursException.httpStatus}\n" +
                "Melding: ${ressursException.cause?.message}",
        )

        if (dokumentetErAlleredeDistribuert(ressursException)) {
            logger.warn(alleredeDistribuertMelding(journalpostId, behandlingId))
        } else {
            throw ressursException
        }
    }

    fun prøvDistribuerBrevOgLoggHendelseFraBehandling(
        distribuerDokumentDTO: DistribuerDokumentDTO,
        loggBehandlerRolle: BehandlerRolle,
    ) {
        try {
            prøvDistribuerBrevOgLoggHendelse(distribuerDokumentDTO, loggBehandlerRolle)
        } catch (ressursException: RessursException) {
            val journalpostId = distribuerDokumentDTO.journalpostId
            val behandlingId = distribuerDokumentDTO.behandlingId
            val brevmal = distribuerDokumentDTO.brevmal

            when {
                mottakerErDødUtenDødsboadresse(ressursException) && behandlingId != null ->
                    opprettLogginnslagPåBehandlingOgNyTaskSomDistribuererPåJournalpostId(distribuerDokumentDTO)

                mottakerErIkkeDigitalOgHarUkjentAdresse(ressursException) && behandlingId != null ->
                    loggBrevIkkeDistribuertUkjentAdresse(journalpostId, behandlingId, brevmal)

                else -> throw ressursException
            }
        }
    }

    internal fun opprettLogginnslagPåBehandlingOgNyTaskSomDistribuererPåJournalpostId(distribuerDokumentDTO: DistribuerDokumentDTO) {
        val task = DistribuerDokumentPåJournalpostIdTask.opprettTask(distribuerDokumentDTO.copy(behandlingId = null))
        taskService.save(task)

        logger.info(
            "Klarte ikke å distribuere brev for journalpostId ${distribuerDokumentDTO.journalpostId} " +
                "på behandling ${distribuerDokumentDTO.behandlingId}. Bruker har ukjent dødsboadresse.",
        )

        loggService.opprettBrevIkkeDistribuertUkjentDødsboadresseLogg(
            behandlingId = checkNotNull(distribuerDokumentDTO.behandlingId),
            brevnavn = distribuerDokumentDTO.brevmal.visningsTekst,
        )
    }

    internal fun loggBrevIkkeDistribuertUkjentAdresse(
        journalpostId: String,
        behandlingId: Long,
        brevMal: Brevmal,
    ) {
        logger.info("Klarte ikke å distribuere brev for journalpostId $journalpostId på behandling $behandlingId. Bruker har ukjent adresse.")
        loggService.opprettBrevIkkeDistribuertUkjentAdresseLogg(
            behandlingId = behandlingId,
            brevnavn = brevMal.visningsTekst,
        )
        antallBrevIkkeDistribuertUkjentAndresse[brevMal]?.increment()
    }

    private fun distribuerBrevOgLoggHendelse(
        distribuerDokumentDTO: DistribuerDokumentDTO,
        loggBehandlerRolle: BehandlerRolle,
    ) {
        val brevmal = distribuerDokumentDTO.brevmal
        integrasjonClient.distribuerBrev(distribuerDokumentDTO)

        if (distribuerDokumentDTO.behandlingId != null) {
            loggService.opprettDistribuertBrevLogg(
                behandlingId = distribuerDokumentDTO.behandlingId,
                tekst = brevmal.visningsTekst,
                rolle = loggBehandlerRolle,
            )
        }

        antallBrevSendt[brevmal]?.increment()
    }

    private val antallBrevSendt: Map<Brevmal, Counter> = mutableListOf<Brevmal>().plus(Brevmal.values()).associateWith {
        Metrics.counter(
            "brev.sendt",
            "brevtype",
            it.visningsTekst,
        )
    }

    private val antallBrevIkkeDistribuertUkjentAndresse: Map<Brevmal, Counter> =
        mutableListOf<Brevmal>().plus(Brevmal.values()).associateWith {
            Metrics.counter(
                "brev.ikke.sendt.ukjent.andresse",
                "brevtype",
                it.visningsTekst,
            )
        }

    fun alleredeDistribuertMelding(journalpostId: String, behandlingId: Long?) =
        "Journalpost med Id=$journalpostId er allerede distiribuert. Hopper over distribuering." +
            if (behandlingId != null) " BehandlingId=$behandlingId." else ""

    companion object {
        val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }
}
