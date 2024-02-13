package no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdService
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.FagsystemRegelVurdering.SEND_TIL_BA
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.FagsystemRegelVurdering.SEND_TIL_INFOTRYGD
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.FagsystemUtfall.FAGSAK_UTEN_IVERKSATTE_BEHANDLINGER_I_BA_SAK
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.FagsystemUtfall.IVERKSATTE_BEHANDLINGER_I_BA_SAK
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.FagsystemUtfall.LØPENDE_SAK_I_INFOTRYGD
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.FagsystemUtfall.MOR_IKKE_GYLDIG_MEDLEMSKAP_FOR_AUTOMATISK_VURDERING
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.FagsystemUtfall.SAKER_I_INFOTRYGD_MEN_IKKE_LØPENDE_UTBETALINGER
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.FagsystemUtfall.STØTTET_I_BA_SAK
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.FagsystemUtfall.values
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Medlemskap.EØS
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Medlemskap.NORDEN
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Medlemskap.STATSLØS
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Medlemskap.TREDJELANDSBORGER
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Medlemskap.UKJENT
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.StatsborgerskapService
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class VelgFagSystemService(
    private val fagsakService: FagsakService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val infotrygdService: InfotrygdService,
    private val personidentService: PersonidentService,
    private val personopplysningerService: PersonopplysningerService,
    private val statsborgerskapService: StatsborgerskapService,
) {

    val utfallForValgAvFagsystem = mutableMapOf<FagsystemUtfall, Counter>()

    init {
        values().forEach {
            utfallForValgAvFagsystem[it] = Metrics.counter(
                "familie.ba.sak.velgfagsystem",
                "navn",
                it.name,
                "beskrivelse",
                it.beskrivelse,
            )
        }
    }

    internal fun morHarLøpendeEllerTidligereUtbetalinger(fagsak: Fagsak?): Boolean {
        return if (fagsak == null) {
            false
        } else if (behandlingHentOgPersisterService.hentBehandlinger(fagsakId = fagsak.id)
                .any { it.status == BehandlingStatus.UTREDES }
        ) {
            true
        } else {
            behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(fagsakId = fagsak.id) != null
        }
    }

    internal fun morHarSakerMenIkkeLøpendeIInfotrygd(morsIdent: String): Boolean {
        val stønader = infotrygdService.hentInfotrygdstønaderForSøker(morsIdent, historikk = false).bruker
        return stønader.isNotEmpty()
    }

    internal fun morEllerBarnHarLøpendeSakIInfotrygd(morsIdent: String, barnasIdenter: List<String>): Boolean {
        val morsIdenter = personidentService.hentIdenter(personIdent = morsIdent, historikk = true)
            .filter { it.gruppe == "FOLKEREGISTERIDENT" }
            .map { it.ident }
        val alleBarnasIdenter = barnasIdenter.flatMap {
            personidentService.hentIdenter(personIdent = it, historikk = true)
                .filter { identinfo -> identinfo.gruppe == "FOLKEREGISTERIDENT" }
                .map { identinfo -> identinfo.ident }
        }

        return infotrygdService.harLøpendeSakIInfotrygd(morsIdenter, alleBarnasIdenter)
    }

    internal fun harMorGyldigStatsborgerskapForAutomatiskVurdering(morsAktør: Aktør): Boolean {
        val gjeldendeStatsborgerskap = personopplysningerService.hentGjeldendeStatsborgerskap(morsAktør)
        val medlemskap = statsborgerskapService.hentSterkesteMedlemskap(statsborgerskap = gjeldendeStatsborgerskap)

        secureLogger.info(
            "Gjeldende statsborgerskap for ${morsAktør.aktivFødselsnummer()}=" +
                "(${gjeldendeStatsborgerskap.land}, bekreftelsesdato=${gjeldendeStatsborgerskap.bekreftelsesdato}, gyldigFom=${gjeldendeStatsborgerskap.gyldigFraOgMed}, gyldigTom=${gjeldendeStatsborgerskap.gyldigTilOgMed}), " +
                "medlemskap=$medlemskap",
        )

        return when (medlemskap) {
            NORDEN, TREDJELANDSBORGER, STATSLØS, EØS -> true
            UKJENT, null -> false
        }
    }

    fun velgFagsystem(nyBehandlingHendelse: NyBehandlingHendelse): Pair<FagsystemRegelVurdering, FagsystemUtfall> {
        val morsAktør = personidentService.hentAktør(nyBehandlingHendelse.morsIdent)

        val fagsak = fagsakService.hentNormalFagsak(morsAktør)

        val (fagsystemUtfall: FagsystemUtfall, fagsystem: FagsystemRegelVurdering) = when {
            morHarLøpendeEllerTidligereUtbetalinger(fagsak) -> Pair(
                IVERKSATTE_BEHANDLINGER_I_BA_SAK,
                SEND_TIL_BA,
            )
            morEllerBarnHarLøpendeSakIInfotrygd(
                nyBehandlingHendelse.morsIdent,
                nyBehandlingHendelse.barnasIdenter,
            ) -> Pair(
                LØPENDE_SAK_I_INFOTRYGD,
                SEND_TIL_INFOTRYGD,
            )
            fagsak != null -> Pair(
                FAGSAK_UTEN_IVERKSATTE_BEHANDLINGER_I_BA_SAK,
                SEND_TIL_BA,
            )
            morHarSakerMenIkkeLøpendeIInfotrygd(nyBehandlingHendelse.morsIdent) -> Pair(
                SAKER_I_INFOTRYGD_MEN_IKKE_LØPENDE_UTBETALINGER,
                SEND_TIL_INFOTRYGD,
            )
            !harMorGyldigStatsborgerskapForAutomatiskVurdering(
                morsAktør,
            ) -> Pair(
                MOR_IKKE_GYLDIG_MEDLEMSKAP_FOR_AUTOMATISK_VURDERING,
                SEND_TIL_INFOTRYGD,
            )
            else -> Pair(
                STØTTET_I_BA_SAK,
                SEND_TIL_BA,
            )
        }

        secureLogger.info("Sender fødselshendelse for ${nyBehandlingHendelse.morsIdent} til $fagsystem med utfall $fagsystemUtfall")
        utfallForValgAvFagsystem[fagsystemUtfall]?.increment()
        return Pair(fagsystem, fagsystemUtfall)
    }

    companion object {
        val logger = LoggerFactory.getLogger(VelgFagSystemService::class.java)
    }
}

enum class FagsystemRegelVurdering {
    SEND_TIL_BA,
    SEND_TIL_INFOTRYGD,
}

enum class FagsystemUtfall(val beskrivelse: String) {
    IVERKSATTE_BEHANDLINGER_I_BA_SAK("Mor har fagsak med tidligere eller løpende utbetalinger i ba-sak"),
    LØPENDE_SAK_I_INFOTRYGD("Mor har løpende sak i infotrygd"),
    FAGSAK_UTEN_IVERKSATTE_BEHANDLINGER_I_BA_SAK("Mor har fagsak uten iverksatte behandlinger"),
    SAKER_I_INFOTRYGD_MEN_IKKE_LØPENDE_UTBETALINGER("Mor har saker i infotrygd, men ikke løpende utbetalinger"),
    MOR_IKKE_GYLDIG_MEDLEMSKAP_FOR_AUTOMATISK_VURDERING("Mor har ikke gyldig medlemskap for automatisk vurdering"),
    STØTTET_I_BA_SAK("Person kan automatisk vurderes i ba-sak"),
}
