package no.nav.familie.ba.sak.kjerne.behandling

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.integrasjoner.sanity.SanityService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityEØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakRepository
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.EØSStandardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.domene.EØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Vedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeHentOgPersisterService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Component
class BehandlingMetrikker(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val vedtakRepository: VedtakRepository,
    private val vedtaksperiodeHentOgPersisterService: VedtaksperiodeHentOgPersisterService,
    private val sanityService: SanityService,
) {
    private var sanityBegrunnelser: Map<Standardbegrunnelse, SanityBegrunnelse> = emptyMap()
    private var antallGangerBruktStandardbegrunnelse: Map<Standardbegrunnelse, Counter> = emptyMap()

    private var sanityEØSBegrunnelser: Map<EØSStandardbegrunnelse, SanityEØSBegrunnelse> = emptyMap()
    private var antallGangerBruktEØSBegrunnelse: Map<EØSStandardbegrunnelse, Counter> = emptyMap()

    private val antallManuelleBehandlinger: Counter =
        Metrics.counter("behandling.behandlinger", "saksbehandling", "manuell")
    private val antallAutomatiskeBehandlinger: Counter =
        Metrics.counter("behandling.behandlinger", "saksbehandling", "automatisk")

    private val antallManuelleBehandlingerOpprettet: Map<BehandlingType, Counter> =
        initBehandlingTypeMetrikker("manuell")
    private val antallAutomatiskeBehandlingerOpprettet: Map<BehandlingType, Counter> =
        initBehandlingTypeMetrikker("automatisk")
    private val behandlingÅrsak: Map<BehandlingÅrsak, Counter> = initBehandlingÅrsakMetrikker()

    private val antallBehandlingsresultat: Map<Behandlingsresultat, Counter> =
        Behandlingsresultat.values().associateWith {
            Metrics.counter(
                "behandling.resultat",
                "type",
                it.name,
                "beskrivelse",
                it.displayName,
            )
        }

    private val behandlingstid: DistributionSummary = Metrics.summary("behandling.tid")

    fun hentBegrunnelserOgByggMetrikker() {
        try {
            sanityEØSBegrunnelser = sanityService.hentSanityEØSBegrunnelser()
        } catch (exception: Exception) {
            logger.warn("Kunne ikke hente EØS-begrunnelser fra sanity-api", exception)
        }

        antallGangerBruktEØSBegrunnelse = EØSStandardbegrunnelse.values().associateWith {
            val tittel = sanityEØSBegrunnelser[it]?.navnISystem ?: it.name

            Metrics.counter(
                "eøs-begrunnelse",
                "type",
                it.name,
                "beskrivelse",
                tittel,
            )
        }

        try {
            sanityBegrunnelser = sanityService.hentSanityBegrunnelser()
        } catch (exception: Exception) {
            logger.warn("Klarte ikke å bygge tellere for begrunnelser")
        }

        antallGangerBruktStandardbegrunnelse = Standardbegrunnelse.values().associateWith {
            val tittel = sanityBegrunnelser[it]?.navnISystem ?: it.name

            Metrics.counter(
                "brevbegrunnelse",
                "type",
                it.name,
                "beskrivelse",
                tittel,
            )
        }
    }

    fun tellNøkkelTallVedOpprettelseAvBehandling(behandling: Behandling) {
        if (behandling.skalBehandlesAutomatisk) {
            antallAutomatiskeBehandlingerOpprettet[behandling.type]?.increment()
            antallAutomatiskeBehandlinger.increment()
        } else {
            antallManuelleBehandlingerOpprettet[behandling.type]?.increment()
            antallManuelleBehandlinger.increment()
        }

        behandlingÅrsak[behandling.opprettetÅrsak]?.increment()
    }

    fun oppdaterBehandlingMetrikker(behandling: Behandling) {
        tellBehandlingstidMetrikk(behandling)
        økBehandlingsresultatTypeMetrikk(behandling)
        økBegrunnelseMetrikk(behandling)
    }

    private fun tellBehandlingstidMetrikk(behandling: Behandling) {
        val dagerSidenOpprettet = ChronoUnit.DAYS.between(behandling.opprettetTidspunkt, LocalDateTime.now())
        behandlingstid.record(dagerSidenOpprettet.toDouble())
    }

    private fun økBehandlingsresultatTypeMetrikk(behandling: Behandling) {
        val behandlingsresultat = behandlingHentOgPersisterService.hent(behandlingId = behandling.id).resultat
        antallBehandlingsresultat[behandlingsresultat]?.increment()
    }

    private fun økBegrunnelseMetrikk(behandling: Behandling) {
        if (antallGangerBruktStandardbegrunnelse.isEmpty()) hentBegrunnelserOgByggMetrikker()

        if (!behandlingHentOgPersisterService.hent(behandlingId = behandling.id).erHenlagt()) {
            val vedtak = vedtakRepository.findByBehandlingAndAktivOptional(behandlingId = behandling.id)
                ?: error("Finner ikke aktivt vedtak på behandling ${behandling.id}")

            val vedtaksperiodeMedBegrunnelser =
                vedtaksperiodeHentOgPersisterService.finnVedtaksperioderFor(vedtakId = vedtak.id)

            vedtaksperiodeMedBegrunnelser.forEach {
                it.begrunnelser.forEach { vedtaksbegrunnelse: Vedtaksbegrunnelse ->
                    antallGangerBruktStandardbegrunnelse[vedtaksbegrunnelse.standardbegrunnelse]?.increment()
                }

                it.eøsBegrunnelser.forEach { eøsBegrunnelse: EØSBegrunnelse ->
                    antallGangerBruktEØSBegrunnelse[eøsBegrunnelse.begrunnelse]?.increment()
                }
            }
        }
    }

    private fun initBehandlingTypeMetrikker(type: String): Map<BehandlingType, Counter> {
        return BehandlingType.values().associateWith {
            Metrics.counter(
                "behandling.opprettet",
                "type",
                it.name,
                "beskrivelse",
                it.visningsnavn,
                "saksbehandling",
                type,
            )
        }
    }

    private fun initBehandlingÅrsakMetrikker(): Map<BehandlingÅrsak, Counter> {
        return BehandlingÅrsak.values().associateWith {
            Metrics.counter(
                "behandling.aarsak",
                "aarsak",
                it.name,
                "beskrivelse",
                it.visningsnavn,
            )
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(BehandlingMetrikker::class.java)
    }
}
