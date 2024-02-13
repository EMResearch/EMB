package no.nav.familie.tilbake.dokumentbestilling.vedtak

import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.tilbake.api.dto.PeriodeMedTekstDto
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Behandlingstype
import no.nav.familie.tilbake.behandling.domain.Behandlingsårsakstype
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.dokumentbestilling.vedtak.domain.Friteksttype
import no.nav.familie.tilbake.dokumentbestilling.vedtak.domain.Vedtaksbrevsoppsummering
import no.nav.familie.tilbake.dokumentbestilling.vedtak.domain.Vedtaksbrevsperiode
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.Vedtaksbrevstype
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.Vedtaksbrevstype.ORDINÆR
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.FaktaFeilutbetaling
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsesundertype
import no.nav.familie.tilbake.vilkårsvurdering.domain.SærligGrunn
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurdering
import org.springframework.http.HttpStatus

object VedtaksbrevFritekstValidator {

    @Throws(Feil::class)
    fun validerObligatoriskeFritekster(
        behandling: Behandling,
        faktaFeilutbetaling: FaktaFeilutbetaling,
        vilkårsvurdering: Vilkårsvurdering?,
        vedtaksbrevFritekstPerioder: List<Vedtaksbrevsperiode>,
        avsnittMedPerioder: List<PeriodeMedTekstDto>,
        vedtaksbrevsoppsummering: Vedtaksbrevsoppsummering,
        vedtaksbrevstype: Vedtaksbrevstype,
        validerPåkrevetFritekster: Boolean,
        skalIkkeValidereAnnetFritekst: Boolean,
    ) {
        validerPerioder(behandling, avsnittMedPerioder, faktaFeilutbetaling)

        if (!skalIkkeValidereAnnetFritekst) {
            vilkårsvurdering?.let {
                validerFritekstISærligGrunnerAnnetAvsnitt(
                    it,
                    vedtaksbrevFritekstPerioder,
                    validerPåkrevetFritekster,
                )
            }
        }

        if (ORDINÆR == vedtaksbrevstype) {
            validerFritekstIFaktaAvsnitt(
                faktaFeilutbetaling,
                vedtaksbrevFritekstPerioder,
                avsnittMedPerioder,
                validerPåkrevetFritekster,
            )
        }
        validerOppsummeringsfritekstLengde(behandling, vedtaksbrevsoppsummering, vedtaksbrevstype)
        if (validerPåkrevetFritekster) {
            validerNårOppsummeringsfritekstErPåkrevd(behandling, vedtaksbrevsoppsummering)
        }
    }

    private fun validerPerioder(
        behandling: Behandling,
        avsnittMedPerioder: List<PeriodeMedTekstDto>,
        faktaFeilutbetaling: FaktaFeilutbetaling,
    ) {
        avsnittMedPerioder.forEach {
            if (!faktaFeilutbetaling.perioder.any { faktaPeriode ->
                    faktaPeriode.periode.inneholder(it.periode.toMånedsperiode())
                }
            ) {
                throw Feil(
                    message = "Periode ${it.periode.fom}-${it.periode.tom} er ugyldig for behandling ${behandling.id}",
                    frontendFeilmelding = "Periode ${it.periode.fom}-${it.periode.tom} er ugyldig " +
                        "for behandling ${behandling.id}",
                    httpStatus = HttpStatus.BAD_REQUEST,
                )
            }
        }
    }

    private fun validerNårOppsummeringsfritekstErPåkrevd(
        behandling: Behandling,
        vedtaksbrevsoppsummering: Vedtaksbrevsoppsummering,
    ) {
        val revurderingIkkeOpprettetEtterKlage = behandling.årsaker.none {
            it.type in setOf(
                Behandlingsårsakstype.REVURDERING_KLAGE_KA,
                Behandlingsårsakstype.REVURDERING_KLAGE_NFP,
            )
        }
        if (Behandlingstype.REVURDERING_TILBAKEKREVING == behandling.type &&
            revurderingIkkeOpprettetEtterKlage &&
            vedtaksbrevsoppsummering.oppsummeringFritekst.isNullOrEmpty()
        ) {
            throw Feil(
                message = "oppsummering fritekst påkrevet for revurdering ${behandling.id}",
                frontendFeilmelding = "oppsummering fritekst påkrevet for revurdering ${behandling.id}",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
    }

    private fun validerOppsummeringsfritekstLengde(
        behandling: Behandling,
        vedtaksbrevsoppsummering: Vedtaksbrevsoppsummering,
        vedtaksbrevstype: Vedtaksbrevstype,
    ) {
        val maksTekstLengde = when (vedtaksbrevstype) {
            ORDINÆR -> 4000
            else -> 10000
        }
        if (vedtaksbrevsoppsummering.oppsummeringFritekst != null &&
            vedtaksbrevsoppsummering.oppsummeringFritekst.length > maksTekstLengde
        ) {
            throw Feil(
                message = "Oppsummeringstekst er for lang for behandling ${behandling.id}",
                frontendFeilmelding = "Oppsummeringstekst er for lang for behandling ${behandling.id}",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
    }

    private fun validerFritekstIFaktaAvsnitt(
        faktaFeilutbetaling: FaktaFeilutbetaling,
        vedtaksbrevFritekstPerioder: List<Vedtaksbrevsperiode>,
        avsnittMedPerioder: List<PeriodeMedTekstDto>,
        validerPåkrevetFritekster: Boolean,
    ) {
        faktaFeilutbetaling.perioder.filter { Hendelsesundertype.ANNET_FRITEKST == it.hendelsesundertype }
            .forEach { faktaFeilutbetalingsperiode ->
                val perioder = finnFritekstPerioder(
                    vedtaksbrevFritekstPerioder,
                    faktaFeilutbetalingsperiode.periode,
                    Friteksttype.FAKTA,
                )
                if (perioder.isEmpty() && validerPåkrevetFritekster) {
                    throw Feil(
                        message = "Mangler fakta fritekst for alle fakta perioder",
                        frontendFeilmelding = "Mangler Fakta fritekst for alle fakta perioder",
                        httpStatus = HttpStatus.BAD_REQUEST,
                    )
                }
                // Hvis en av de periodene mangler fritekst
                val omsluttetPerioder = avsnittMedPerioder.filter {
                    faktaFeilutbetalingsperiode.periode.inneholder(it.periode.toMånedsperiode())
                }
                omsluttetPerioder.forEach {
                    if (it.faktaAvsnitt.isNullOrBlank() && validerPåkrevetFritekster) {
                        throw Feil(
                            message = "Mangler fakta fritekst for ${it.periode.fom}-${it.periode.tom}",
                            frontendFeilmelding = "Mangler Fakta fritekst for ${it.periode.fom}-${it.periode.tom}",
                            httpStatus = HttpStatus.BAD_REQUEST,
                        )
                    }
                }
            }
    }

    private fun validerFritekstISærligGrunnerAnnetAvsnitt(
        vilkårsvurdering: Vilkårsvurdering,
        vedtaksbrevFritekstPerioder: List<Vedtaksbrevsperiode>,
        validerPåkrevetFritekster: Boolean,
    ) {
        vilkårsvurdering.perioder.filter {
            it.aktsomhet?.vilkårsvurderingSærligeGrunner != null &&
                it.aktsomhet.vilkårsvurderingSærligeGrunner
                    .any { særligGrunn -> SærligGrunn.ANNET == særligGrunn.særligGrunn }
        }.forEach {
            val perioder = finnFritekstPerioder(
                vedtaksbrevFritekstPerioder,
                it.periode,
                Friteksttype.SÆRLIGE_GRUNNER_ANNET,
            )

            if (perioder.isEmpty() && validerPåkrevetFritekster) {
                throw Feil(
                    message = "Mangler ANNET Særliggrunner fritekst for ${it.periode}",
                    frontendFeilmelding = "Mangler ANNET Særliggrunner fritekst for ${it.periode} ",
                    httpStatus = HttpStatus.BAD_REQUEST,
                )
            }
        }
    }

    private fun finnFritekstPerioder(
        vedtaksbrevFritekstPerioder: List<Vedtaksbrevsperiode>,
        vurdertPeriode: Månedsperiode,
        friteksttype: Friteksttype,
    ): List<Vedtaksbrevsperiode> {
        return vedtaksbrevFritekstPerioder.filter {
            friteksttype == it.fritekststype &&
                vurdertPeriode.inneholder(it.periode)
        }
    }
}
