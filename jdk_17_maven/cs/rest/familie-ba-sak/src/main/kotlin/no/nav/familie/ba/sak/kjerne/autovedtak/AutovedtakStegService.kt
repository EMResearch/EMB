package no.nav.familie.ba.sak.kjerne.autovedtak

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.AutovedtakFødselshendelseService
import no.nav.familie.ba.sak.kjerne.autovedtak.omregning.AutovedtakBrevService
import no.nav.familie.ba.sak.kjerne.autovedtak.småbarnstillegg.AutovedtakSmåbarnstilleggService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.task.dto.ManuellOppgaveType
import no.nav.familie.prosessering.error.RekjørSenereException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

interface AutovedtakBehandlingService<Behandlingsdata : AutomatiskBehandlingData> {
    fun skalAutovedtakBehandles(behandlingsdata: Behandlingsdata): Boolean

    fun kjørBehandling(behandlingsdata: Behandlingsdata): String
}

enum class Autovedtaktype(val displayName: String) {
    FØDSELSHENDELSE("Fødselshendelse"),
    SMÅBARNSTILLEGG("Småbarnstillegg"),
    OMREGNING_BREV("Omregning"),
}

sealed interface AutomatiskBehandlingData {
    val type: Autovedtaktype
}

data class FødselshendelseData(
    val nyBehandlingHendelse: NyBehandlingHendelse,
) : AutomatiskBehandlingData {
    override val type = Autovedtaktype.FØDSELSHENDELSE
}

data class SmåbarnstilleggData(
    val aktør: Aktør,
) : AutomatiskBehandlingData {
    override val type = Autovedtaktype.SMÅBARNSTILLEGG
}

data class OmregningBrevData(
    val aktør: Aktør,
    val behandlingsårsak: BehandlingÅrsak,
    val standardbegrunnelse: Standardbegrunnelse,
    val fagsakId: Long,
) : AutomatiskBehandlingData {
    override val type = Autovedtaktype.OMREGNING_BREV
}

@Service
class AutovedtakStegService(
    private val fagsakService: FagsakService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val oppgaveService: OppgaveService,
    private val autovedtakFødselshendelseService: AutovedtakFødselshendelseService,
    private val autovedtakBrevService: AutovedtakBrevService,
    private val autovedtakSmåbarnstilleggService: AutovedtakSmåbarnstilleggService,
) {

    private val antallAutovedtak: Map<Autovedtaktype, Counter> = Autovedtaktype.values().associateWith {
        Metrics.counter("behandling.saksbehandling.autovedtak", "type", it.name)
    }
    private val antallAutovedtakÅpenBehandling: Map<Autovedtaktype, Counter> = Autovedtaktype.values().associateWith {
        Metrics.counter("behandling.saksbehandling.autovedtak.aapen_behandling", "type", it.name)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun kjørBehandlingFødselshendelse(mottakersAktør: Aktør, nyBehandlingHendelse: NyBehandlingHendelse): String {
        return kjørBehandling(
            mottakersAktør = mottakersAktør,
            automatiskBehandlingData = FødselshendelseData(nyBehandlingHendelse),
        )
    }

    fun kjørBehandlingOmregning(mottakersAktør: Aktør, behandlingsdata: OmregningBrevData): String {
        return kjørBehandling(
            mottakersAktør = mottakersAktør,
            automatiskBehandlingData = behandlingsdata,
        )
    }

    fun kjørBehandlingSmåbarnstillegg(mottakersAktør: Aktør, aktør: Aktør): String {
        return kjørBehandling(
            mottakersAktør = mottakersAktør,
            automatiskBehandlingData = SmåbarnstilleggData(aktør),
        )
    }

    private fun kjørBehandling(
        automatiskBehandlingData: AutomatiskBehandlingData,
        mottakersAktør: Aktør,
    ): String {
        secureLoggAutovedtakBehandling(automatiskBehandlingData.type, mottakersAktør, BEHANDLING_STARTER)
        antallAutovedtak[automatiskBehandlingData.type]?.increment()

        val skalAutovedtakBehandles = when (automatiskBehandlingData) {
            is FødselshendelseData -> autovedtakFødselshendelseService.skalAutovedtakBehandles(automatiskBehandlingData)
            is OmregningBrevData -> autovedtakBrevService.skalAutovedtakBehandles(automatiskBehandlingData)
            is SmåbarnstilleggData -> autovedtakSmåbarnstilleggService.skalAutovedtakBehandles(automatiskBehandlingData)
        }

        if (!skalAutovedtakBehandles) {
            secureLoggAutovedtakBehandling(
                automatiskBehandlingData.type,
                mottakersAktør,
                "Skal ikke behandles",
            )
            return "${automatiskBehandlingData.type.displayName}: Skal ikke behandles"
        }

        if (håndterÅpenBehandlingOgAvbrytAutovedtak(
                aktør = mottakersAktør,
                autovedtaktype = automatiskBehandlingData.type,
                fagsakId = hentFagsakIdFraBehandlingsdata(automatiskBehandlingData),
            )
        ) {
            secureLoggAutovedtakBehandling(
                automatiskBehandlingData.type,
                mottakersAktør,
                "Bruker har åpen behandling",
            )
            return "${automatiskBehandlingData.type.displayName}: Bruker har åpen behandling"
        }

        val resultatAvKjøring = when (automatiskBehandlingData) {
            is FødselshendelseData -> autovedtakFødselshendelseService.kjørBehandling(automatiskBehandlingData)
            is OmregningBrevData -> autovedtakBrevService.kjørBehandling(automatiskBehandlingData)
            is SmåbarnstilleggData -> autovedtakSmåbarnstilleggService.kjørBehandling(automatiskBehandlingData)
        }

        secureLoggAutovedtakBehandling(
            automatiskBehandlingData.type,
            mottakersAktør,
            resultatAvKjøring,
        )

        return resultatAvKjøring
    }

    private fun hentFagsakIdFraBehandlingsdata(
        behandlingsdata: AutomatiskBehandlingData,
    ): Long? = when (behandlingsdata) {
        is OmregningBrevData -> behandlingsdata.fagsakId
        is FødselshendelseData,
        is SmåbarnstilleggData,
        -> null
    }

    private fun håndterÅpenBehandlingOgAvbrytAutovedtak(
        aktør: Aktør,
        autovedtaktype: Autovedtaktype,
        fagsakId: Long?,
    ): Boolean {
        val fagsak = if (fagsakId != null) {
            fagsakService.hentPåFagsakId(fagsakId)
        } else {
            fagsakService.hentNormalFagsak(aktør = aktør)
        }
        val åpenBehandling = fagsak?.let {
            behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(it.id)
        }

        return if (åpenBehandling == null) {
            false
        } else if (åpenBehandling.status == BehandlingStatus.UTREDES || åpenBehandling.status == BehandlingStatus.FATTER_VEDTAK || åpenBehandling.status == BehandlingStatus.SATT_PÅ_VENT) {
            antallAutovedtakÅpenBehandling[autovedtaktype]?.increment()
            oppgaveService.opprettOppgaveForManuellBehandling(
                behandling = åpenBehandling,
                begrunnelse = "${autovedtaktype.displayName}: Bruker har åpen behandling",
                manuellOppgaveType = ManuellOppgaveType.ÅPEN_BEHANDLING,
            )
            true
        } else if (åpenBehandling.status == BehandlingStatus.IVERKSETTER_VEDTAK || åpenBehandling.status == BehandlingStatus.SATT_PÅ_MASKINELL_VENT) {
            throw RekjørSenereException(
                årsak = "Åpen behandling med status ${åpenBehandling.status}, prøver igjen om 1 time",
                triggerTid = LocalDateTime.now().plusHours(1),
            )
        } else {
            throw Feil("Ikke håndtert feilsituasjon på $åpenBehandling")
        }
    }

    private fun secureLoggAutovedtakBehandling(
        autovedtaktype: Autovedtaktype,
        aktør: Aktør,
        melding: String,
    ) {
        secureLogger.info("$autovedtaktype(${aktør.aktivFødselsnummer()}): $melding")
    }

    companion object {
        const val BEHANDLING_STARTER = "Behandling starter"
        const val BEHANDLING_FERDIG = "Behandling ferdig"
    }
}
