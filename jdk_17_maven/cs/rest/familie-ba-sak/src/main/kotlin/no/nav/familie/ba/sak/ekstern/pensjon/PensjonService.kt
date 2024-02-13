package no.nav.familie.ba.sak.ekstern.pensjon

import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.ekstern.bisys.BisysService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.task.HentAlleIdenterTilPsysTask
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class PensjonService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val fagsakRepository: FagsakRepository,
    private val personidentService: PersonidentService,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val taskRepository: TaskRepositoryWrapper,
) {
    fun hentBarnetrygd(personIdent: String, fraDato: LocalDate): List<BarnetrygdTilPensjon> {
        val aktør = personidentService.hentAktør(personIdent)
        val fagsak = fagsakRepository.finnFagsakForAktør(aktør) ?: return emptyList()
        val barnetrygdTilPensjon = hentBarnetrygdTilPensjon(fagsak, fraDato) ?: return emptyList()

        // Sjekk om det finnes relaterte saker, dvs om barna finnes i andre behandlinger
        val barnetrygdMedRelaterteSaker = barnetrygdTilPensjon.barnetrygdPerioder
            .filter { it.personIdent != aktør.aktivFødselsnummer() }
            .map { it.personIdent }.distinct()
            .map { hentBarnetrygdForRelatertPersonTilPensjon(it, fraDato, aktør) }
            .flatten()
        return barnetrygdMedRelaterteSaker.plus(barnetrygdTilPensjon).distinct()
    }

    fun lagTaskForHentingAvIdenterTilPensjon(år: Int): String {
        val uuid = UUID.randomUUID()
        taskRepository.save(HentAlleIdenterTilPsysTask.lagTask(år, uuid))
        return uuid.toString()
    }

    private fun hentBarnetrygdForRelatertPersonTilPensjon(
        personIdent: String,
        fraDato: LocalDate,
        forelderAktør: Aktør,
    ): List<BarnetrygdTilPensjon> {
        val aktør = personidentService.hentAktør(personIdent)
        val fagsaker = fagsakRepository.finnFagsakerSomHarAndelerForAktør(aktør)
            .filter { it.type == FagsakType.NORMAL } // skal kun ha normale fagsaker til med her
            .filter { it.aktør != forelderAktør } // trenger ikke å hente data til forelderen på nytt
            .distinct()
        return fagsaker.mapNotNull { fagsak -> hentBarnetrygdTilPensjon(fagsak, fraDato) }
    }

    private fun hentBarnetrygdTilPensjon(fagsak: Fagsak, fraDato: LocalDate): BarnetrygdTilPensjon? {
        val behandling = behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(fagsak.id)
            ?: return null
        logger.info("Henter perioder med barnetrygd til pensjon for fagsakId=${fagsak.id}, behandlingId=${behandling.id}")

        val perioder = hentPerioder(behandling, fraDato)

        return BarnetrygdTilPensjon(
            fagsakId = fagsak.id.toString(),
            barnetrygdPerioder = perioder,
            fagsakEiersIdent = fagsak.aktør.aktivFødselsnummer(),
        )
    }

    private fun hentPerioder(
        behandling: Behandling,
        fraDato: LocalDate,
    ): List<BarnetrygdPeriode> {
        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandlingAndHasUtbetalingsoppdrag(behandling.id)
            ?: error("Finner ikke tilkjent ytelse for behandling=${behandling.id}")
        return tilkjentYtelse.andelerTilkjentYtelse
            .filter { it.stønadTom.isSameOrAfter(fraDato.toYearMonth()) }
            .map {
                BarnetrygdPeriode(
                    ytelseTypeEkstern = it.type.tilPensjonYtelsesType(),
                    personIdent = it.aktør.aktivFødselsnummer(),
                    stønadFom = it.stønadFom,
                    stønadTom = it.stønadTom,
                    utbetaltPerMnd = it.kalkulertUtbetalingsbeløp,
                    delingsprosentYtelse = it.prosent.toInt(),
                )
            }
    }

    fun YtelseType.tilPensjonYtelsesType(): YtelseTypeEkstern {
        return when (this) {
            YtelseType.ORDINÆR_BARNETRYGD -> YtelseTypeEkstern.ORDINÆR_BARNETRYGD
            YtelseType.SMÅBARNSTILLEGG -> YtelseTypeEkstern.SMÅBARNSTILLEGG
            YtelseType.UTVIDET_BARNETRYGD -> YtelseTypeEkstern.UTVIDET_BARNETRYGD
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BisysService::class.java)
    }
}
