package no.nav.familie.ba.sak.integrasjoner.økonomi.internkonsistensavstemming

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiKlient
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.task.InternKonsistensavstemmingTask
import no.nav.familie.ba.sak.task.OpprettTaskService.Companion.overstyrTaskMedNyCallId
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.log.IdUtils
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class InternKonsistensavstemmingService(
    val økonomiKlient: ØkonomiKlient,
    val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    val fagsakRepository: FagsakRepository,
    val taskService: TaskService,
) {
    fun validerLikUtbetalingIAndeleneOgUtbetalingsoppdragetPåAlleFagsaker(maksAntallTasker: Int = Int.MAX_VALUE) {
        val fagsakerSomIkkeErArkivert = fagsakRepository
            .hentFagsakerSomIkkeErArkivert()
            .map { it.id }
            .sortedBy { it }

        val startTid = LocalDateTime.now()

        fagsakerSomIkkeErArkivert
            // Antall fagsaker per task burde være en multippel av 3000 siden vi chunker databasekallet i 3000 i familie-oppdrag
            .chunked(3000)
            .take(maksAntallTasker)
            .forEachIndexed { index, fagsaker ->
                // Venter 15 sekunder mellom hver task for å ikke overkjøre familie-oppdrag siden ba-sak har mer ressurser
                val startTidForTask = startTid.plusSeconds(15 * index.toLong())
                overstyrTaskMedNyCallId(IdUtils.generateId()) {
                    val task = InternKonsistensavstemmingTask.opprettTask(fagsaker.toSet(), startTidForTask)
                    taskService.save(task)
                }
            }
    }

    fun validerLikUtbetalingIAndeleneOgUtbetalingsoppdraget(fagsaker: Set<Long>) {
        val fagsakTilSisteUtbetalingsoppdragOgSisteAndelerMap =
            hentFagsakTilSisteUtbetalingsoppdragOgSisteAndelerMap(fagsaker)

        val fagsakerMedFeil = fagsakTilSisteUtbetalingsoppdragOgSisteAndelerMap.mapNotNull { entry ->
            val fagsakId = entry.key
            val andeler = entry.value.first
            val utbetalingsoppdrag = entry.value.second

            fagsakId.takeIf { erForskjellMellomAndelerOgOppdrag(andeler, utbetalingsoppdrag, fagsakId) }
        }

        if (fagsakerMedFeil.isNotEmpty()) {
            throw Feil(
                "Tilkjent ytelse og utbetalingsoppdraget som er lagret i familie-oppdrag er inkonsistent" +
                    "\nSe secure logs for mer detaljer." +
                    "\nDette gjelder fagsakene $fagsakerMedFeil",
            )
        }
    }

    private fun hentFagsakTilSisteUtbetalingsoppdragOgSisteAndelerMap(fagsakIder: Set<Long>): Map<Long, Pair<List<AndelTilkjentYtelse>, Utbetalingsoppdrag?>> {
        val scope = CoroutineScope(SupervisorJob())
        val utbetalingsoppdragDeferred = scope.async {
            økonomiKlient.hentSisteUtbetalingsoppdragForFagsaker(fagsakIder)
        }

        val fagsakTilAndelerISisteBehandlingSendTilØkonomiMap =
            hentFagsakTilAndelerISisteBehandlingSendtTilØkonomiMap(fagsakIder)

        val fagsakTilSisteUtbetalingsoppdragMap = runBlocking { utbetalingsoppdragDeferred.await() }
            .associate { it.fagsakId to it.utbetalingsoppdrag }

        val fagsakTilSisteUtbetalingsoppdragOgSisteAndeler =
            fagsakTilAndelerISisteBehandlingSendTilØkonomiMap.mapValues { (fagsakId, andel) ->
                Pair(andel, fagsakTilSisteUtbetalingsoppdragMap[fagsakId])
            }
        return fagsakTilSisteUtbetalingsoppdragOgSisteAndeler
    }

    private fun hentFagsakTilAndelerISisteBehandlingSendtTilØkonomiMap(fagsaker: Set<Long>): Map<Long, List<AndelTilkjentYtelse>> {
        val behandlinger = behandlingHentOgPersisterService.hentSisteBehandlingSomErSendtTilØkonomiPerFagsak(fagsaker)

        return andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlinger(behandlinger.map { it.id })
            .groupBy { it.behandlingId }
            .mapKeys { (behandlingId, _) -> behandlinger.find { it.id == behandlingId }?.fagsak?.id!! }
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)!!
    }
}
