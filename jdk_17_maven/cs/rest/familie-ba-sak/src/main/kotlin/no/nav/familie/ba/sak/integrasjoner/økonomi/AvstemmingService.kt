package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.task.KonsistensavstemMotOppdragAvsluttTask
import no.nav.familie.ba.sak.task.KonsistensavstemMotOppdragFinnPerioderForRelevanteBehandlingerTask
import no.nav.familie.ba.sak.task.dto.KonsistensavstemmingAvsluttTaskDTO
import no.nav.familie.ba.sak.task.dto.KonsistensavstemmingFinnPerioderForRelevanteBehandlingerDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.PerioderForBehandling
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.Properties
import java.util.UUID

@Service
class AvstemmingService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val økonomiKlient: ØkonomiKlient,
    private val beregningService: BeregningService,
    private val taskService: TaskService,
    private val batchRepository: BatchRepository,
    private val dataChunkRepository: DataChunkRepository,
) {
    fun grensesnittavstemOppdrag(fraDato: LocalDateTime, tilDato: LocalDateTime) {
        økonomiKlient.grensesnittavstemOppdrag(fraDato, tilDato)
    }

    fun sendKonsistensavstemmingStart(avstemmingsdato: LocalDateTime, transaksjonsId: UUID) {
        økonomiKlient.konsistensavstemOppdragStart(
            avstemmingsdato,
            transaksjonsId,
        )
    }

    fun harBatchStatusFerdig(batchId: Long): Boolean {
        val batch = batchRepository.getReferenceById(batchId)
        return batch.status == KjøreStatus.FERDIG
    }

    fun erKonsistensavstemmingStartet(transaksjonsId: UUID): Boolean {
        return dataChunkRepository.findByTransaksjonsId(transaksjonsId).isNotEmpty()
    }

    fun skalOppretteFinnPerioderForRelevanteBehandlingerTask(transaksjonsId: UUID, chunkNr: Int): Boolean {
        logger.info("Sjekker om konsistensavstemming er gjort for=$transaksjonsId og chunkNr=$chunkNr")
        return dataChunkRepository.findByTransaksjonsIdAndChunkNr(transaksjonsId, chunkNr) == null
    }

    fun erKonsistensavstemmingKjørtForTransaksjonsidOgChunk(transaksjonsId: UUID, chunkNr: Int): Boolean {
        val dataChunk = dataChunkRepository.findByTransaksjonsIdAndChunkNr(transaksjonsId, chunkNr)
        return dataChunk?.erSendt == true
    }

    fun konsistensavstemOppdragData(
        avstemmingsdato: LocalDateTime,
        perioderTilAvstemming: List<PerioderForBehandling>,
        transaksjonsId: UUID,
        chunkNr: Int,
        sendTilØkonomi: Boolean,
    ) {
        logger.info("Utfører konsistensavstemOppdragData: Sender perioder for transaksjonsId $transaksjonsId og chunk nr $chunkNr")
        val dataChunk = dataChunkRepository.findByTransaksjonsIdAndChunkNr(transaksjonsId, chunkNr)
            ?: error("Finner ingen datachunk for $transaksjonsId og $chunkNr")

        if (dataChunk.erSendt) {
            logger.info("Utfører konsistensavstemOppdragData: Perioder for transaksjonsId $transaksjonsId og chunk nr $chunkNr er allerede sendt.")
            return
        }

        if (sendTilØkonomi) {
            økonomiKlient.konsistensavstemOppdragData(
                avstemmingsdato,
                perioderTilAvstemming,
                transaksjonsId,
            )
        } else {
            logger.info("Send datamelding til økonomi i dry-run modus for $transaksjonsId og $chunkNr")
        }

        dataChunkRepository.save(dataChunk.also { it.erSendt = true })
    }

    fun konsistensavstemOppdragAvslutt(avstemmingsdato: LocalDateTime, transaksjonsId: UUID) {
        logger.info("Avslutter konsistensavstemming for $transaksjonsId")

        økonomiKlient.konsistensavstemOppdragAvslutt(avstemmingsdato, transaksjonsId)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun opprettKonsistensavstemmingAvsluttTask(
        konsistensavstemmingAvsluttTaskDTO: KonsistensavstemmingAvsluttTaskDTO,
    ) {
        logger.info("Oppretter avsluttingstask for transaksjonsId=${konsistensavstemmingAvsluttTaskDTO.transaksjonsId}")
        val konsistensavstemmingAvsluttTask = Task(
            type = KonsistensavstemMotOppdragAvsluttTask.TASK_STEP_TYPE,
            payload = objectMapper.writeValueAsString(konsistensavstemmingAvsluttTaskDTO),
            properties = Properties().apply {
                this["transaksjonsId"] = konsistensavstemmingAvsluttTaskDTO.transaksjonsId.toString()
            },
        )
        taskService.save(konsistensavstemmingAvsluttTask)
    }

    fun hentSisteIverksatteBehandlingerFraLøpendeFagsaker() =
        behandlingHentOgPersisterService.hentSisteIverksatteBehandlingerFraLøpendeFagsaker()

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun opprettKonsistensavstemmingFinnPerioderForRelevanteBehandlingerTask(
        konsistensavstemmingFinnPerioderForRelevanteBehandlingerDTO: KonsistensavstemmingFinnPerioderForRelevanteBehandlingerDTO,
        triggerTid: LocalDateTime,
    ) {
        val batch =
            batchRepository.getReferenceById(konsistensavstemmingFinnPerioderForRelevanteBehandlingerDTO.batchId)
        dataChunkRepository.save(
            DataChunk(
                batch = batch,
                chunkNr = konsistensavstemmingFinnPerioderForRelevanteBehandlingerDTO.chunkNr,
                transaksjonsId = konsistensavstemmingFinnPerioderForRelevanteBehandlingerDTO.transaksjonsId,
            ),
        )

        logger.info("Oppretter task for å finne perioder for relevante behandlinger. transaksjonsId=${konsistensavstemmingFinnPerioderForRelevanteBehandlingerDTO.transaksjonsId} og chunk=${konsistensavstemmingFinnPerioderForRelevanteBehandlingerDTO.chunkNr} for ${konsistensavstemmingFinnPerioderForRelevanteBehandlingerDTO.relevanteBehandlinger.size} behandlinger")
        val task = Task(
            type = KonsistensavstemMotOppdragFinnPerioderForRelevanteBehandlingerTask.TASK_STEP_TYPE,
            payload = objectMapper.writeValueAsString(
                konsistensavstemmingFinnPerioderForRelevanteBehandlingerDTO,
            ),
            properties = Properties().apply {
                this["transaksjonsId"] =
                    konsistensavstemmingFinnPerioderForRelevanteBehandlingerDTO.transaksjonsId.toString()
                this["chunkNr"] = konsistensavstemmingFinnPerioderForRelevanteBehandlingerDTO.chunkNr.toString()
            },
        ).medTriggerTid(triggerTid)
        taskService.save(task)
    }

    fun hentDataForKonsistensavstemming(
        avstemmingstidspunkt: LocalDateTime,
        relevanteBehandlinger: List<Long>,
    ): List<PerioderForBehandling> {
        return relevanteBehandlinger
            .chunked(1000)
            .map { chunk ->
                val relevanteAndeler = beregningService.hentLøpendeAndelerTilkjentYtelseMedUtbetalingerForBehandlinger(
                    behandlingIder = chunk,
                    avstemmingstidspunkt = avstemmingstidspunkt,
                )
                val aktiveFødselsnummere =
                    behandlingHentOgPersisterService.hentAktivtFødselsnummerForBehandlinger(
                        relevanteAndeler.mapNotNull { it.kildeBehandlingId },
                    )

                val tssEksternIdForBehandlinger =
                    behandlingHentOgPersisterService.hentTssEksternIdForBehandlinger(
                        relevanteAndeler.mapNotNull { it.kildeBehandlingId },
                    )

                relevanteAndeler.groupBy { it.kildeBehandlingId }
                    .map { (kildeBehandlingId, andeler) ->
                        if (kildeBehandlingId == null) {
                            secureLogger.warn("Finner ikke behandlingsId for andeler=$andeler")
                        }
                        PerioderForBehandling(
                            behandlingId = kildeBehandlingId.toString(),
                            aktivFødselsnummer = aktiveFødselsnummere[kildeBehandlingId]
                                ?: error("Finnes ikke et aktivt fødselsnummer for behandling $kildeBehandlingId"),
                            perioder = andeler
                                .map {
                                    it.periodeOffset
                                        ?: error("Andel ${it.id} på iverksatt behandling på løpende fagsak mangler periodeOffset")
                                }
                                .toSet(),
                            utebetalesTil = tssEksternIdForBehandlinger[kildeBehandlingId],
                        )
                    }
            }.flatten()
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(AvstemmingService::class.java)

        const val KONSISTENSAVSTEMMING_DATA_CHUNK_STORLEK = 500
    }
}
