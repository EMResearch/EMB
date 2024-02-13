package no.nav.familie.ba.sak.kjerne.brev.domene

import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.IVedtakBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.TriggesAv
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.erAvslagUregistrerteBarnBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.MinimertRestPerson
import java.time.LocalDate

data class BrevBegrunnelseGrunnlagMedPersoner(
    val standardbegrunnelse: IVedtakBegrunnelse,
    val vedtakBegrunnelseType: VedtakBegrunnelseType,
    val triggesAv: TriggesAv,
    val personIdenter: List<String>,
    val avtaletidspunktDeltBosted: LocalDate? = null,
) {
    fun hentAntallBarnForBegrunnelse(
        uregistrerteBarn: List<MinimertUregistrertBarn>,
        gjelderSøker: Boolean,
        barnasFødselsdatoer: List<LocalDate>,
    ): Int {
        val erAvslagUregistrerteBarn = standardbegrunnelse.erAvslagUregistrerteBarnBegrunnelse()

        return when {
            erAvslagUregistrerteBarn -> uregistrerteBarn.size
            gjelderSøker && this.vedtakBegrunnelseType == VedtakBegrunnelseType.AVSLAG -> 0
            else -> barnasFødselsdatoer.size
        }
    }

    fun hentBarnasFødselsdagerForBegrunnelse(
        uregistrerteBarn: List<MinimertUregistrertBarn>,
        gjelderSøker: Boolean,
        personerIBehandling: List<MinimertRestPerson>,
        personerPåBegrunnelse: List<MinimertRestPerson>,
        personerMedUtbetaling: List<MinimertRestPerson>,
    ) = when {
        this.standardbegrunnelse.erAvslagUregistrerteBarnBegrunnelse() -> uregistrerteBarn.mapNotNull { it.fødselsdato }

        gjelderSøker && this.vedtakBegrunnelseType != VedtakBegrunnelseType.ENDRET_UTBETALING && this.vedtakBegrunnelseType != VedtakBegrunnelseType.ETTER_ENDRET_UTBETALING -> {
            when (this.vedtakBegrunnelseType) {
                VedtakBegrunnelseType.AVSLAG, VedtakBegrunnelseType.OPPHØR -> {
                    personerIBehandling
                        .filter { it.type == PersonType.BARN }
                        .map { it.fødselsdato } +
                        uregistrerteBarn.mapNotNull { it.fødselsdato }
                }

                else -> {
                    (personerMedUtbetaling + personerPåBegrunnelse).toSet()
                        .filter { it.type == PersonType.BARN }
                        .map { it.fødselsdato }
                }
            }
        }
        else ->
            personerPåBegrunnelse
                .filter { it.type == PersonType.BARN }
                .map { it.fødselsdato }
    }
}
