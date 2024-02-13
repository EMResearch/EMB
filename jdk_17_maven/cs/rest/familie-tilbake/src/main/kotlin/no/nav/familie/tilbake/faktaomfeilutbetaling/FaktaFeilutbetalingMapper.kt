package no.nav.familie.tilbake.faktaomfeilutbetaling

import no.nav.familie.kontrakter.felles.Datoperiode
import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.kontrakter.felles.tilbakekreving.Faktainfo
import no.nav.familie.tilbake.api.dto.FaktaFeilutbetalingDto
import no.nav.familie.tilbake.api.dto.FeilutbetalingsperiodeDto
import no.nav.familie.tilbake.behandling.domain.Fagsystemsbehandling
import no.nav.familie.tilbake.behandling.domain.Varsel
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.FaktaFeilutbetaling
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagUtil
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

object FaktaFeilutbetalingMapper {

    fun tilRespons(
        faktaFeilutbetaling: FaktaFeilutbetaling?,
        kravgrunnlag: Kravgrunnlag431,
        revurderingsvedtaksdato: LocalDate,
        varsletData: Varsel?,
        fagsystemsbehandling: Fagsystemsbehandling,
    ): FaktaFeilutbetalingDto {
        val logiskePerioder =
            LogiskPeriodeUtil.utledLogiskPeriode(KravgrunnlagUtil.finnFeilutbetalingPrPeriode(kravgrunnlag))
        val feilutbetaltePerioder = hentFeilutbetaltePerioder(
            faktaFeilutbetaling = faktaFeilutbetaling,
            logiskePerioder = logiskePerioder,
        )
        val faktainfo = Faktainfo(
            revurderingsårsak = fagsystemsbehandling.årsak,
            revurderingsresultat = fagsystemsbehandling.resultat,
            tilbakekrevingsvalg = fagsystemsbehandling.tilbakekrevingsvalg,
            konsekvensForYtelser = fagsystemsbehandling.konsekvenser.map { it.konsekvens }.toSet(),
        )

        return FaktaFeilutbetalingDto(
            varsletBeløp = varsletData?.varselbeløp,
            revurderingsvedtaksdato = revurderingsvedtaksdato,
            begrunnelse = faktaFeilutbetaling?.begrunnelse ?: "",
            faktainfo = faktainfo,
            feilutbetaltePerioder = feilutbetaltePerioder,
            totaltFeilutbetaltBeløp = logiskePerioder.sumOf(LogiskPeriode::feilutbetaltBeløp),
            totalFeilutbetaltPeriode = utledTotalFeilutbetaltPeriode(logiskePerioder),
        )
    }

    private fun hentFeilutbetaltePerioder(
        faktaFeilutbetaling: FaktaFeilutbetaling?,
        logiskePerioder: List<LogiskPeriode>,
    ): List<FeilutbetalingsperiodeDto> {
        return faktaFeilutbetaling?.perioder?.map {
            FeilutbetalingsperiodeDto(
                periode = it.periode.toDatoperiode(),
                feilutbetaltBeløp = hentFeilutbetaltBeløp(logiskePerioder, it.periode),
                hendelsestype = it.hendelsestype,
                hendelsesundertype = it.hendelsesundertype,
            )
        } ?: logiskePerioder.map {
            FeilutbetalingsperiodeDto(
                periode = it.periode.toDatoperiode(),
                feilutbetaltBeløp = it.feilutbetaltBeløp,
            )
        }
    }

    private fun hentFeilutbetaltBeløp(logiskePerioder: List<LogiskPeriode>, faktaPeriode: Månedsperiode): BigDecimal {
        return logiskePerioder.first { faktaPeriode == it.periode }.feilutbetaltBeløp
    }

    private fun utledTotalFeilutbetaltPeriode(perioder: List<LogiskPeriode>): Datoperiode {
        var totalPeriodeFom: YearMonth? = null
        var totalPeriodeTom: YearMonth? = null
        for (periode in perioder) {
            totalPeriodeFom = if (totalPeriodeFom == null || totalPeriodeFom > periode.fom) periode.fom else totalPeriodeFom
            totalPeriodeTom = if (totalPeriodeTom == null || totalPeriodeTom < periode.tom) periode.tom else totalPeriodeTom
        }
        return Datoperiode(totalPeriodeFom!!, totalPeriodeTom!!)
    }
}
