package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent

import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.brev.brevBegrunnelseProdusent.GrunnlagForBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.EndretUtbetalingsperiodeDeltBostedTriggere
import no.nav.familie.ba.sak.kjerne.brev.domene.ISanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityEØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityPeriodeResultat
import no.nav.familie.ba.sak.kjerne.brev.domene.Tema
import no.nav.familie.ba.sak.kjerne.brev.domene.UtvidetBarnetrygdTrigger
import no.nav.familie.ba.sak.kjerne.brev.domene.Valgbarhet
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.BrevPeriodeType
import no.nav.familie.ba.sak.kjerne.brev.domene.tilPersonType
import no.nav.familie.ba.sak.kjerne.brev.domene.ØvrigTrigger
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.TomTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombiner
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.mapInnhold
import no.nav.familie.ba.sak.kjerne.tidslinje.månedPeriodeAv
import no.nav.familie.ba.sak.kjerne.tidslinje.periodeAv
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilYearMonthEllerUendeligFortid
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilYearMonthEllerUendeligFramtid
import no.nav.familie.ba.sak.kjerne.tidslinje.tilTidslinje
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.EØSStandardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.IVedtakBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.landkodeTilBarnetsBostedsland
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.domene.hentBrevPeriodeType
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.AndelForVedtaksperiode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.BehandlingsGrunnlagForVedtaksperioder
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.EndretUtbetalingAndelForVedtaksperiode
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.tilForskjøvedeVilkårTidslinjer
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

fun VedtaksperiodeMedBegrunnelser.hentGyldigeBegrunnelserForPeriode(
    grunnlagForBegrunnelser: GrunnlagForBegrunnelse,
): Set<IVedtakBegrunnelse> {
    val gyldigeBegrunnelserPerPerson = hentGyldigeBegrunnelserPerPerson(
        grunnlagForBegrunnelser,
    )

    return gyldigeBegrunnelserPerPerson.values.flatten().toSet()
}

fun VedtaksperiodeMedBegrunnelser.hentGyldigeBegrunnelserPerPerson(
    grunnlag: GrunnlagForBegrunnelse,
): Map<Person, Set<IVedtakBegrunnelse>> {
    val avslagsbegrunnelserPerPerson = hentAvslagsbegrunnelserPerPerson(grunnlag.behandlingsGrunnlagForVedtaksperioder)

    if (this.type == Vedtaksperiodetype.AVSLAG) {
        return avslagsbegrunnelserPerPerson
    }

    val begrunnelseGrunnlagPerPerson = this.finnBegrunnelseGrunnlagPerPerson(
        grunnlag,
    )

    if (this.type == Vedtaksperiodetype.FORTSATT_INNVILGET) {
        return hentFortsattInnvilgetBegrunnelserPerPerson(
            begrunnelseGrunnlagPerPerson = begrunnelseGrunnlagPerPerson,
            grunnlag = grunnlag,
            vedtaksperiode = this,

        )
    }

    val erUtbetalingEllerDeltBostedIPeriode = erUtbetalingEllerDeltBostedIPeriode(begrunnelseGrunnlagPerPerson)

    return begrunnelseGrunnlagPerPerson.mapValues { (person, begrunnelseGrunnlag) ->
        val relevantePeriodeResultater =
            hentResultaterForPeriode(begrunnelseGrunnlag.dennePerioden, begrunnelseGrunnlag.forrigePeriode)

        val standardBegrunnelser = hentStandardBegrunnelser(
            begrunnelseGrunnlag = begrunnelseGrunnlag,
            sanityBegrunnelser = grunnlag.sanityBegrunnelser,
            person = person,
            vedtaksperiode = this,
            fagsakType = grunnlag.behandlingsGrunnlagForVedtaksperioder.fagsakType,
            relevantePeriodeResultater = relevantePeriodeResultater,
            erUtbetalingEllerDeltBostedIPeriode = erUtbetalingEllerDeltBostedIPeriode,
        )

        val eøsBegrunnelser = hentEØSStandardBegrunnelser(
            sanityEØSBegrunnelser = grunnlag.sanityEØSBegrunnelser,
            begrunnelseGrunnlag = begrunnelseGrunnlag,
            relevantePeriodeResultater = relevantePeriodeResultater,
            erUtbetalingEllerDeltBostedIPeriode = erUtbetalingEllerDeltBostedIPeriode,
            vedtaksperiode = this,
        )

        val avslagsbegrunnelser = avslagsbegrunnelserPerPerson[person] ?: emptySet()

        val temaSomPeriodeErVurdertEtter = hentTemaSomPeriodeErVurdertEtter(begrunnelseGrunnlag)

        val standardOgEøsBegrunnelser: Map<IVedtakBegrunnelse, ISanityBegrunnelse> =
            (standardBegrunnelser + eøsBegrunnelser)

        val standardOgEøsBegrunnelserFiltrertPåTema =
            standardOgEøsBegrunnelser.filtrerPåTema(temaSomPeriodeErVurdertEtter)

        standardOgEøsBegrunnelserFiltrertPåTema + avslagsbegrunnelser
    }
}

fun erUtbetalingEllerDeltBostedIPeriode(begrunnelseGrunnlagPerPerson: Map<Person, IBegrunnelseGrunnlagForPeriode>) =
    begrunnelseGrunnlagPerPerson.values.any { grunnlagForPeriode ->
        val dennePerioden = grunnlagForPeriode.dennePerioden
        dennePerioden.endretUtbetalingAndel?.årsak == Årsak.DELT_BOSTED ||
            dennePerioden.andeler.any { it.prosent != BigDecimal.ZERO }
    }

private fun Map<IVedtakBegrunnelse, ISanityBegrunnelse>.filtrerPåTema(
    temaSomPeriodeErVurdertEtter: Tema,
) = filter {
    val temaPåBegrunnelse = it.value.tema

    temaSomPeriodeErVurdertEtter == temaPåBegrunnelse || temaPåBegrunnelse == Tema.FELLES
}.keys.toSet()

fun hentTemaSomPeriodeErVurdertEtter(
    begrunnelseGrunnlag: IBegrunnelseGrunnlagForPeriode,
): Tema {
    val harKompetanseDennePerioden = begrunnelseGrunnlag.dennePerioden.kompetanse != null
    val harMistetKompetanseDennePerioden =
        !harKompetanseDennePerioden && begrunnelseGrunnlag.forrigePeriode?.kompetanse != null
    val harGåttFraEøsTilNasjonal =
        harMistetKompetanseDennePerioden && begrunnelseGrunnlag.dennePerioden.andeler.any { it.nasjonaltPeriodebeløp != 0 }

    return when {
        harGåttFraEøsTilNasjonal -> Tema.NASJONAL
        harKompetanseDennePerioden || harMistetKompetanseDennePerioden -> Tema.EØS
        else -> Tema.NASJONAL
    }
}

private fun VedtaksperiodeMedBegrunnelser.hentAvslagsbegrunnelserPerPerson(
    behandlingsGrunnlagForVedtaksperioder: BehandlingsGrunnlagForVedtaksperioder,
): Map<Person, Set<IVedtakBegrunnelse>> {
    val tidslinjeMedVedtaksperioden = this.tilTidslinjeForAktuellPeriode()

    return behandlingsGrunnlagForVedtaksperioder.persongrunnlag.personer.associateWith { person ->
        val avslagsbegrunnelserTisdlinje =
            behandlingsGrunnlagForVedtaksperioder.personResultater.single { it.aktør == person.aktør }.vilkårResultater.filter { it.erEksplisittAvslagPåSøknad == true }
                .tilForskjøvedeVilkårTidslinjer(person.fødselsdato)
                .kombiner { vilkårResultaterIPeriode -> vilkårResultaterIPeriode.flatMap { it.standardbegrunnelser } }

        tidslinjeMedVedtaksperioden.kombinerMed(avslagsbegrunnelserTisdlinje) { h, v ->
            v.takeIf { h != null }
        }.perioder().mapNotNull { it.innhold }.flatten().toSet()
    }
}

private fun hentStandardBegrunnelser(
    begrunnelseGrunnlag: IBegrunnelseGrunnlagForPeriode,
    sanityBegrunnelser: Map<Standardbegrunnelse, SanityBegrunnelse>,
    person: Person,
    vedtaksperiode: VedtaksperiodeMedBegrunnelser,
    fagsakType: FagsakType,
    relevantePeriodeResultater: List<SanityPeriodeResultat>,
    erUtbetalingEllerDeltBostedIPeriode: Boolean,
): Map<Standardbegrunnelse, SanityBegrunnelse> {
    val endretUtbetalingDennePerioden = hentEndretUtbetalingDennePerioden(begrunnelseGrunnlag)

    val relevantePeriodeResultaterForrigePeriode = hentResultaterForForrigePeriode(begrunnelseGrunnlag.forrigePeriode)

    val filtrertPåRolle = sanityBegrunnelser.filterValues { begrunnelse ->
        begrunnelse.erGjeldendeForRolle(person, fagsakType)
    }
    val filtrertPåRolleOgFagsaktype = filtrertPåRolle.filterValues {
        it.erGjeldendeForFagsakType(fagsakType)
    }
    val filtrertPåRolleFagsaktypeOgPeriodetype = filtrertPåRolleOgFagsaktype.filterValues {
        it.periodeResultat in relevantePeriodeResultater
    }

    val filtrertPåRolleFagsaktypePeriodeTypeOgManuelleBegrunnelser =
        filtrertPåRolleFagsaktypeOgPeriodetype.filterValues {
            it.erManuellBegrunnelse()
        }

    val relevanteBegrunnelser = filtrertPåRolleFagsaktypePeriodeTypeOgManuelleBegrunnelser
        .filterValues { it.erGjeldendeForBrevPeriodeType(vedtaksperiode, erUtbetalingEllerDeltBostedIPeriode) }
        .filterValues { !it.begrunnelseGjelderReduksjonFraForrigeBehandling() && !it.begrunnelseGjelderOpphørFraForrigeBehandling() }

    val filtrertPåVilkårOgEndretUtbetaling = relevanteBegrunnelser.filterValues {
        val begrunnelseErGjeldendeForUtgjørendeVilkår = it.vilkår.isNotEmpty()
        val begrunnelseErGjeldendeForEndretUtbetaling = it.endringsaarsaker.isNotEmpty()

        when {
            begrunnelseErGjeldendeForUtgjørendeVilkår && begrunnelseErGjeldendeForEndretUtbetaling -> filtrerPåVilkår(
                it,
                begrunnelseGrunnlag,
            ) && filtrerPåEndretUtbetaling(it, endretUtbetalingDennePerioden)

            begrunnelseErGjeldendeForUtgjørendeVilkår -> filtrerPåVilkår(it, begrunnelseGrunnlag)
            else -> it.erEndretUtbetaling(endretUtbetalingDennePerioden)
        }
    }

    val filtrertPåReduksjonFraForrigeBehandling = filtrertPåRolleOgFagsaktype.filterValues {
        it.erGjeldendeForReduksjonFraForrigeBehandling(begrunnelseGrunnlag)
    }

    val filtrertPåOpphørFraForrigeBehandling = filtrertPåRolleOgFagsaktype.filterValues {
        it.erGjeldendeForOpphørFraForrigeBehandling(begrunnelseGrunnlag)
    }

    val filtrertPåSmåbarnstillegg =
        relevanteBegrunnelser.filterValues { begrunnelse ->
            begrunnelse.erGjeldendeForSmåbarnstillegg(begrunnelseGrunnlag)
        }

    val begrunnelserFiltrertPåPeriodetypeForrigePeriode = sanityBegrunnelser.filterValues {
        it.periodeResultat in relevantePeriodeResultaterForrigePeriode
    }

    val filtrertPåRolleOgPeriodetypeForrigePeriode =
        begrunnelserFiltrertPåPeriodetypeForrigePeriode.filterValues { begrunnelse ->
            begrunnelse.erGjeldendeForRolle(person, fagsakType)
        }

    val filtrertPåEtterEndretUtbetaling = filtrertPåRolleOgPeriodetypeForrigePeriode.filterValues {
        it.erEtterEndretUtbetaling(
            endretUtbetalingDennePerioden = endretUtbetalingDennePerioden,
            endretUtbetalingForrigePeriode = hentEndretUtbetalingForrigePeriode(begrunnelseGrunnlag),
        )
    }

    val filtrertPåHendelser = relevanteBegrunnelser.filtrerPåHendelser(
        begrunnelseGrunnlag,
        vedtaksperiode.fom,
    )

    return filtrertPåVilkårOgEndretUtbetaling + filtrertPåReduksjonFraForrigeBehandling + filtrertPåOpphørFraForrigeBehandling + filtrertPåSmåbarnstillegg + filtrertPåEtterEndretUtbetaling + filtrertPåHendelser
}

private fun SanityBegrunnelse.erManuellBegrunnelse() = ØvrigTrigger.ALLTID_AUTOMATISK !in ovrigeTriggere

fun ISanityBegrunnelse.erGjeldendeForFagsakType(
    fagsakType: FagsakType,
) = if (valgbarhet == Valgbarhet.SAKSPESIFIKK) {
    fagsakType == this.fagsakType
} else {
    true
}

private fun filtrerPåEndretUtbetaling(
    it: SanityBegrunnelse,
    endretUtbetalingDennePerioden: EndretUtbetalingAndelForVedtaksperiode?,
) = it.erEndretUtbetaling(endretUtbetalingDennePerioden)

private fun filtrerPåVilkår(
    it: SanityBegrunnelse,
    begrunnelseGrunnlag: IBegrunnelseGrunnlagForPeriode,
) =
    !it.begrunnelseGjelderReduksjonFraForrigeBehandling() && it.erGjeldendeForUtgjørendeVilkår(begrunnelseGrunnlag) && it.erGjeldendeForRegelverk(
        begrunnelseGrunnlag,
    )

private fun SanityBegrunnelse.erGjeldendeForRegelverk(begrunnelseGrunnlag: IBegrunnelseGrunnlagForPeriode): Boolean =
    begrunnelseGrunnlag.dennePerioden.vilkårResultater.none { it.vurderesEtter == Regelverk.EØS_FORORDNINGEN } || this.tema == Tema.FELLES

private fun SanityBegrunnelse.erGjeldendeForReduksjonFraForrigeBehandling(begrunnelseGrunnlag: IBegrunnelseGrunnlagForPeriode): Boolean {
    if (begrunnelseGrunnlag !is BegrunnelseGrunnlagForPeriodeMedReduksjonPåTversAvBehandlinger) {
        return false
    }

    val oppfylteVilkårDenneBehandlingen =
        begrunnelseGrunnlag.dennePerioden.vilkårResultater.filter { it.resultat == Resultat.OPPFYLT }
            .map { it.vilkårType }.toSet()
    val oppfylteVilkårForrigeBehandling =
        begrunnelseGrunnlag.sammePeriodeForrigeBehandling?.vilkårResultater?.filter { it.resultat == Resultat.OPPFYLT }
            ?.map { it.vilkårType }?.toSet() ?: emptySet()

    val vilkårMistetSidenForrigeBehandling = oppfylteVilkårForrigeBehandling - oppfylteVilkårDenneBehandlingen

    val begrunnelseGjelderMistedeVilkår = this.vilkår.all { it in vilkårMistetSidenForrigeBehandling }

    val haddeSmåbarnstilleggForrigeBehandling = begrunnelseGrunnlag.erSmåbarnstilleggIForrigeBehandlingPeriode
    val harSmåbarnstilleggDennePerioden =
        begrunnelseGrunnlag.dennePerioden.andeler.any { it.type == YtelseType.SMÅBARNSTILLEGG }

    val begrunnelseGjelderTaptSmåbarnstillegg =
        UtvidetBarnetrygdTrigger.SMÅBARNSTILLEGG in utvidetBarnetrygdTriggere && haddeSmåbarnstilleggForrigeBehandling && !harSmåbarnstilleggDennePerioden

    return begrunnelseGjelderReduksjonFraForrigeBehandling() && (begrunnelseGjelderMistedeVilkår || begrunnelseGjelderTaptSmåbarnstillegg)
}

private fun SanityBegrunnelse.begrunnelseGjelderReduksjonFraForrigeBehandling() =
    ØvrigTrigger.GJELDER_FRA_INNVILGELSESTIDSPUNKT in this.ovrigeTriggere || ØvrigTrigger.REDUKSJON_FRA_FORRIGE_BEHANDLING in this.ovrigeTriggere

private fun SanityBegrunnelse.erGjeldendeForOpphørFraForrigeBehandling(begrunnelseGrunnlag: IBegrunnelseGrunnlagForPeriode): Boolean {
    if (begrunnelseGrunnlag !is BegrunnelseGrunnlagForPeriodeMedOpphør || !begrunnelseGjelderOpphørFraForrigeBehandling()) {
        return false
    }

    val oppfylteVilkårDenneBehandlingen =
        begrunnelseGrunnlag.dennePerioden.vilkårResultater.filter { it.resultat == Resultat.OPPFYLT }
            .map { it.vilkårType }.toSet()

    val oppfylteVilkårsresultaterForrigeBehandling =
        begrunnelseGrunnlag.sammePeriodeForrigeBehandling?.vilkårResultater?.filter { it.resultat == Resultat.OPPFYLT }
    val oppfylteVilkårForrigeBehandling =
        oppfylteVilkårsresultaterForrigeBehandling?.map { it.vilkårType }?.toSet() ?: emptySet()

    val vilkårMistetSidenForrigeBehandling = oppfylteVilkårForrigeBehandling - oppfylteVilkårDenneBehandlingen

    val begrunnelseGjelderMistedeVilkår = this.erLikVilkårOgUtdypendeVilkårIPeriode(
        oppfylteVilkårsresultaterForrigeBehandling?.filter { it.vilkårType in vilkårMistetSidenForrigeBehandling }
            ?: emptyList(),
    )

    val dennePeriodenErFørsteVedtaksperiodePåFagsak =
        begrunnelseGrunnlag.forrigePeriode == null || begrunnelseGrunnlag.forrigePeriode!!.andeler.firstOrNull() == null

    return begrunnelseGjelderMistedeVilkår && dennePeriodenErFørsteVedtaksperiodePåFagsak
}

private fun SanityBegrunnelse.begrunnelseGjelderOpphørFraForrigeBehandling() =
    ØvrigTrigger.GJELDER_FØRSTE_PERIODE in this.ovrigeTriggere || ØvrigTrigger.OPPHØR_FRA_FORRIGE_BEHANDLING in this.ovrigeTriggere

private fun hentEØSStandardBegrunnelser(
    vedtaksperiode: VedtaksperiodeMedBegrunnelser,
    sanityEØSBegrunnelser: Map<EØSStandardbegrunnelse, SanityEØSBegrunnelse>,
    begrunnelseGrunnlag: IBegrunnelseGrunnlagForPeriode,
    relevantePeriodeResultater: List<SanityPeriodeResultat>,
    erUtbetalingEllerDeltBostedIPeriode: Boolean,
): Map<EØSStandardbegrunnelse, SanityEØSBegrunnelse> {
    val begrunnelserFiltrertPåPeriodetype = sanityEØSBegrunnelser.filterValues {
        it.periodeResultat in relevantePeriodeResultater
    }

    val begrunnelserFiltrertPåPerioderesultatOgBrevPeriodeType = begrunnelserFiltrertPåPeriodetype
        .filterValues { it.erGjeldendeForBrevPeriodeType(vedtaksperiode, erUtbetalingEllerDeltBostedIPeriode) }

    val filtrertPåVilkår = begrunnelserFiltrertPåPerioderesultatOgBrevPeriodeType.filterValues {
        it.erGjeldendeForUtgjørendeVilkår(begrunnelseGrunnlag)
    }

    val filtrertPåKompetanse = begrunnelserFiltrertPåPerioderesultatOgBrevPeriodeType.filterValues { begrunnelse ->
        erEndringIKompetanse(begrunnelseGrunnlag) && begrunnelse.erLikKompetanseIPeriode(begrunnelseGrunnlag)
    }

    val filtrertPåPeriodeResultat = begrunnelserFiltrertPåPeriodetype.filterValues {
        filtrerPåPeriodeResultat(relevantePeriodeResultater, it)
    }

    return filtrertPåVilkår + filtrertPåKompetanse + filtrertPåPeriodeResultat
}

private fun filtrerPåPeriodeResultat(
    relevantePeriodeResultater: List<SanityPeriodeResultat>,
    sanityEøsBegrunnelse: SanityEØSBegrunnelse,
): Boolean {
    val periodeResultatErIngenEndring = SanityPeriodeResultat.INGEN_ENDRING in relevantePeriodeResultater
    val periodeResultatPåBegrunnelseErInnvilgetEllerØkning =
        sanityEøsBegrunnelse.periodeResultat == SanityPeriodeResultat.INNVILGET_ELLER_ØKNING

    return periodeResultatErIngenEndring && periodeResultatPåBegrunnelseErInnvilgetEllerØkning
}

fun SanityBegrunnelse.erGjeldendeForRolle(
    person: Person,
    fagsakType: FagsakType,
): Boolean {
    val rolleErRelevantForBegrunnelse = this.rolle.isNotEmpty()

    val begrunnelseGjelderPersonSinRolle =
        person.type in this.rolle.map { it.tilPersonType() } || fagsakType.erBarnSøker()

    return !rolleErRelevantForBegrunnelse || begrunnelseGjelderPersonSinRolle
}

fun SanityEØSBegrunnelse.erLikKompetanseIPeriode(
    begrunnelseGrunnlag: IBegrunnelseGrunnlagForPeriode,
): Boolean {
    val kompetanse = when (this.periodeResultat) {
        SanityPeriodeResultat.INNVILGET_ELLER_ØKNING, SanityPeriodeResultat.INGEN_ENDRING ->
            begrunnelseGrunnlag.dennePerioden.kompetanse
                ?: return false

        SanityPeriodeResultat.IKKE_INNVILGET,
        SanityPeriodeResultat.REDUKSJON,
        -> begrunnelseGrunnlag.forrigePeriode?.kompetanse ?: return false

        null,
        -> return false
    }

    return this.annenForeldersAktivitet.contains(kompetanse.annenForeldersAktivitet) && this.barnetsBostedsland.contains(
        landkodeTilBarnetsBostedsland(kompetanse.barnetsBostedsland),
    ) && this.kompetanseResultat.contains(kompetanse.resultat)
}

fun Map<Standardbegrunnelse, SanityBegrunnelse>.filtrerPåHendelser(
    begrunnelseGrunnlag: IBegrunnelseGrunnlagForPeriode,
    fomVedtaksperiode: LocalDate?,
): Map<Standardbegrunnelse, SanityBegrunnelse> = if (!begrunnelseGrunnlag.dennePerioden.erOrdinæreVilkårInnvilget()) {
    val person = begrunnelseGrunnlag.dennePerioden.person

    this.filtrerPåBarnDød(person, fomVedtaksperiode)
} else {
    val person = begrunnelseGrunnlag.dennePerioden.person

    this.filtrerPåBarn6år(person, fomVedtaksperiode) + this.filtrerPåSatsendring(
        person,
        begrunnelseGrunnlag.dennePerioden.andeler,
        fomVedtaksperiode,
    )
}

fun Map<Standardbegrunnelse, SanityBegrunnelse>.filtrerPåBarn6år(
    person: Person,
    fomVedtaksperiode: LocalDate?,
): Map<Standardbegrunnelse, SanityBegrunnelse> {
    val blirPerson6DennePerioden = person.hentSeksårsdag().toYearMonth() == fomVedtaksperiode?.toYearMonth()

    return if (blirPerson6DennePerioden) {
        this.filterValues { it.ovrigeTriggere.contains(ØvrigTrigger.BARN_MED_6_ÅRS_DAG) }
    } else {
        emptyMap()
    }
}

fun Map<Standardbegrunnelse, SanityBegrunnelse>.filtrerPåBarnDød(
    person: Person,
    fomVedtaksperiode: LocalDate?,
): Map<Standardbegrunnelse, SanityBegrunnelse> {
    val dødsfall = person.dødsfall
    val personDødeForrigeMåned =
        dødsfall != null && dødsfall.dødsfallDato.toYearMonth().plusMonths(1) == fomVedtaksperiode?.toYearMonth()

    return if (personDødeForrigeMåned && person.type == PersonType.BARN) {
        this.filterValues { it.ovrigeTriggere.contains(ØvrigTrigger.BARN_DØD) }
    } else {
        emptyMap()
    }
}

fun Map<Standardbegrunnelse, SanityBegrunnelse>.filtrerPåSatsendring(
    person: Person,
    andeler: Iterable<AndelForVedtaksperiode>,
    fomVedtaksperiode: LocalDate?,
): Map<Standardbegrunnelse, SanityBegrunnelse> {
    val satstyperPåAndelene = andeler.map { it.type.tilSatsType(person, fomVedtaksperiode ?: TIDENES_MORGEN) }.toSet()

    val erSatsendringIPeriodenForPerson = satstyperPåAndelene.any { satstype ->
        SatsService.finnAlleSatserFor(satstype).any { it.gyldigFom == fomVedtaksperiode }
    }

    return if (erSatsendringIPeriodenForPerson) {
        this.filterValues { it.ovrigeTriggere.contains(ØvrigTrigger.SATSENDRING) }
    } else {
        emptyMap()
    }
}

private fun hentResultaterForForrigePeriode(
    begrunnelseGrunnlagForrigePeriode: BegrunnelseGrunnlagForPersonIPeriode?,
) =
    if (begrunnelseGrunnlagForrigePeriode?.erOrdinæreVilkårInnvilget() == true && begrunnelseGrunnlagForrigePeriode.erInnvilgetEtterEndretUtbetaling()) {
        listOf(
            SanityPeriodeResultat.REDUKSJON,
            SanityPeriodeResultat.INNVILGET_ELLER_ØKNING,
        )
    } else {
        listOf(
            SanityPeriodeResultat.REDUKSJON,
            SanityPeriodeResultat.IKKE_INNVILGET,
        )
    }

private fun hentResultaterForPeriode(
    begrunnelseGrunnlagForPeriode: BegrunnelseGrunnlagForPersonIPeriode,
    begrunnelseGrunnlagForrigePeriode: BegrunnelseGrunnlagForPersonIPeriode?,
): List<SanityPeriodeResultat> {
    val erAndelerPåPersonHvisBarn =
        begrunnelseGrunnlagForPeriode.person.type != PersonType.BARN || begrunnelseGrunnlagForPeriode.andeler.toList()
            .isNotEmpty()

    val erInnvilgetEtterVilkårOgEndretUtbetaling =
        begrunnelseGrunnlagForPeriode.erOrdinæreVilkårInnvilget() && begrunnelseGrunnlagForPeriode.erInnvilgetEtterEndretUtbetaling()

    val erReduksjonIAndel = erReduksjonIAndelMellomPerioder(
        begrunnelseGrunnlagForPeriode,
        begrunnelseGrunnlagForrigePeriode,
    )

    return if (erInnvilgetEtterVilkårOgEndretUtbetaling && erAndelerPåPersonHvisBarn) {
        val erØkingIAndel = erØkningIAndelMellomPerioder(
            begrunnelseGrunnlagForPeriode,
            begrunnelseGrunnlagForrigePeriode,
        )

        val erSøker = begrunnelseGrunnlagForPeriode.person.type == PersonType.SØKER
        val erOrdinæreVilkårOppfyltIForrigePeriode =
            begrunnelseGrunnlagForrigePeriode?.erOrdinæreVilkårInnvilget() == true

        val erIngenEndring = !erØkingIAndel && !erReduksjonIAndel && erOrdinæreVilkårOppfyltIForrigePeriode
        listOfNotNull(
            if (erØkingIAndel || erSøker || erIngenEndring) SanityPeriodeResultat.INNVILGET_ELLER_ØKNING else null,
            if (erReduksjonIAndel) SanityPeriodeResultat.REDUKSJON else null,
            if (erIngenEndring) SanityPeriodeResultat.INGEN_ENDRING else null,
        )
    } else {
        listOfNotNull(
            if (erReduksjonIAndel) SanityPeriodeResultat.REDUKSJON else null,
            SanityPeriodeResultat.IKKE_INNVILGET,
        )
    }
}

private fun erReduksjonIAndelMellomPerioder(
    begrunnelseGrunnlagForPeriode: BegrunnelseGrunnlagForPersonIPeriode?,
    begrunnelseGrunnlagForrigePeriode: BegrunnelseGrunnlagForPersonIPeriode?,
): Boolean {
    val andelerForrigePeriode = begrunnelseGrunnlagForrigePeriode?.andeler ?: emptyList()
    val andelerDennePerioden = begrunnelseGrunnlagForPeriode?.andeler ?: emptyList()

    return andelerForrigePeriode.any { andelIForrigePeriode ->
        val sammeAndelDennePerioden = andelerDennePerioden.singleOrNull { andelIForrigePeriode.type == it.type }

        val erAndelenMistet =
            sammeAndelDennePerioden == null && begrunnelseGrunnlagForrigePeriode?.erInnvilgetEtterEndretUtbetaling() == true
        val harAndelenGåttNedIProsent =
            sammeAndelDennePerioden != null && andelIForrigePeriode.prosent > sammeAndelDennePerioden.prosent
        val erSatsenRedusert = andelIForrigePeriode.sats > (sammeAndelDennePerioden?.sats ?: 0)

        erAndelenMistet || harAndelenGåttNedIProsent || erSatsenRedusert
    }
}

private fun erØkningIAndelMellomPerioder(
    begrunnelseGrunnlagForPeriode: BegrunnelseGrunnlagForPersonIPeriode,
    begrunnelseGrunnlagForrigePeriode: BegrunnelseGrunnlagForPersonIPeriode?,
): Boolean {
    val andelerForrigePeriode = begrunnelseGrunnlagForrigePeriode?.andeler ?: emptyList()
    val andelerDennePerioden = begrunnelseGrunnlagForPeriode.andeler

    return andelerDennePerioden.any { andelIPeriode ->
        val sammeAndelForrigePeriode = andelerForrigePeriode.singleOrNull { andelIPeriode.type == it.type }

        val erAndelenTjent =
            sammeAndelForrigePeriode == null && begrunnelseGrunnlagForPeriode.erInnvilgetEtterEndretUtbetaling()
        val harAndelenGåttOppIProsent =
            sammeAndelForrigePeriode != null && andelIPeriode.prosent > sammeAndelForrigePeriode.prosent
        val erSatsenØkt = andelIPeriode.sats > (sammeAndelForrigePeriode?.sats ?: 0)

        erAndelenTjent || harAndelenGåttOppIProsent || erSatsenØkt
    }
}

private fun SanityBegrunnelse.erEtterEndretUtbetaling(
    endretUtbetalingDennePerioden: EndretUtbetalingAndelForVedtaksperiode?,
    endretUtbetalingForrigePeriode: EndretUtbetalingAndelForVedtaksperiode?,
): Boolean {
    if (!this.erEndringsårsakOgGjelderEtterEndretUtbetaling()) return false

    return this.matcherEtterEndretUtbetaling(
        endretUtbetalingDennePerioden = endretUtbetalingDennePerioden,
        endretUtbetalingForrigePeriode = endretUtbetalingForrigePeriode,
    )
}

private fun SanityBegrunnelse.matcherEtterEndretUtbetaling(
    endretUtbetalingDennePerioden: EndretUtbetalingAndelForVedtaksperiode?,
    endretUtbetalingForrigePeriode: EndretUtbetalingAndelForVedtaksperiode?,
): Boolean {
    val begrunnelseMatcherEndretUtbetalingIForrigePeriode =
        this.endringsaarsaker.all { it == endretUtbetalingForrigePeriode?.årsak }

    val begrunnelseMatcherEndretUtbetalingIDennePerioden =
        this.endringsaarsaker.all { it == endretUtbetalingDennePerioden?.årsak }

    if (!begrunnelseMatcherEndretUtbetalingIForrigePeriode || begrunnelseMatcherEndretUtbetalingIDennePerioden) return false

    return endretUtbetalingForrigePeriode?.årsak != Årsak.DELT_BOSTED || this.erDeltBostedUtbetalingstype(
        endretUtbetalingForrigePeriode,
    )
}

private fun SanityBegrunnelse.erEndringsårsakOgGjelderEtterEndretUtbetaling() =
    this.endringsaarsaker.isNotEmpty() && this.gjelderEtterEndretUtbetaling()

private fun SanityBegrunnelse.erEndretUtbetaling(
    endretUtbetaling: EndretUtbetalingAndelForVedtaksperiode?,
): Boolean {
    return this.gjelderEndretUtbetaling() && this.erLikEndretUtbetalingIPeriode(endretUtbetaling)
}

private fun SanityBegrunnelse.gjelderEndretUtbetaling() =
    this.endringsaarsaker.isNotEmpty() && !this.gjelderEtterEndretUtbetaling()

private fun SanityBegrunnelse.erLikEndretUtbetalingIPeriode(
    endretUtbetaling: EndretUtbetalingAndelForVedtaksperiode?,
): Boolean {
    if (endretUtbetaling == null) return false

    val erEndringsårsakerIBegrunnelseOgPeriodeLike = this.endringsaarsaker.all { it == endretUtbetaling.årsak }
    if (!erEndringsårsakerIBegrunnelseOgPeriodeLike) return false

    return if (endretUtbetaling.årsak == Årsak.DELT_BOSTED) {
        this.erDeltBostedUtbetalingstype(endretUtbetaling)
    } else {
        true
    }
}

private fun SanityBegrunnelse.erDeltBostedUtbetalingstype(
    endretUtbetaling: EndretUtbetalingAndelForVedtaksperiode,
): Boolean {
    val inneholderAndelSomSkalUtbetales = endretUtbetaling.prosent != BigDecimal.ZERO

    return when (this.endretUtbetalingsperiodeDeltBostedUtbetalingTrigger) {
        EndretUtbetalingsperiodeDeltBostedTriggere.UTBETALING_IKKE_RELEVANT -> true
        EndretUtbetalingsperiodeDeltBostedTriggere.SKAL_UTBETALES -> inneholderAndelSomSkalUtbetales
        EndretUtbetalingsperiodeDeltBostedTriggere.SKAL_IKKE_UTBETALES -> !inneholderAndelSomSkalUtbetales
        null -> true
    }
}

private fun hentEndretUtbetalingDennePerioden(begrunnelseGrunnlag: IBegrunnelseGrunnlagForPeriode) =
    begrunnelseGrunnlag.dennePerioden.endretUtbetalingAndel.takeIf { begrunnelseGrunnlag.dennePerioden.erOrdinæreVilkårInnvilget() }

private fun hentEndretUtbetalingForrigePeriode(begrunnelseGrunnlag: IBegrunnelseGrunnlagForPeriode) =
    begrunnelseGrunnlag.forrigePeriode?.endretUtbetalingAndel.takeIf { begrunnelseGrunnlag.forrigePeriode?.erOrdinæreVilkårInnvilget() == true }

fun VedtaksperiodeMedBegrunnelser.finnBegrunnelseGrunnlagPerPerson(
    grunnlag: GrunnlagForBegrunnelse,
): Map<Person, IBegrunnelseGrunnlagForPeriode> {
    val tidslinjeMedVedtaksperioden = this.tilTidslinjeForAktuellPeriode()

    val begrunnelsegrunnlagTidslinjerPerPerson =
        grunnlag.behandlingsGrunnlagForVedtaksperioder.lagBegrunnelseGrunnlagTidslinjer()

    val grunnlagTidslinjePerPersonForrigeBehandling =
        grunnlag.behandlingsGrunnlagForVedtaksperioderForrigeBehandling?.lagBegrunnelseGrunnlagTidslinjer()

    return begrunnelsegrunnlagTidslinjerPerPerson.mapValues { (person, grunnlagTidslinje) ->
        val grunnlagMedForrigePeriodeOgBehandlingTidslinje =
            tidslinjeMedVedtaksperioden.lagTidslinjeGrunnlagDennePeriodenForrigePeriodeOgPeriodeForrigeBehandling(
                grunnlagTidslinje,
                grunnlagTidslinjePerPersonForrigeBehandling,
                person,
            )

        val begrunnelseperioderIVedtaksperiode =
            grunnlagMedForrigePeriodeOgBehandlingTidslinje.perioder().mapNotNull { it.innhold }

        when (this.type) {
            Vedtaksperiodetype.OPPHØR -> begrunnelseperioderIVedtaksperiode.first()
            Vedtaksperiodetype.FORTSATT_INNVILGET -> if (this.fom == null && this.tom == null) {
                val perioder = grunnlagMedForrigePeriodeOgBehandlingTidslinje.perioder()
                perioder.single { grunnlag.nåDato.toYearMonth() in it.fraOgMed.tilYearMonthEllerUendeligFortid()..it.tilOgMed.tilYearMonthEllerUendeligFramtid() }.innhold!!
            } else {
                begrunnelseperioderIVedtaksperiode.first()
            }

            else -> begrunnelseperioderIVedtaksperiode.first()
        }
    }
}

private fun Tidslinje<VedtaksperiodeMedBegrunnelser, Måned>.lagTidslinjeGrunnlagDennePeriodenForrigePeriodeOgPeriodeForrigeBehandling(
    grunnlagTidslinje: Tidslinje<BegrunnelseGrunnlagForPersonIPeriode, Måned>,
    grunnlagTidslinjePerPersonForrigeBehandling: Map<Person, Tidslinje<BegrunnelseGrunnlagForPersonIPeriode, Måned>>?,
    person: Person,
): Tidslinje<IBegrunnelseGrunnlagForPeriode, Måned> {
    val grunnlagMedForrigePeriodeTidslinje = grunnlagTidslinje.tilForrigeOgNåværendePeriodeTidslinje(this)

    val grunnlagForrigeBehandlingTidslinje = grunnlagTidslinjePerPersonForrigeBehandling?.get(person) ?: TomTidslinje()

    return this.kombinerMed(
        grunnlagMedForrigePeriodeTidslinje,
        grunnlagForrigeBehandlingTidslinje,
    ) { vedtaksPerioden, forrigeOgDennePerioden, forrigeBehandling ->
        val dennePerioden = forrigeOgDennePerioden?.denne

        if (vedtaksPerioden == null) {
            null
        } else {
            IBegrunnelseGrunnlagForPeriode.opprett(
                dennePerioden = dennePerioden ?: BegrunnelseGrunnlagForPersonIPeriode.tomPeriode(person),
                forrigePeriode = forrigeOgDennePerioden?.forrige,
                sammePeriodeForrigeBehandling = forrigeBehandling,
                periodetype = vedtaksPerioden.type,
            )
        }
    }
}

private fun VedtaksperiodeMedBegrunnelser.tilTidslinjeForAktuellPeriode(): Tidslinje<VedtaksperiodeMedBegrunnelser, Måned> {
    return listOf(
        månedPeriodeAv(
            fraOgMed = this.fom?.toYearMonth(),
            tilOgMed = this.tom?.toYearMonth(),
            innhold = this,
        ),
    ).tilTidslinje()
}

data class ForrigeOgDennePerioden(
    val forrige: BegrunnelseGrunnlagForPersonIPeriode?,
    val denne: BegrunnelseGrunnlagForPersonIPeriode?,
)

private fun Tidslinje<BegrunnelseGrunnlagForPersonIPeriode, Måned>.tilForrigeOgNåværendePeriodeTidslinje(
    vedtaksperiodeTidslinje: Tidslinje<VedtaksperiodeMedBegrunnelser, Måned>,
): Tidslinje<ForrigeOgDennePerioden, Måned> {
    val grunnlagPerioderSplittetPåVedtaksperiode = kombinerMed(vedtaksperiodeTidslinje) { grunnlag, periode ->
        Pair(grunnlag, periode)
    }.perioder().mapInnhold { it?.first }

    return (
        listOf(
            månedPeriodeAv(YearMonth.now(), YearMonth.now(), null),
        ) + grunnlagPerioderSplittetPåVedtaksperiode
        ).zipWithNext { forrige, denne ->
        periodeAv(denne.fraOgMed, denne.tilOgMed, ForrigeOgDennePerioden(forrige.innhold, denne.innhold))
    }.tilTidslinje()
}

private fun SanityBegrunnelse.erGjeldendeForSmåbarnstillegg(
    begrunnelseGrunnlag: IBegrunnelseGrunnlagForPeriode,
): Boolean {
    val erSmåbarnstilleggForrigePeriode =
        begrunnelseGrunnlag.forrigePeriode?.andeler?.any { it.type == YtelseType.SMÅBARNSTILLEGG } == true
    val erSmåbarnstilleggDennePerioden =
        begrunnelseGrunnlag.dennePerioden.andeler.any { it.type == YtelseType.SMÅBARNSTILLEGG }

    val erSmåbarnstilleggIForrigeBehandlingPeriode = begrunnelseGrunnlag.erSmåbarnstilleggIForrigeBehandlingPeriode

    val begrunnelseGjelderSmåbarnstillegg = UtvidetBarnetrygdTrigger.SMÅBARNSTILLEGG in utvidetBarnetrygdTriggere

    val erEndringISmåbarnstilleggFraForrigeBehandling =
        erSmåbarnstilleggIForrigeBehandlingPeriode != erSmåbarnstilleggDennePerioden

    val begrunnelseMatcherPeriodeResultat = this.matcherPerioderesultat(
        erSmåbarnstilleggForrigePeriode,
        erSmåbarnstilleggDennePerioden,
        erSmåbarnstilleggIForrigeBehandlingPeriode,
    )

    val erEndringISmåbarnstillegg = erSmåbarnstilleggForrigePeriode != erSmåbarnstilleggDennePerioden

    return begrunnelseGjelderSmåbarnstillegg && begrunnelseMatcherPeriodeResultat && (erEndringISmåbarnstillegg || erEndringISmåbarnstilleggFraForrigeBehandling)
}

private fun SanityBegrunnelse.matcherPerioderesultat(
    erSmåbarnstilleggForrigePeriode: Boolean,
    erSmåbarnstilleggDennePerioden: Boolean,
    erSmåbarnstilleggIForrigeBehandlingPeriode: Boolean,
): Boolean {
    val erReduksjon =
        !erSmåbarnstilleggDennePerioden && (erSmåbarnstilleggForrigePeriode || erSmåbarnstilleggIForrigeBehandlingPeriode)
    val erØkning =
        erSmåbarnstilleggDennePerioden && (!erSmåbarnstilleggForrigePeriode || !erSmåbarnstilleggIForrigeBehandlingPeriode)

    val erBegrunnelseReduksjon = periodeResultat == SanityPeriodeResultat.REDUKSJON
    val erBegrunnelseØkning = periodeResultat == SanityPeriodeResultat.INNVILGET_ELLER_ØKNING

    val reduksjonMatcher = erReduksjon == erBegrunnelseReduksjon
    val økningMatcher = erØkning == erBegrunnelseØkning
    return reduksjonMatcher && økningMatcher
}

private fun erEndringIKompetanse(begrunnelseGrunnlag: IBegrunnelseGrunnlagForPeriode) =
    begrunnelseGrunnlag.dennePerioden.kompetanse != begrunnelseGrunnlag.forrigePeriode?.kompetanse

fun ISanityBegrunnelse.erGjeldendeForBrevPeriodeType(
    vedtaksperiode: VedtaksperiodeMedBegrunnelser,
    erUtbetalingEllerDeltBostedIPeriode: Boolean,
): Boolean {
    val brevPeriodeType = hentBrevPeriodeType(
        vedtaksperiode.type,
        vedtaksperiode.fom,
        erUtbetalingEllerDeltBostedIPeriode,
    )
    return this.periodeType == brevPeriodeType ||
        (this.periodeType == BrevPeriodeType.FORTSATT_INNVILGET && brevPeriodeType == BrevPeriodeType.FORTSATT_INNVILGET_NY)
}
