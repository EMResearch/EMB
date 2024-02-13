package no.nav.familie.tilbake.micrometer

import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.MultiGauge
import io.micrometer.core.instrument.Tags
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.leader.LeaderClient
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultat
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultatstype
import no.nav.familie.tilbake.common.fagsystem
import no.nav.familie.tilbake.micrometer.domain.MeldingstellingRepository
import no.nav.familie.tilbake.micrometer.domain.Meldingstype
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class MålerService(
    private val meldingstellingRepository: MeldingstellingRepository,
    private val taskService: TaskService,
) {

    private val åpneBehandlingerGauge = MultiGauge.builder("UavsluttedeBehandlinger").register(Metrics.globalRegistry)
    private val klarTilBehandlingGauge = MultiGauge.builder("KlarTilBehandling").register(Metrics.globalRegistry)
    private val ventendeBehandlingGauge = MultiGauge.builder("VentendeBehandlinger").register(Metrics.globalRegistry)
    private val sendteBrevGauge = MultiGauge.builder("SendteBrev").register(Metrics.globalRegistry)
    private val vedtakGauge = MultiGauge.builder("Vedtak").register(Metrics.globalRegistry)
    private val mottatteKravgrunnlagGauge = MultiGauge.builder("MottatteKravgrunnlag").register(Metrics.globalRegistry)
    private val mottatteStatusmeldingerGauge = MultiGauge.builder("mottatteStatusmeldinger").register(Metrics.globalRegistry)
    private val feiledeTasker = MultiGauge.builder("FeiledeTasker").register(Metrics.globalRegistry)

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Scheduled(initialDelay = 60000L, fixedDelay = OPPDATERINGSFREKVENS)
    fun åpneBehandlinger() {
        if (LeaderClient.isLeader() != true) return
        val behandlinger = meldingstellingRepository.finnÅpneBehandlinger()
        Fagsystem.values().map { fagsystem ->
            val forekomster = behandlinger.filter { it.fagsystem == fagsystem }
            if (forekomster.isNotEmpty()) {
                logger.info(
                    "Åpne behandlinger for ${fagsystem.name} returnerte ${forekomster.sumOf { it.antall }} " +
                        "fordelt på ${forekomster.size} uker.",
                )
            }
        }
        val rows = behandlinger.map {
            MultiGauge.Row.of(
                Tags.of(
                    "fagsystem",
                    it.fagsystem.name,
                    "uke",
                    it.år.toString() + "-" + it.uke.toString().padStart(2, '0'),
                ),
                it.antall,
            )
        }

        åpneBehandlingerGauge.register(rows, true)
    }

    @Scheduled(initialDelay = 90000L, fixedDelay = OPPDATERINGSFREKVENS)
    fun behandlingerKlarTilSaksbehandling() {
        if (LeaderClient.isLeader() != true) return
        val behandlinger = meldingstellingRepository.finnKlarTilBehandling()
        Fagsystem.values().map { fagsystem ->
            val forekomster = behandlinger.filter { it.fagsystem == fagsystem }
            if (forekomster.isNotEmpty()) {
                logger.info(
                    "Behandlinger klar til saksbehandling for ${fagsystem.name} returnerte ${forekomster.sumOf { it.antall }} " +
                        "fordelt på ${forekomster.size} steg.",
                )
            }
        }
        val rows = behandlinger.map {
            MultiGauge.Row.of(
                Tags.of(
                    "fagsystem",
                    it.fagsystem.name,
                    "steg",
                    it.behandlingssteg.name,
                ),
                it.antall,
            )
        }

        klarTilBehandlingGauge.register(rows, true)
    }

    @Scheduled(initialDelay = 120000L, fixedDelay = OPPDATERINGSFREKVENS)
    fun behandlingerPåVent() {
        if (LeaderClient.isLeader() != true) return
        val behandlinger = meldingstellingRepository.finnVentendeBehandlinger()
        Fagsystem.values().map { fagsystem ->
            val forekomster = behandlinger.filter { it.fagsystem == fagsystem }
            if (forekomster.isNotEmpty()) {
                logger.info(
                    "Behandlinger på vent for ${fagsystem.name} returnerte ${forekomster.sumOf { it.antall }} " +
                        "fordelt på ${forekomster.size} steg.",
                )
            }
        }

        val rows = behandlinger.map {
            MultiGauge.Row.of(
                Tags.of(
                    "fagsystem",
                    it.fagsystem.name,
                    "steg",
                    it.behandlingssteg.name,
                ),
                it.antall,
            )
        }

        ventendeBehandlingGauge.register(rows, true)
    }

    @Scheduled(initialDelay = 150000L, fixedDelay = OPPDATERINGSFREKVENS)
    fun sendteBrev() {
        if (LeaderClient.isLeader() != true) return
        val data = meldingstellingRepository.finnSendteBrev()
        Fagsystem.values().map { fagsystem ->
            val forekomster = data.filter { it.fagsystem == fagsystem }
            if (forekomster.isNotEmpty()) {
                logger.info("Sendte brev for ${fagsystem.name} returnerte ${forekomster.sumOf { it.antall }} fordelt på ${forekomster.size} typer/uker.")
            }
        }

        val rows = data.map {
            MultiGauge.Row.of(
                Tags.of(
                    "fagsystem",
                    it.fagsystem.name,
                    "brevtype",
                    it.brevtype.name,
                    "uke",
                    it.år.toString() + "-" + it.uke.toString().padStart(2, '0'),
                ),
                it.antall,
            )
        }
        sendteBrevGauge.register(rows, true)
    }

    @Scheduled(initialDelay = 180000L, fixedDelay = OPPDATERINGSFREKVENS)
    fun vedtak() {
        if (LeaderClient.isLeader() != true) return
        val data = meldingstellingRepository.finnVedtak()
        Fagsystem.values().map { fagsystem ->
            val forekomster = data.filter { it.fagsystem == fagsystem }
            if (forekomster.isNotEmpty()) {
                logger.info(
                    "Vedtak for ${fagsystem.name} returnerte ${forekomster.sumOf { it.antall }} " +
                        "fordelt på ${forekomster.size} typer/uker.",
                )
            }
        }

        val rows = data.map {
            val vedtakstype = if (it.vedtakstype in Behandlingsresultat.ALLE_HENLEGGELSESKODER) {
                Behandlingsresultatstype.HENLAGT.name
            } else {
                it.vedtakstype.name
            }

            MultiGauge.Row.of(
                Tags.of(
                    "fagsystem",
                    it.fagsystem.name,
                    "vedtakstype",
                    vedtakstype,
                    "uke",
                    it.år.toString() + "-" + it.uke.toString().padStart(2, '0'),
                ),
                it.antall,
            )
        }
        vedtakGauge.register(rows, true)
    }

    @Scheduled(initialDelay = 210000L, fixedDelay = OPPDATERINGSFREKVENS)
    fun mottatteKravgrunnlagKoblet() {
        val data = meldingstellingRepository.findByType(Meldingstype.KRAVGRUNNLAG)
        Fagsystem.values().map { fagsystem ->
            val forekomster = data.filter { it.fagsystem == fagsystem }
            if (forekomster.isNotEmpty()) {
                logger.info(
                    "Mottatte kravgrunnlag koblet for ${fagsystem.name} returnerte ${forekomster.sumOf { it.antall }} " +
                        "fordelt på ${forekomster.size} fagsystem/dager.",
                )
            }
        }

        val rows = data.map {
            MultiGauge.Row.of(
                Tags.of(
                    "fagsystem",
                    it.fagsystem.name,
                    "type",
                    it.type.name,
                    "status",
                    it.status.name,
                    "dato",
                    it.dato.toString(),
                ),
                it.antall,
            )
        }
        mottatteKravgrunnlagGauge.register(rows, true)
    }

    @Scheduled(initialDelay = 240000L, fixedDelay = OPPDATERINGSFREKVENS)
    fun mottatteStatusmeldinger() {
        val data = meldingstellingRepository.summerAntallForType(Meldingstype.STATUSMELDING)
        Fagsystem.values().map { fagsystem ->
            val forekomster = data.filter { it.fagsystem == fagsystem }
            if (forekomster.isNotEmpty()) {
                logger.info(
                    "Mottatte statusmeldinger for ${fagsystem.name} " +
                        "returnerte ${forekomster.sumOf { it.antall }} fordelt på ${forekomster.size} fagsystem/dager.",
                )
            }
        }

        val rows = data.map {
            MultiGauge.Row.of(
                Tags.of(
                    "fagsystem",
                    it.fagsystem.name,
                    "dato",
                    it.dato.toString(),
                ),
                it.antall,
            )
        }
        mottatteStatusmeldingerGauge.register(rows, true)
    }

    @Scheduled(initialDelay = 270000L, fixedDelay = OPPDATERINGSFREKVENS)
    fun feiledeTasker() {
        val data = taskService.finnAlleFeiledeTasks()
        val fagsystemTilTasker = data.groupBy { it.fagsystem() }

        val rows = Fagsystem.values().map {
            MultiGauge.Row.of(
                Tags.of("fagsystem", it.name),
                (fagsystemTilTasker[it.name]?.size ?: 0) + (fagsystemTilTasker["UKJENT"]?.size ?: 0),
            )
        }

        feiledeTasker.register(rows, true)
    }

    companion object {

        const val OPPDATERINGSFREKVENS = 1800 * 1000L
    }
}
