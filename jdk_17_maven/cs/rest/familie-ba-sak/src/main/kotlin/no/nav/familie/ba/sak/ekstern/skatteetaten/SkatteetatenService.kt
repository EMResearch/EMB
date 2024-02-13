package no.nav.familie.ba.sak.ekstern.skatteetaten

import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdBarnetrygdClient
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.Behandlingutils
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.eksterne.kontrakter.skatteetaten.SkatteetatenPeriode
import no.nav.familie.eksterne.kontrakter.skatteetaten.SkatteetatenPerioder
import no.nav.familie.eksterne.kontrakter.skatteetaten.SkatteetatenPerioderResponse
import no.nav.familie.eksterne.kontrakter.skatteetaten.SkatteetatenPerson
import no.nav.familie.eksterne.kontrakter.skatteetaten.SkatteetatenPersonerResponse
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Service
class SkatteetatenService(
    private val infotrygdBarnetrygdClient: InfotrygdBarnetrygdClient,
    private val fagsakRepository: FagsakRepository,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
) {
    @Cacheable("skatt_personer", cacheManager = "skattPersonerCache", unless = "#result == null")
    fun finnPersonerMedUtvidetBarnetrygd(år: String): SkatteetatenPersonerResponse {
        LOG.info("Kaller finnPersonerMedUtvidetBarnetrygd for år=$år")
        val personerFraInfotrygd = infotrygdBarnetrygdClient.hentPersonerMedUtvidetBarnetrygd(år)
        LOG.info("Hentet ${personerFraInfotrygd.brukere.size} saker med utvidet fra infotrygd i $år")
        val personerFraBaSak = hentPersonerMedUtvidetBarnetrygd(år)
        LOG.info("Hentet ${personerFraBaSak.size} saker med utvidet fra basak i $år")

        val personIdentSet = personerFraBaSak.map { it.ident }.toSet()

        // Assumes that vedtak in ba-sak is always newer than that in Infotrygd for the same person ident
        val kombinertListe =
            personerFraBaSak + personerFraInfotrygd.brukere.filter { !personIdentSet.contains(it.ident) }

        return SkatteetatenPersonerResponse(kombinertListe)
    }

    fun finnPerioderMedUtvidetBarnetrygd(personer: List<String>, år: String): SkatteetatenPerioderResponse {
        val unikePersoner = personer.distinct()
        val perioderFraBaSak = hentPerioderMedUtvidetBarnetrygdFraBaSak(unikePersoner, år)
        LOG.info("Fant ${perioderFraBaSak.size} skatteetatenperioder fra ba-sak")
        val perioderFraInfotrygd =
            infotrygdBarnetrygdClient.hentPerioderMedUtvidetBarnetrygdForPersoner(unikePersoner, år)
        LOG.info("Fant ${perioderFraInfotrygd.size} skatteetatenperioder fra infotrygd")

        val samletPerioder = mutableListOf<SkatteetatenPerioder>()
        unikePersoner.forEach { personIdent ->
            val resultatInfotrygdForPerson =
                perioderFraInfotrygd.firstOrNull { perioder -> perioder.ident == personIdent }
            val perioderFraInfotrygdForPerson =
                resultatInfotrygdForPerson?.perioder ?: emptyList()

            val resultatBaSakForPerson = perioderFraBaSak.firstOrNull { perioder -> perioder.ident == personIdent }

            val perioderFraBasak = resultatBaSakForPerson?.perioder ?: emptyList()

            val perioder =
                (perioderFraBasak + perioderFraInfotrygdForPerson).groupBy { periode -> periode.delingsprosent }.values
                    .flatMap(::slåSammenSkatteetatenPeriode).toMutableList()
            if (perioder.isNotEmpty()) {
                samletPerioder.add(
                    SkatteetatenPerioder(
                        ident = personIdent,
                        perioder = perioder,
                        sisteVedtakPaaIdent = resultatBaSakForPerson?.sisteVedtakPaaIdent
                            ?: resultatInfotrygdForPerson!!.sisteVedtakPaaIdent,
                    ),
                )
            }
        }

        // Assumes that vedtak in ba-sak is always newer than that in Infotrygd for the same person ident
        return SkatteetatenPerioderResponse(samletPerioder)
    }

    private fun hentPersonerMedUtvidetBarnetrygd(år: String): List<SkatteetatenPerson> {
        return fagsakRepository.finnFagsakerMedUtvidetBarnetrygdInnenfor(
            fom = LocalDate.of(år.toInt(), 1, 1).atStartOfDay(),
            tom = LocalDate.of(år.toInt() + 1, 1, 1).atStartOfDay(),
        )
            .map { SkatteetatenPerson(it.fnr, it.sisteVedtaksdato.atStartOfDay()) }
    }

    private fun hentPerioderMedUtvidetBarnetrygdFraBaSak(
        personer: List<String>,
        år: String,
    ): List<SkatteetatenPerioder> {
        val stonadPerioder = hentUtvidetStonadPerioderForPersoner(personer, år)
        val aktivAndelTilkjentYtelsePeriode = mutableListOf<AndelTilkjentYtelsePeriode>()
        stonadPerioder.groupBy { it.getId() }.values.forEach { perioderGroupedByPerson ->
            if (perioderGroupedByPerson.size > 1) {
                val behandlinger =
                    perioderGroupedByPerson.map { behandlingHentOgPersisterService.hent(behandlingId = it.getBehandlingId()) }
                val sisteIverksatteBehandling = Behandlingutils.hentSisteBehandlingSomErIverksatt(behandlinger)
                if (sisteIverksatteBehandling != null) {
                    aktivAndelTilkjentYtelsePeriode.addAll(perioderGroupedByPerson.filter { it.getBehandlingId() == sisteIverksatteBehandling.id })
                }
            } else {
                aktivAndelTilkjentYtelsePeriode.add(perioderGroupedByPerson.first())
            }
        }

        val skatteetatenPerioderMap =
            stonadPerioder.fold(mutableMapOf<String, SkatteetatenPerioder>()) { perioderMap, period ->
                val ident = period.getIdent()
                val nyList = listOf(
                    SkatteetatenPeriode(
                        fraMaaned = period.getFom().format(DateTimeFormatter.ofPattern("yyyy-MM")),
                        delingsprosent = period.getProsent().tilDelingsprosent(),
                        tomMaaned = period.getTom().format(DateTimeFormatter.ofPattern("yyyy-MM")),
                    ),
                )
                val samletPerioder = if (perioderMap.containsKey(ident)) {
                    perioderMap[ident]!!.perioder + nyList
                } else {
                    nyList
                }
                perioderMap[ident] = SkatteetatenPerioder(ident, period.getEndretDato(), samletPerioder)
                perioderMap
            }

        return skatteetatenPerioderMap.toList().map {
            // Slå sammen perioder basert på delingsprosent
            SkatteetatenPerioder(
                ident = it.second.ident,
                sisteVedtakPaaIdent = it.second.sisteVedtakPaaIdent,
                perioder = it.second.perioder.groupBy { periode -> periode.delingsprosent }.values
                    .flatMap(::slåSammenSkatteetatenPeriode).toMutableList(),
            )
        }
    }

    private fun hentUtvidetStonadPerioderForPersoner(
        personIdenter: List<String>,
        år: String,
    ): List<AndelTilkjentYtelsePeriode> {
        val yearStart = LocalDateTime.of(år.toInt(), 1, 1, 0, 0, 0)
        val yearEnd = LocalDateTime.of(år.toInt(), 12, 31, 23, 59, 59)
        return andelTilkjentYtelseRepository.finnPerioderMedUtvidetBarnetrygdForPersoner(
            personIdenter,
            yearStart,
            yearEnd,
        )
    }

    private fun slåSammenSkatteetatenPeriode(perioderAvEtGittDelingsprosent: List<SkatteetatenPeriode>): List<SkatteetatenPeriode> {
        return perioderAvEtGittDelingsprosent.sortedBy { it.fraMaaned }
            .fold(mutableListOf()) { sammenslåttePerioder, nesteUtbetaling ->
                val nesteUtbetalingFraMåned = YearMonth.parse(nesteUtbetaling.fraMaaned)
                val forrigeUtbetalingTomMåned =
                    sammenslåttePerioder.lastOrNull()?.tomMaaned?.let { YearMonth.parse(it) }

                if (forrigeUtbetalingTomMåned?.isSameOrAfter(nesteUtbetalingFraMåned.minusMonths(1)) == true || (sammenslåttePerioder.isNotEmpty() && forrigeUtbetalingTomMåned == null)) {
                    val nySammenslåing =
                        sammenslåttePerioder.removeLast().copy(tomMaaned = nesteUtbetaling.tomMaaned)
                    sammenslåttePerioder.apply { add(nySammenslåing) }
                } else {
                    sammenslåttePerioder.apply { add(nesteUtbetaling) }
                }
            }
    }

    companion object {
        val LOG = LoggerFactory.getLogger(SkatteetatenService::class.java)
    }
}

fun String.tilDelingsprosent(): SkatteetatenPeriode.Delingsprosent =
    if (this == "100") {
        SkatteetatenPeriode.Delingsprosent._0
    } else if (this == "50") {
        SkatteetatenPeriode.Delingsprosent._50
    } else {
        SkatteetatenPeriode.Delingsprosent.usikker
    }

fun SkatteetatenPeriode.Delingsprosent.tilBigDecimal(): BigDecimal = when (this) {
    SkatteetatenPeriode.Delingsprosent._0 -> BigDecimal.valueOf(100)
    SkatteetatenPeriode.Delingsprosent._50 -> BigDecimal.valueOf(50)
    else -> BigDecimal.valueOf(0)
}

interface UtvidetSkatt {
    val fnr: String
    val sisteVedtaksdato: LocalDate
}
