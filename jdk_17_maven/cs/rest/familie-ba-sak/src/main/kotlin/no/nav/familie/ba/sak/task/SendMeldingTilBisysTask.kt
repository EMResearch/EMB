package no.nav.familie.ba.sak.task

import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.isSameOrBefore
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.statistikk.producer.KafkaProducer
import no.nav.familie.eksterne.kontrakter.bisys.BarnEndretOpplysning
import no.nav.familie.eksterne.kontrakter.bisys.BarnetrygdBisysMelding
import no.nav.familie.eksterne.kontrakter.bisys.BarnetrygdEndretType
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.YearMonth
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = SendMeldingTilBisysTask.TASK_STEP_TYPE,
    beskrivelse = "Send melding til Bisys om opphør eller reduksjon",
    maxAntallFeil = 3,
)
class SendMeldingTilBisysTask(
    private val kafkaProducer: KafkaProducer,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
) : AsyncTaskStep {

    private val logger = LoggerFactory.getLogger(SendMeldingTilBisysTask::class.java)
    private val meldingsTeller = Metrics.counter("familie.ba.sak.bisys.meldinger.sendt")

    override fun doTask(task: Task) {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId = task.payload.toLong())

        // Bisys vil kun ha rene manuelle opphør eller reduksjon
        if (behandling.resultat == Behandlingsresultat.OPPHØRT ||
            behandling.resultat == Behandlingsresultat.ENDRET_UTBETALING ||
            behandling.resultat == Behandlingsresultat.ENDRET_OG_OPPHØRT
        ) {
            val barnEndretOpplysning = finnBarnEndretOpplysning(behandling)
            val barnetrygdBisysMelding = BarnetrygdBisysMelding(
                søker = behandling.fagsak.aktør.aktivFødselsnummer(),
                barn = barnEndretOpplysning.filter { it.value.isNotEmpty() }.map { it.value.first() },
            )

            if (barnetrygdBisysMelding.barn.isEmpty()) {
                logger.info("Behandling endret men ikke reduksjon eller opphør. Send ikke melding til bisys")
                return
            }

            logger.info("Sender melding til bisys om opphør eller reduksjon av barnetrygd.")

            kafkaProducer.sendBarnetrygdBisysMelding(
                behandling.id.toString(),
                barnetrygdBisysMelding,
            )
            meldingsTeller.increment()
        } else {
            logger.info("Sender ikke melding til bisys siden resultat ikke er opphør eller reduksjon.")
        }
    }

    fun finnBarnEndretOpplysning(behandling: Behandling): Map<String, List<BarnEndretOpplysning>> {
        val forrigeIverksatteBehandling = behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling = behandling) ?: error("Finnes ikke forrige behandling for behandling ${behandling.id}")

        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandling(behandling.id)
        val forrigeTilkjentYtelse = tilkjentYtelseRepository.findByBehandling(forrigeIverksatteBehandling.id)

        val endretOpplysning: MutableMap<String, MutableList<BarnEndretOpplysning>> = mutableMapOf()

        forrigeTilkjentYtelse.andelerTilkjentYtelse.groupBy { it.aktør.aktørId }.entries.forEach { entry ->
            val nyAndelerTilkjentYtelse =
                tilkjentYtelse.andelerTilkjentYtelse.filter { it.aktør.aktørId == entry.key }
                    .sortedBy { it.stønadFom }
            entry.value.sortedBy { it.periode.fom }.forEach {
                var forblePeriode: MånedPeriode? = it.periode
                val prosent = it.prosent
                val barnIdent = it.aktør.aktivFødselsnummer()
                if (!endretOpplysning.contains(barnIdent)) {
                    endretOpplysning[barnIdent] = mutableListOf()
                }
                run checkEndretPerioder@{
                    nyAndelerTilkjentYtelse.forEach {
                        val intersectPerioder = forblePeriode!!.intersect(it.periode)
                        if (intersectPerioder.first != null) {
                            endretOpplysning[it.aktør.aktivFødselsnummer()]!!.add(
                                BarnEndretOpplysning(
                                    ident = barnIdent,
                                    fom = forblePeriode!!.fom,
                                    årsakskode = BarnetrygdEndretType.RO,
                                ),
                            )
                        }
                        if (intersectPerioder.second != null && it.prosent < prosent) {
                            endretOpplysning[it.aktør.aktivFødselsnummer()]!!.add(
                                BarnEndretOpplysning(
                                    ident = barnIdent,
                                    fom = latest(it.periode.fom, forblePeriode!!.fom),
                                    årsakskode = BarnetrygdEndretType.RR,
                                ),
                            )
                        }
                        forblePeriode = intersectPerioder.third
                        if (forblePeriode == null) {
                            return@checkEndretPerioder
                        }
                    }
                }
                if (forblePeriode != null && !forblePeriode!!.erTom()) {
                    endretOpplysning[it.aktør.aktivFødselsnummer()]!!.add(
                        BarnEndretOpplysning(
                            ident = barnIdent,
                            fom = forblePeriode!!.fom,
                            årsakskode = BarnetrygdEndretType.RO,
                        ),
                    )
                }
            }
        }
        return endretOpplysning
    }

    companion object {
        const val TASK_STEP_TYPE = "sendMeldingOmOpphørTilBisys"

        fun opprettTask(behandlingsId: Long): Task {
            return Task(
                type = TASK_STEP_TYPE,
                payload = behandlingsId.toString(),
                properties = Properties().apply {
                    this["behandlingsId"] = behandlingsId.toString()
                },
            )
        }
    }
}

fun earlist(yearMonth1: YearMonth, yearMonth2: YearMonth): YearMonth {
    return if (yearMonth1.isSameOrBefore(yearMonth2)) yearMonth1 else yearMonth2
}

fun latest(yearMonth1: YearMonth, yearMonth2: YearMonth): YearMonth {
    return if (yearMonth1.isSameOrAfter(yearMonth2)) yearMonth1 else yearMonth2
}

fun MånedPeriode.intersect(periode: MånedPeriode): Triple<MånedPeriode?, MånedPeriode?, MånedPeriode?> {
    val overlappetFom = latest(this.fom, periode.fom)
    val overlappetTom = earlist(this.tom, periode.tom)
    return Triple(
        if (this.fom.isSameOrAfter(periode.fom)) null else MånedPeriode(this.fom, periode.fom.minusMonths(1)),
        if (overlappetTom.isBefore(overlappetFom)) null else MånedPeriode(overlappetFom, overlappetTom),
        if (this.tom.isSameOrBefore(periode.tom)) null else MånedPeriode(periode.tom.plusMonths(1), this.tom),
    )
}

fun MånedPeriode.erTom() = fom.isAfter(tom)
