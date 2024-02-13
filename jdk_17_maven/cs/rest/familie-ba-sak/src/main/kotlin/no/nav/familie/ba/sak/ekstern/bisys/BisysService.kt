package no.nav.familie.ba.sak.ekstern.bisys

import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdBarnetrygdClient
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class BisysService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val infotrygdBarnetrygdClient: InfotrygdBarnetrygdClient,
    private val fagsakRepository: FagsakRepository,
    private val personidentService: PersonidentService,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
) {
    fun hentUtvidetBarnetrygd(personIdent: String, fraDato: LocalDate): BisysUtvidetBarnetrygdResponse {
        val aktør = personidentService.hentAktør(personIdent)

        val samledeUtvidetBarnetrygdPerioder = mutableListOf<UtvidetBarnetrygdPeriode>()
        samledeUtvidetBarnetrygdPerioder.addAll(hentBisysPerioderFraInfotrygd(aktør, fraDato))

        val perioderFraBasak = hentBisysPerioderFraBaSak(aktør, fraDato)
        secureLogger.info("Bisysperioder for $personIdent i ba-sak: perioderFraBasak=$perioderFraBasak")
        samledeUtvidetBarnetrygdPerioder.addAll(perioderFraBasak)

        val sammenslåttePerioder =
            samledeUtvidetBarnetrygdPerioder.filter { it.stønadstype == BisysStønadstype.UTVIDET }
                .groupBy { it.beløp }.values
                .flatMap(::slåSammenSammenhengendePerioder).toMutableList()

        sammenslåttePerioder.addAll(
            samledeUtvidetBarnetrygdPerioder.filter { it.stønadstype == BisysStønadstype.SMÅBARNSTILLEGG }
                .groupBy { it.beløp }.values
                .flatMap(::slåSammenSammenhengendePerioder),
        )

        return BisysUtvidetBarnetrygdResponse(
            sammenslåttePerioder.sortedWith(
                compareBy(
                    { it.stønadstype },
                    { it.fomMåned },
                ),
            ),
        )
    }

    private fun hentBisysPerioderFraInfotrygd(
        aktør: Aktør,
        fraDato: LocalDate,
    ): List<UtvidetBarnetrygdPeriode> =
        personidentService.hentAlleFødselsnummerForEnAktør(aktør).flatMap {
            infotrygdBarnetrygdClient.hentUtvidetBarnetrygd(it, fraDato.toYearMonth()).perioder
        }

    private fun hentBisysPerioderFraBaSak(
        aktør: Aktør,
        fraDato: LocalDate,
    ): List<UtvidetBarnetrygdPeriode> {
        val fagsak = fagsakRepository.finnFagsakForAktør(aktør)
        val behandling = fagsak?.let { behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(it.id) }
        if (fagsak == null || behandling == null) {
            return emptyList()
        }
        logger.info("Henter bisysperioder for siste iverksette behandlong for fagsakId=${fagsak.id}, behandlingId=${behandling.id}")
        return tilkjentYtelseRepository.findByBehandlingAndHasUtbetalingsoppdrag(behandling.id)?.andelerTilkjentYtelse
            ?.filter { it.erSøkersAndel() }
            ?.filter {
                it.stønadTom.isSameOrAfter(fraDato.toYearMonth())
            }
            ?.map {
                UtvidetBarnetrygdPeriode(
                    stønadstype = if (it.erUtvidet()) BisysStønadstype.UTVIDET else BisysStønadstype.SMÅBARNSTILLEGG,
                    fomMåned = it.stønadFom,
                    tomMåned = it.stønadTom,
                    beløp = it.kalkulertUtbetalingsbeløp.toDouble(),
                    manueltBeregnet = false,
                    deltBosted = it.erDeltBosted(),
                )
            } ?: emptyList()
    }

    private fun slåSammenSammenhengendePerioder(utbetalingerAvSammeBeløp: List<UtvidetBarnetrygdPeriode>): List<UtvidetBarnetrygdPeriode> {
        return utbetalingerAvSammeBeløp.sortedBy { it.fomMåned }
            .fold(mutableListOf()) { sammenslåttePerioder, nesteUtbetaling ->
                if (sammenslåttePerioder.lastOrNull()?.tomMåned?.isSameOrAfter(nesteUtbetaling.fomMåned.minusMonths(1)) != false &&
                    sammenslåttePerioder.lastOrNull()?.manueltBeregnet == nesteUtbetaling.manueltBeregnet &&
                    sammenslåttePerioder.lastOrNull()?.deltBosted == nesteUtbetaling.deltBosted
                ) {
                    sammenslåttePerioder.apply { add(removeLast().copy(tomMåned = nesteUtbetaling.tomMåned)) }
                } else {
                    sammenslåttePerioder.apply { add(nesteUtbetaling) }
                }
            }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BisysService::class.java)
    }
}
