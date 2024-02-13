package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.kontrakter.felles.oppdrag.Opphør
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import java.math.BigDecimal
import java.time.LocalDate.now
import java.time.YearMonth

/**
 * Lager mal for generering av utbetalingsperioder med tilpasset setting av verdier basert på parametre
 *
 * @param[vedtak] for vedtakdato og opphørsdato hvis satt
 * @param[erEndringPåEksisterendePeriode] ved true vil oppdrag sette asksjonskode ENDR på linje og ikke referere bakover
 * @return mal med tilpasset lagPeriodeFraAndel
 */
data class UtbetalingsperiodeMal(
    val vedtak: Vedtak,
    val erEndringPåEksisterendePeriode: Boolean = false,
) {

    /**
     * Lager utbetalingsperioder som legges på utbetalingsoppdrag. En utbetalingsperiode tilsvarer linjer hos økonomi
     *
     * Denne metoden brukes også til simulering og på dette tidspunktet er ikke vedtaksdatoen satt.
     * Derfor defaulter vi til now() når vedtaksdato mangler.
     *
     * @param[andel] andel som skal mappes til periode
     * @param[periodeIdOffset] brukes til å synce våre linjer med det som ligger hos økonomi
     * @param[forrigePeriodeIdOffset] peker til forrige i kjeden. Kun relevant når IKKE erEndringPåEksisterendePeriode
     * @param[opphørKjedeFom] fom-dato fra tidligste periode i kjede med endring
     * @return Periode til utbetalingsoppdrag
     */
    fun lagPeriodeFraAndel(
        andel: AndelTilkjentYtelseForUtbetalingsoppdrag,
        periodeIdOffset: Int,
        forrigePeriodeIdOffset: Int?,
        opphørKjedeFom: YearMonth? = null,
    ): Utbetalingsperiode =
        Utbetalingsperiode(
            erEndringPåEksisterendePeriode = erEndringPåEksisterendePeriode,
            opphør = if (erEndringPåEksisterendePeriode) {
                Opphør(
                    opphørKjedeFom?.førsteDagIInneværendeMåned()
                        ?: error("Mangler opphørsdato for kjede"),
                )
            } else {
                null
            },
            forrigePeriodeId = forrigePeriodeIdOffset?.let { forrigePeriodeIdOffset.toLong() },
            periodeId = periodeIdOffset.toLong(),
            datoForVedtak = vedtak.vedtaksdato?.toLocalDate() ?: now(),
            klassifisering = andel.type.klassifisering,
            vedtakdatoFom = andel.stønadFom.førsteDagIInneværendeMåned(),
            vedtakdatoTom = andel.stønadTom.sisteDagIInneværendeMåned(),
            sats = BigDecimal(andel.kalkulertUtbetalingsbeløp),
            satsType = Utbetalingsperiode.SatsType.MND,
            utbetalesTil = hentUtebetalesTil(vedtak.behandling.fagsak),
            behandlingId = vedtak.behandling.id,
        )

    private fun hentUtebetalesTil(fagsak: Fagsak): String {
        return when (fagsak.type) {
            FagsakType.INSTITUSJON -> {
                fagsak.institusjon?.tssEksternId
                    ?: error("Fagsak ${fagsak.id} er av type institusjon og mangler informasjon om institusjonen")
            }

            else -> {
                fagsak.aktør.aktivFødselsnummer()
            }
        }
    }
}
