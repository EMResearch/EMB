package no.nav.familie.ba.sak.kjerne.autovedtak.omregning

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakStegService
import no.nav.familie.ba.sak.kjerne.autovedtak.OmregningBrevData
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.StartSatsendring
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.task.dto.Autobrev6og18ÅrDTO
import no.nav.familie.prosessering.error.RekjørSenereException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.YearMonth

@Service
class Autobrev6og18ÅrService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    private val autovedtakBrevService: AutovedtakBrevService,
    private val autovedtakStegService: AutovedtakStegService,
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService,
    private val startSatsendring: StartSatsendring,
) {

    @Transactional
    fun opprettOmregningsoppgaveForBarnIBrytingsalder(autobrev6og18ÅrDTO: Autobrev6og18ÅrDTO) {
        logger.info("opprettOmregningsoppgaveForBarnIBrytingsalder for fagsak ${autobrev6og18ÅrDTO.fagsakId}")

        val behandling =
            behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(autobrev6og18ÅrDTO.fagsakId)
                ?: error("Fant ikke aktiv behandling")

        val behandlingsårsak = finnBehandlingÅrsakForAlder(
            autobrev6og18ÅrDTO.alder,
        )

        if (!autovedtakBrevService.skalAutobrevBehandlingOpprettes(
                fagsakId = autobrev6og18ÅrDTO.fagsakId,
                behandlingsårsak = behandlingsårsak,
                standardbegrunnelser = AutobrevUtils.hentStandardbegrunnelserReduksjonForAlder(autobrev6og18ÅrDTO.alder),
            )
        ) {
            return
        }

        if (behandling.fagsak.status != FagsakStatus.LØPENDE) {
            logger.info("Fagsak ${behandling.fagsak.id} har ikke status løpende, og derfor prosesseres den ikke videre.")
            return
        }

        if (!barnMedAngittAlderInneværendeMånedEksisterer(
                behandlingId = behandling.id,
                alder = autobrev6og18ÅrDTO.alder,
            )
        ) {
            logger.warn("Fagsak ${behandling.fagsak.id} har ikke noe barn med alder ${autobrev6og18ÅrDTO.alder} ")
            return
        }

        if (barnetrygdOpphører(autobrev6og18ÅrDTO, behandling)) {
            logger.info("Fagsak ${behandling.fagsak.id} har ikke løpende utbetalinger for barn under 18 år og vil opphøre.")
            return
        }

        if (!barnIBrytningsalderHarLøpendeYtelse(
                alder = autobrev6og18ÅrDTO.alder,
                behandlingId = behandling.id,
                årMåned = autobrev6og18ÅrDTO.årMåned,

            )
        ) {
            logger.info("Ingen løpende ytelse for barnet i brytningsalder for fagsak=${behandling.fagsak.id} behandlingsårsak=$behandlingsårsak")
            return
        }

        if (startSatsendring.sjekkOgOpprettSatsendringVedGammelSats(autobrev6og18ÅrDTO.fagsakId)) {
            throw RekjørSenereException(
                "Satsedring skal kjøre ferdig før man behandler autobrev 6 og 18 år",
                LocalDateTime.now().plusHours(1),
            )
        }

        autovedtakStegService.kjørBehandlingOmregning(
            mottakersAktør = behandling.fagsak.aktør,
            behandlingsdata = OmregningBrevData(
                aktør = behandling.fagsak.aktør,
                behandlingsårsak = behandlingsårsak,
                standardbegrunnelse = AutobrevUtils.hentGjeldendeVedtakbegrunnelseReduksjonForAlder(
                    autobrev6og18ÅrDTO.alder,
                ),
                fagsakId = behandling.fagsak.id,
            ),
        )
    }

    private fun barnetrygdOpphører(
        autobrev6og18ÅrDTO: Autobrev6og18ÅrDTO,
        behandling: Behandling,
    ) =
        autobrev6og18ÅrDTO.alder == Alder.ATTEN.år &&
            !barnUnder18årInneværendeMånedEksisterer(behandlingId = behandling.id)

    private fun finnBehandlingÅrsakForAlder(alder: Int): BehandlingÅrsak =
        when (alder) {
            Alder.SEKS.år -> BehandlingÅrsak.OMREGNING_6ÅR
            Alder.ATTEN.år -> BehandlingÅrsak.OMREGNING_18ÅR
            else -> throw Feil("Alder må være oppgitt til enten 6 eller 18 år.")
        }

    private fun barnMedAngittAlderInneværendeMånedEksisterer(behandlingId: Long, alder: Int): Boolean =
        barnMedAngittAlderInneværendeMåned(behandlingId, alder).isNotEmpty()

    private fun barnMedAngittAlderInneværendeMåned(behandlingId: Long, alder: Int): List<Person> =
        personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = behandlingId)?.barna
            ?.filter { it.type == PersonType.BARN && it.fyllerAntallÅrInneværendeMåned(alder) }?.toList() ?: listOf()

    private fun barnUnder18årInneværendeMånedEksisterer(behandlingId: Long): Boolean =
        personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = behandlingId)?.barna
            ?.any { it.type == PersonType.BARN && it.erYngreEnnInneværendeMåned(Alder.ATTEN.år) } ?: false

    private fun barnIBrytningsalderHarLøpendeYtelse(
        alder: Int,
        behandlingId: Long,
        årMåned: YearMonth,
    ): Boolean {
        val barnIBrytningsalder =
            barnMedAngittAlderInneværendeMåned(behandlingId = behandlingId, alder = alder).map { it.aktør }

        if (barnIBrytningsalder.isEmpty()) {
            throw Feil("Forventer å finne minst et barn i brytningsalder for omregning 6 eller 18 år for behandling=$behandlingId")
        }

        val andelerTilBarnIBrytningsalder =
            andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(
                behandlingId,
            ).filter { it.aktør in barnIBrytningsalder }

        return harBarnIBrytningsalderLøpendeAndeler(alder, andelerTilBarnIBrytningsalder, årMåned)
    }

    private fun harBarnIBrytningsalderLøpendeAndeler(
        alder: Int,
        andelerTilBarnIBrytningsalder: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
        årMåned: YearMonth,
    ) = when (alder) {
        Alder.ATTEN.år -> andelerTilBarnIBrytningsalder.any { it.stønadTom.plusMonths(1) == årMåned }
        Alder.SEKS.år -> andelerTilBarnIBrytningsalder.any { it.stønadTom.plusMonths(1) == årMåned } && andelerTilBarnIBrytningsalder.any { it.stønadFom == årMåned }
        else -> throw Feil("Ugyldig alder")
    }

    companion object {

        private val logger = LoggerFactory.getLogger(Autobrev6og18ÅrService::class.java)
    }
}

enum class Alder(val år: Int) {
    SEKS(år = 6),
    ATTEN(år = 18),
}
