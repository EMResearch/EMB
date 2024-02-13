package no.nav.familie.ba.sak.kjerne.brev

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.NullablePeriode
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.common.erSenereEnnInneværendeMåned
import no.nav.familie.ba.sak.common.førsteDagINesteMåned
import no.nav.familie.ba.sak.common.tilDagMånedÅr
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.common.tilMånedÅr
import no.nav.familie.ba.sak.kjerne.brev.domene.BrevBegrunnelseGrunnlagMedPersoner
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertKompetanse
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertUregistrertBarn
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertVedtaksperiode
import no.nav.familie.ba.sak.kjerne.brev.domene.RestBehandlingsgrunnlagForBrev
import no.nav.familie.ba.sak.kjerne.brev.domene.Valgbarhet
import no.nav.familie.ba.sak.kjerne.brev.domene.eøs.EØSBegrunnelseMedKompetanser
import no.nav.familie.ba.sak.kjerne.brev.domene.eøs.hentKompetanserForEØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.BrevPeriodeType
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriode
import no.nav.familie.ba.sak.kjerne.brev.domene.totaltUtbetalt
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.IVedtakBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vedtak.domene.BrevBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.EØSBegrunnelseData
import no.nav.familie.ba.sak.kjerne.vedtak.domene.EØSBegrunnelseDataMedKompetanse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.EØSBegrunnelseDataUtenKompetanse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.FritekstBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.MinimertRestPerson
import no.nav.familie.ba.sak.kjerne.vedtak.domene.hentBrevPeriodeType
import no.nav.familie.ba.sak.kjerne.vedtak.domene.tilBrevBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import java.time.LocalDate

class BrevPeriodeGenerator(
    private val restBehandlingsgrunnlagForBrev: RestBehandlingsgrunnlagForBrev,
    private val erFørsteVedtaksperiodePåFagsak: Boolean,
    private val uregistrerteBarn: List<MinimertUregistrertBarn>,
    private val brevMålform: Målform,
    private val minimertVedtaksperiode: MinimertVedtaksperiode,
    private val barnMedReduksjonFraForrigeBehandlingIdent: List<String>,
    private val minimerteKompetanserForPeriode: List<MinimertKompetanse>,
    private val minimerteKompetanserSomStopperRettFørPeriode: List<MinimertKompetanse>,
    private val dødeBarnForrigePeriode: List<String>,
) {

    fun genererBrevPeriode(): BrevPeriode? {
        val begrunnelseGrunnlagMedPersoner = hentBegrunnelsegrunnlagMedPersoner()
        val eøsBegrunnelserMedKompetanser = hentEøsBegrunnelserMedKompetanser()

        val begrunnelserOgFritekster =
            byggBegrunnelserOgFritekster(
                begrunnelserGrunnlagMedPersoner = begrunnelseGrunnlagMedPersoner,
                eøsBegrunnelserMedKompetanser = eøsBegrunnelserMedKompetanser,
            )

        if (begrunnelserOgFritekster.isEmpty()) return null

        val identerIBegrunnelene = begrunnelseGrunnlagMedPersoner
            .filter { it.vedtakBegrunnelseType.erInnvilget() }
            .flatMap { it.personIdenter }

        return byggBrevPeriode(
            begrunnelserOgFritekster = begrunnelserOgFritekster,
            identerIBegrunnelene = identerIBegrunnelene,
        )
    }

    fun hentEØSBegrunnelseData(eøsBegrunnelserMedKompetanser: List<EØSBegrunnelseMedKompetanser>): List<EØSBegrunnelseData> =
        eøsBegrunnelserMedKompetanser.flatMap { begrunnelseMedData ->
            val begrunnelse = begrunnelseMedData.begrunnelse

            if (begrunnelseMedData.kompetanser.isEmpty() && begrunnelse.vedtakBegrunnelseType == VedtakBegrunnelseType.EØS_AVSLAG) {
                val minimertePersonResultater =
                    restBehandlingsgrunnlagForBrev.minimertePersonResultater.filter { personResultat ->
                        personResultat.minimerteVilkårResultater.any {
                            it.erEksplisittAvslagPåSøknad == true &&
                                it.periodeFom?.førsteDagINesteMåned() == minimertVedtaksperiode.fom &&
                                it.standardbegrunnelser.contains(begrunnelse)
                        }
                    }

                val personerIBegrunnelse =
                    restBehandlingsgrunnlagForBrev.personerPåBehandling.filter { person -> minimertePersonResultater.any { personResultat -> personResultat.personIdent == person.personIdent } }
                val barnPåBehandling =
                    restBehandlingsgrunnlagForBrev.personerPåBehandling.filter { it.type == PersonType.BARN }
                val barnIBegrunnelse = personerIBegrunnelse.filter { it.type == PersonType.BARN }
                val gjelderSøker = personerIBegrunnelse.any { it.type == PersonType.SØKER }

                val barnasFødselsdatoer = hentBarnasFødselsdatoerForAvslagsbegrunnelse(
                    barnIBegrunnelse = barnIBegrunnelse,
                    barnPåBehandling = barnPåBehandling,
                    uregistrerteBarn = uregistrerteBarn,
                    gjelderSøker = gjelderSøker,
                )
                val antallBarn = hentAntallBarnForAvslagsbegrunnelse(
                    barnIBegrunnelse = barnIBegrunnelse,
                    barnPåBehandling = barnPåBehandling,
                    uregistrerteBarn = uregistrerteBarn,
                    gjelderSøker = gjelderSøker,
                )

                listOf(
                    EØSBegrunnelseDataUtenKompetanse(
                        vedtakBegrunnelseType = begrunnelse.vedtakBegrunnelseType,
                        apiNavn = begrunnelse.sanityApiNavn,
                        barnasFodselsdatoer = barnasFødselsdatoer,
                        antallBarn = antallBarn,
                        maalform = brevMålform.tilSanityFormat(),
                        gjelderSoker = gjelderSøker,
                    ),
                )
            } else {
                begrunnelseMedData.kompetanser.map { kompetanse ->
                    EØSBegrunnelseDataMedKompetanse(
                        vedtakBegrunnelseType = begrunnelse.vedtakBegrunnelseType,
                        apiNavn = begrunnelse.sanityApiNavn,
                        annenForeldersAktivitet = kompetanse.annenForeldersAktivitet,
                        annenForeldersAktivitetsland = kompetanse.annenForeldersAktivitetslandNavn?.navn,
                        barnetsBostedsland = kompetanse.barnetsBostedslandNavn.navn,
                        barnasFodselsdatoer = Utils.slåSammen(kompetanse.personer.map { it.fødselsdato.tilKortString() }),
                        antallBarn = kompetanse.personer.size,
                        maalform = brevMålform.tilSanityFormat(),
                        sokersAktivitet = kompetanse.søkersAktivitet,
                        sokersAktivitetsland = kompetanse.søkersAktivitetsland?.navn,
                    )
                }
            }
        }

    fun hentEøsBegrunnelserMedKompetanser(): List<EØSBegrunnelseMedKompetanser> =
        minimertVedtaksperiode.eøsBegrunnelser.map { eøsBegrunnelseMedTriggere ->
            val kompetanser = when (eøsBegrunnelseMedTriggere.eøsBegrunnelse.vedtakBegrunnelseType) {
                VedtakBegrunnelseType.EØS_INNVILGET, VedtakBegrunnelseType.EØS_FORTSATT_INNVILGET -> hentKompetanserForEØSBegrunnelse(
                    eøsBegrunnelseMedTriggere,
                    minimerteKompetanserForPeriode,
                )

                VedtakBegrunnelseType.EØS_OPPHØR, VedtakBegrunnelseType.EØS_REDUKSJON -> hentKompetanserForEØSBegrunnelse(
                    eøsBegrunnelseMedTriggere,
                    minimerteKompetanserSomStopperRettFørPeriode,
                )

                else -> emptyList()
            }
            EØSBegrunnelseMedKompetanser(
                begrunnelse = eøsBegrunnelseMedTriggere.eøsBegrunnelse,
                kompetanser = kompetanser,
            )
        }

    fun hentBegrunnelsegrunnlagMedPersoner() = minimertVedtaksperiode.begrunnelser.flatMap {
        it.tilBrevBegrunnelseGrunnlagMedPersoner(
            periode = NullablePeriode(
                fom = minimertVedtaksperiode.fom,
                tom = minimertVedtaksperiode.tom,
            ),
            vedtaksperiodetype = minimertVedtaksperiode.type,
            restBehandlingsgrunnlagForBrev = restBehandlingsgrunnlagForBrev,
            identerMedUtbetalingPåPeriode = minimertVedtaksperiode.minimerteUtbetalingsperiodeDetaljer
                .map { utbetalingsperiodeDetalj -> utbetalingsperiodeDetalj.person.personIdent },
            minimerteUtbetalingsperiodeDetaljer = minimertVedtaksperiode.minimerteUtbetalingsperiodeDetaljer,
            erFørsteVedtaksperiodePåFagsak = erFørsteVedtaksperiodePåFagsak,
            erUregistrerteBarnPåbehandling = uregistrerteBarn.isNotEmpty(),
            barnMedReduksjonFraForrigeBehandlingIdent = barnMedReduksjonFraForrigeBehandlingIdent,
            dødeBarnForrigePeriode = dødeBarnForrigePeriode,
        )
    }

    fun byggBegrunnelserOgFritekster(
        begrunnelserGrunnlagMedPersoner: List<BrevBegrunnelseGrunnlagMedPersoner>,
        eøsBegrunnelserMedKompetanser: List<EØSBegrunnelseMedKompetanser>,
    ): List<BrevBegrunnelse> {
        val brevBegrunnelser = begrunnelserGrunnlagMedPersoner
            .map {
                it.tilBrevBegrunnelse(
                    vedtaksperiode = NullablePeriode(minimertVedtaksperiode.fom, minimertVedtaksperiode.tom),
                    personerIPersongrunnlag = restBehandlingsgrunnlagForBrev.personerPåBehandling,
                    brevMålform = brevMålform,
                    uregistrerteBarn = uregistrerteBarn,
                    minimerteUtbetalingsperiodeDetaljer = minimertVedtaksperiode.minimerteUtbetalingsperiodeDetaljer,
                    minimerteRestEndredeAndeler = restBehandlingsgrunnlagForBrev.minimerteEndredeUtbetalingAndeler,
                )
            }

        val eøsBegrunnelser = hentEØSBegrunnelseData(eøsBegrunnelserMedKompetanser)

        val fritekster = minimertVedtaksperiode.fritekster.map { FritekstBegrunnelse(it) }

        return (brevBegrunnelser + eøsBegrunnelser + fritekster).sorted()
    }

    private fun byggBrevPeriode(
        begrunnelserOgFritekster: List<BrevBegrunnelse>,
        identerIBegrunnelene: List<String>,
    ): BrevPeriode {
        val (utbetalingerBarn, nullutbetalingerBarn) = minimertVedtaksperiode.minimerteUtbetalingsperiodeDetaljer
            .filter { it.person.type == PersonType.BARN }
            .partition { it.utbetaltPerMnd != 0 }

        val tomDato =
            if (minimertVedtaksperiode.tom?.erSenereEnnInneværendeMåned() == false) {
                minimertVedtaksperiode.tom.tilMånedÅr()
            } else {
                null
            }

        val barnMedUtbetaling = utbetalingerBarn.map { it.person }
        val barnMedNullutbetaling = nullutbetalingerBarn.map { it.person }

        val barnIPeriode: List<MinimertRestPerson> = when (minimertVedtaksperiode.type) {
            Vedtaksperiodetype.UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING,
            Vedtaksperiodetype.UTBETALING,
            -> finnBarnIUtbetalingPeriode(identerIBegrunnelene)

            Vedtaksperiodetype.OPPHØR -> emptyList()
            Vedtaksperiodetype.AVSLAG -> emptyList()
            Vedtaksperiodetype.FORTSATT_INNVILGET -> barnMedUtbetaling + barnMedNullutbetaling
            Vedtaksperiodetype.ENDRET_UTBETALING -> throw Feil("Endret utbetaling skal ikke benyttes lenger.")
        }

        val utbetalingsbeløp = minimertVedtaksperiode.minimerteUtbetalingsperiodeDetaljer.totaltUtbetalt()
        val brevPeriodeType = hentBrevPeriodeType(
            vedtaksperiodetype = minimertVedtaksperiode.type,
            fom = minimertVedtaksperiode.fom,
            erUtbetalingEllerDeltBostedIPeriode = minimertVedtaksperiode.minimerteUtbetalingsperiodeDetaljer.any { it.endringsårsak == Årsak.DELT_BOSTED || it.utbetaltPerMnd > 0 },
        )

        val duEllerInstitusjonen = hentDuEllerInstitusjonenTekst(brevPeriodeType)

        return BrevPeriode(

            fom = this.hentFomTekst(),
            tom = when {
                tomDato.isNullOrBlank() -> ""
                minimertVedtaksperiode.type == Vedtaksperiodetype.FORTSATT_INNVILGET -> ""
                minimertVedtaksperiode.type == Vedtaksperiodetype.AVSLAG -> "til og med $tomDato "
                brevPeriodeType == BrevPeriodeType.INGEN_UTBETALING -> ""
                brevPeriodeType == BrevPeriodeType.INNVILGELSE_INGEN_UTBETALING -> " til $tomDato"
                else -> "til $tomDato "
            },
            beløp = Utils.formaterBeløp(utbetalingsbeløp),
            begrunnelser = begrunnelserOgFritekster,
            brevPeriodeType = brevPeriodeType,
            antallBarn = barnIPeriode.size.toString(),
            barnasFodselsdager = barnIPeriode.tilBarnasFødselsdatoer(),
            duEllerInstitusjonen = duEllerInstitusjonen,
        )
    }

    private fun hentFomTekst(): String = if (minimertVedtaksperiode.fom != null) minimertVedtaksperiode.fom.tilMånedÅr() else ""

    @Deprecated("Erstattes av hentFomTekst når nye begrunnelse logikk går live")
    private fun hentFomTekstGammel(): String = when (minimertVedtaksperiode.type) {
        Vedtaksperiodetype.FORTSATT_INNVILGET -> hentFomtekstFortsattInnvilget(
            brevMålform,
            minimertVedtaksperiode.fom,
            minimertVedtaksperiode.begrunnelser.map { it.standardbegrunnelse },
        ) ?: "Du får:"

        Vedtaksperiodetype.UTBETALING -> minimertVedtaksperiode.fom!!.tilDagMånedÅr()
        Vedtaksperiodetype.OPPHØR -> minimertVedtaksperiode.fom!!.tilDagMånedÅr()
        Vedtaksperiodetype.AVSLAG -> if (minimertVedtaksperiode.fom != null) minimertVedtaksperiode.fom.tilDagMånedÅr() else ""
        Vedtaksperiodetype.UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING -> minimertVedtaksperiode.fom!!.tilDagMånedÅr()
        Vedtaksperiodetype.ENDRET_UTBETALING -> throw Feil("Endret utbetaling skal ikke benyttes lenger.")
    }

    private fun hentDuEllerInstitusjonenTekst(brevPeriodeType: BrevPeriodeType): String =
        when (restBehandlingsgrunnlagForBrev.fagsakType) {
            FagsakType.INSTITUSJON -> {
                when (brevPeriodeType) {
                    BrevPeriodeType.UTBETALING, BrevPeriodeType.INGEN_UTBETALING -> "institusjonen"
                    else -> "Institusjonen"
                }
            }

            FagsakType.NORMAL, FagsakType.BARN_ENSLIG_MINDREÅRIG -> {
                when (brevPeriodeType) {
                    BrevPeriodeType.UTBETALING, BrevPeriodeType.INGEN_UTBETALING -> "du"
                    else -> "Du"
                }
            }
        }

    fun finnBarnIUtbetalingPeriode(identerIBegrunnelene: List<String>): List<MinimertRestPerson> {
        val identerMedUtbetaling =
            minimertVedtaksperiode.minimerteUtbetalingsperiodeDetaljer.map { it.person.personIdent }

        val barnIPeriode = (identerIBegrunnelene + identerMedUtbetaling)
            .toSet()
            .mapNotNull { personIdent ->
                restBehandlingsgrunnlagForBrev.personerPåBehandling.find { it.personIdent == personIdent }
            }
            .filter { it.type == PersonType.BARN }

        return barnIPeriode
    }

    private fun hentFomtekstFortsattInnvilget(
        målform: Målform,
        fom: LocalDate?,
        begrunnelser: List<IVedtakBegrunnelse>,
    ): String? {
        val erAutobrev = begrunnelser.any {
            this.minimertVedtaksperiode.begrunnelser.any { it.triggesAv.valgbarhet == Valgbarhet.AUTOMATISK }
        }
        return if (erAutobrev && fom != null) {
            val fra = if (målform == Målform.NB) "Fra" else "Frå"
            "$fra ${fom.tilDagMånedÅr()} får du:"
        } else {
            null
        }
    }
}
