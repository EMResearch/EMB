package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.common.tilMånedÅr
import no.nav.familie.ba.sak.ekstern.restDomene.BarnMedOpplysninger
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.InternPeriodeOvergangsstønad
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.beregning.domene.tilTidslinjerPerAktørOgType
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.IUtfyltEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.tilIEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.tilTidslinje
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.UtfyltKompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.tilIKompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.tilTidslinje
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrerIkkeNull
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombiner
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMedDatert
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMedNullable
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerUtenNull
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.slåSammenLike
import no.nav.familie.ba.sak.kjerne.tidslinje.månedPeriodeAv
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilMånedTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilYearMonthEllerNull
import no.nav.familie.ba.sak.kjerne.tidslinje.tilTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.map
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.mapIkkeNull
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.alleOrdinæreVilkårErOppfylt
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.tilForskjøvedeVilkårTidslinjer
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingForskyvningUtils.tilTidslinjeForSplittForPerson
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat

typealias AktørId = String

data class GrunnlagForPersonTidslinjerSplittetPåOverlappendeGenerelleAvslag(
    val overlappendeGenerelleAvslagVedtaksperiodeGrunnlagForPerson: Tidslinje<VedtaksperiodeGrunnlagForPerson, Måned>,
    val vedtaksperiodeGrunnlagForPerson: Tidslinje<VedtaksperiodeGrunnlagForPerson, Måned>,
)

data class AktørOgRolleBegrunnelseGrunnlag(
    val aktør: Aktør,
    val rolleBegrunnelseGrunnlag: PersonType,
)

data class BehandlingsGrunnlagForVedtaksperioder(
    val persongrunnlag: PersonopplysningGrunnlag,
    val personResultater: Set<PersonResultat>,
    val fagsakType: FagsakType,
    val kompetanser: List<Kompetanse>,
    val endredeUtbetalinger: List<EndretUtbetalingAndel>,
    val andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    val perioderOvergangsstønad: List<InternPeriodeOvergangsstønad>,
    val uregistrerteBarn: List<BarnMedOpplysninger>,
) {
    val utfylteEndredeUtbetalinger = endredeUtbetalinger
        .map { it.tilIEndretUtbetalingAndel() }
        .filterIsInstance<IUtfyltEndretUtbetalingAndel>()

    val utfylteKompetanser = kompetanser
        .map { it.tilIKompetanse() }
        .filterIsInstance<UtfyltKompetanse>()

    fun utledGrunnlagTidslinjePerPerson(): Map<AktørOgRolleBegrunnelseGrunnlag, GrunnlagForPersonTidslinjerSplittetPåOverlappendeGenerelleAvslag> {
        val søker = persongrunnlag.søker
        val ordinæreVilkårForSøkerForskjøvetTidslinje =
            hentOrdinæreVilkårForSøkerForskjøvetTidslinje(søker, personResultater)

        val erMinstEttBarnMedUtbetalingTidslinje =
            hentErMinstEttBarnMedUtbetalingTidslinje(personResultater, fagsakType, persongrunnlag)

        val erUtbetalingSmåbarnstilleggTidslinje = this.andelerTilkjentYtelse.hentErUtbetalingSmåbarnstilleggTidslinje()

        val personresultaterOgRolleForVilkår = if (fagsakType.erBarnSøker()) {
            personResultater.single().splittOppVilkårForBarnOgSøkerRolle()
        } else {
            personResultater.map {
                Pair(persongrunnlag.personer.single { person -> it.aktør == person.aktør }.type, it)
            }
        }

        val bareSøkerOgUregistrertBarn = uregistrerteBarn.isNotEmpty() && personResultater.size == 1

        val grunnlagForPersonTidslinjer = personresultaterOgRolleForVilkår.associate { (vilkårRolle, personResultat) ->
            val aktør = personResultat.aktør
            val person = persongrunnlag.personer.single { person -> aktør == person.aktør }

            val (overlappendeGenerelleAvslag, vilkårResultaterUtenGenerelleAvslag) = splittOppPåErOverlappendeGenerelleAvslag(
                personResultat,
            )

            val forskjøvedeVilkårResultaterForPersonsAndeler: Tidslinje<List<VilkårResultat>, Måned> =
                vilkårResultaterUtenGenerelleAvslag.hentForskjøvedeVilkårResultaterForPersonsAndelerTidslinje(
                    person = person,
                    erMinstEttBarnMedUtbetalingTidslinje = erMinstEttBarnMedUtbetalingTidslinje,
                    ordinæreVilkårForSøkerTidslinje = ordinæreVilkårForSøkerForskjøvetTidslinje,
                    fagsakType = fagsakType,
                    vilkårRolle = vilkårRolle,
                    bareSøkerOgUregistrertBarn = bareSøkerOgUregistrertBarn,
                )

            AktørOgRolleBegrunnelseGrunnlag(aktør, vilkårRolle) to
                GrunnlagForPersonTidslinjerSplittetPåOverlappendeGenerelleAvslag(
                    overlappendeGenerelleAvslagVedtaksperiodeGrunnlagForPerson = overlappendeGenerelleAvslag.generelleAvslagTilGrunnlagForPersonTidslinje(
                        person,
                    ),
                    vedtaksperiodeGrunnlagForPerson = forskjøvedeVilkårResultaterForPersonsAndeler.tilGrunnlagForPersonTidslinje(
                        person = person,
                        søker = søker,
                        erUtbetalingSmåbarnstilleggTidslinje = erUtbetalingSmåbarnstilleggTidslinje,
                        vilkårRolle = vilkårRolle,
                    ),
                )
        }

        return grunnlagForPersonTidslinjer
    }

    private fun PersonResultat.splittOppVilkårForBarnOgSøkerRolle(): List<Pair<PersonType, PersonResultat>> {
        val personResultaterVilkårForSøker = hentDelAvPersonResultatForRolle(rolle = PersonType.SØKER)

        val personResultaterVilkårForBarn = hentDelAvPersonResultatForRolle(rolle = PersonType.BARN)

        return listOf(
            Pair(PersonType.SØKER, personResultaterVilkårForSøker),
            Pair(PersonType.BARN, personResultaterVilkårForBarn),
        )
    }

    private fun PersonResultat.hentDelAvPersonResultatForRolle(
        rolle: PersonType,
    ): PersonResultat {
        val personResultaterVilkårForSøker = this.kopierMedParent(this.vilkårsvurdering, true)
        personResultaterVilkårForSøker.setSortedVilkårResultater(
            personResultaterVilkårForSøker.vilkårResultater
                .filter { it.vilkårType.gjelder(rolle) }.toSet(),
        )
        return personResultaterVilkårForSøker
    }

    private fun Vilkår.gjelder(persontype: PersonType) = when (this) {
        Vilkår.UNDER_18_ÅR -> listOf(PersonType.BARN).contains(persontype)
        Vilkår.BOR_MED_SØKER -> listOf(PersonType.BARN).contains(persontype)
        Vilkår.GIFT_PARTNERSKAP -> listOf(PersonType.BARN).contains(persontype)
        Vilkår.BOSATT_I_RIKET -> listOf(PersonType.BARN, PersonType.SØKER).contains(persontype)
        Vilkår.LOVLIG_OPPHOLD -> listOf(PersonType.BARN, PersonType.SØKER).contains(persontype)
        Vilkår.UTVIDET_BARNETRYGD -> listOf(PersonType.SØKER).contains(persontype)
    }

    private fun List<VilkårResultat>.generelleAvslagTilGrunnlagForPersonTidslinje(
        person: Person,
    ): Tidslinje<VedtaksperiodeGrunnlagForPerson, Måned> = this
        .map {
            listOf(månedPeriodeAv(null, null, it))
                .tilTidslinje()
        }
        .kombinerUtenNull { it.toList() }
        .map { vilkårResultater ->
            vilkårResultater?.let {
                VedtaksperiodeGrunnlagForPersonVilkårIkkeInnvilget(
                    person = person,
                    vilkårResultaterForVedtaksperiode = it.map { vilkårResultat ->
                        VilkårResultatForVedtaksperiode(
                            vilkårResultat,
                        )
                    },
                )
            }
        }

    private fun Tidslinje<List<VilkårResultat>, Måned>.tilGrunnlagForPersonTidslinje(
        person: Person,
        søker: Person,
        erUtbetalingSmåbarnstilleggTidslinje: Tidslinje<Boolean, Måned>,
        vilkårRolle: PersonType,
    ): Tidslinje<VedtaksperiodeGrunnlagForPerson, Måned> {
        val harRettPåUtbetalingTidslinje = this.tilHarRettPåUtbetalingTidslinje(
            person = person,
            søker = søker,
            vilkårRolle = vilkårRolle,
        )

        val kompetanseTidslinje = utfylteKompetanser.filtrerPåAktør(person.aktør)
            .tilTidslinje().mapIkkeNull { KompetanseForVedtaksperiode(it) }

        val endredeUtbetalingerTidslinje = utfylteEndredeUtbetalinger.filtrerPåAktør(person.aktør)
            .tilTidslinje().mapIkkeNull { EndretUtbetalingAndelForVedtaksperiode(it) }

        val overgangsstønadTidslinje =
            perioderOvergangsstønad.filtrerPåAktør(person.aktør)
                .tilPeriodeOvergangsstønadForVedtaksperiodeTidslinje(erUtbetalingSmåbarnstilleggTidslinje)

        val grunnlagTidslinje = harRettPåUtbetalingTidslinje
            .kombinerMedDatert(
                this.tilVilkårResultaterForVedtaksPeriodeTidslinje(),
                andelerTilkjentYtelse.filtrerPåAktør(person.aktør).tilAndelerForVedtaksPeriodeTidslinje(),
            ) { personHarRettPåUtbetalingIPeriode, vilkårResultater, andeler, tidspunkt ->
                lagGrunnlagForVilkårOgAndel(
                    personHarRettPåUtbetalingIPeriode = personHarRettPåUtbetalingIPeriode,
                    vilkårResultater = vilkårResultater,
                    person = person,
                    andeler = andeler,
                    tidspunkt,
                )
            }.kombinerMedNullable(kompetanseTidslinje) { grunnlagForPerson, kompetanse ->
                lagGrunnlagMedKompetanse(grunnlagForPerson, kompetanse)
            }.kombinerMedNullable(endredeUtbetalingerTidslinje) { grunnlagForPerson, endretUtbetalingAndel ->
                lagGrunnlagMedEndretUtbetalingAndel(grunnlagForPerson, endretUtbetalingAndel)
            }.kombinerMedNullable(overgangsstønadTidslinje) { grunnlagForPerson, overgangsstønad ->
                lagGrunnlagMedOvergangsstønad(grunnlagForPerson, overgangsstønad)
            }.filtrerIkkeNull()

        return grunnlagTidslinje
            .slåSammenLike()
            .perioder()
            .dropWhile { !it.erInnvilgetEllerEksplisittAvslag() }
            .tilTidslinje()
    }
}

private fun splittOppPåErOverlappendeGenerelleAvslag(personResultat: PersonResultat): Pair<List<VilkårResultat>, List<VilkårResultat>> {
    val overlappendeGenerelleAvslag =
        personResultat.vilkårResultater.groupBy { it.vilkårType }.mapNotNull { (_, resultat) ->
            if (resultat.size > 1) {
                resultat.filter { it.erGenereltAvslag() }
            } else {
                null
            }
        }.flatten()

    val vilkårResultaterUtenGenerelleAvslag =
        personResultat.vilkårResultater.filterNot { overlappendeGenerelleAvslag.contains(it) }
    return Pair(overlappendeGenerelleAvslag, vilkårResultaterUtenGenerelleAvslag)
}

private fun List<VilkårResultat>.filtrerVilkårErOrdinærtFor(
    søker: Person,
): List<VilkårResultat>? {
    val ordinæreVilkårForSøker = Vilkår.hentOrdinæreVilkårFor(søker.type)

    return this
        .filter { ordinæreVilkårForSøker.contains(it.vilkårType) }
        .takeIf { it.isNotEmpty() }
}

fun hentOrdinæreVilkårForSøkerForskjøvetTidslinje(
    søker: Person,
    personResultater: Set<PersonResultat>,
): Tidslinje<List<VilkårResultat>, Måned> {
    val søkerPersonResultater = personResultater.single { it.aktør == søker.aktør }

    val (_, vilkårResultaterUtenOverlappendeGenerelleAvslag) = splittOppPåErOverlappendeGenerelleAvslag(
        søkerPersonResultater,
    )

    return vilkårResultaterUtenOverlappendeGenerelleAvslag
        .tilForskjøvedeVilkårTidslinjer(søker.fødselsdato)
        .kombiner { vilkårResultater -> vilkårResultater.toList().takeIf { it.isNotEmpty() } }
        .map { it?.toList()?.filtrerVilkårErOrdinærtFor(søker) }
}

fun VilkårResultat.erGenereltAvslag() =
    periodeFom == null && periodeTom == null && erEksplisittAvslagPåSøknad == true

private fun hentErMinstEttBarnMedUtbetalingTidslinje(
    personResultater: Set<PersonResultat>,
    fagsakType: FagsakType,
    persongrunnlag: PersonopplysningGrunnlag,
): Tidslinje<Boolean, Måned> {
    val søker = persongrunnlag.søker
    val søkerSinerOrdinæreVilkårErOppfyltTidslinje =
        personResultater.single { it.aktør == søker.aktør }.tilTidslinjeForSplittForPerson(
            person = søker,
            fagsakType = fagsakType,
        ).map { it != null }

    val barnSineVilkårErOppfyltTidslinjer = personResultater
        .filter { it.aktør != søker.aktør || søker.type == PersonType.BARN }
        .map { personResultat ->
            personResultat.tilTidslinjeForSplittForPerson(
                person = persongrunnlag.barna.single { it.aktør == personResultat.aktør },
                fagsakType = fagsakType,
            ).map { it != null }
        }

    return barnSineVilkårErOppfyltTidslinjer
        .map {
            it.kombinerMed(søkerSinerOrdinæreVilkårErOppfyltTidslinje) { barnetHarAlleOrdinæreVilkårOppfylt, søkerHarAlleOrdinæreVilkårOppfylt ->
                barnetHarAlleOrdinæreVilkårOppfylt == true && søkerHarAlleOrdinæreVilkårOppfylt == true
            }
        }
        .kombiner { erOrdinæreVilkårOppfyltForSøkerOgBarn ->
            erOrdinæreVilkårOppfyltForSøkerOgBarn.any { it }
        }
}

private fun List<VilkårResultat>.hentForskjøvedeVilkårResultaterForPersonsAndelerTidslinje(
    person: Person,
    erMinstEttBarnMedUtbetalingTidslinje: Tidslinje<Boolean, Måned>,
    ordinæreVilkårForSøkerTidslinje: Tidslinje<List<VilkårResultat>, Måned>,
    fagsakType: FagsakType,
    vilkårRolle: PersonType,
    bareSøkerOgUregistrertBarn: Boolean,
): Tidslinje<List<VilkårResultat>, Måned> {
    val forskjøvedeVilkårResultaterForPerson = this.tilForskjøvedeVilkårTidslinjer(person.fødselsdato).kombiner { it }

    return when (vilkårRolle) {
        PersonType.SØKER -> forskjøvedeVilkårResultaterForPerson.map { vilkårResultater ->
            if (bareSøkerOgUregistrertBarn) {
                vilkårResultater?.toList()?.takeIf { it.isNotEmpty() }
            } else {
                vilkårResultater?.filtrerErIkkeOrdinærtFor(vilkårRolle)?.takeIf { it.isNotEmpty() }
            }
        }.kombinerMed(erMinstEttBarnMedUtbetalingTidslinje) { vilkårResultaterForSøker, erMinstEttBarnMedUtbetaling ->
            vilkårResultaterForSøker?.takeIf { erMinstEttBarnMedUtbetaling == true || vilkårResultaterForSøker.any { it.erEksplisittAvslagPåSøknad == true } }
        }

        PersonType.BARN -> if (fagsakType == FagsakType.BARN_ENSLIG_MINDREÅRIG || fagsakType == FagsakType.INSTITUSJON) {
            forskjøvedeVilkårResultaterForPerson.map { it?.toList() }
        } else {
            forskjøvedeVilkårResultaterForPerson
                .kombinerMed(ordinæreVilkårForSøkerTidslinje) { vilkårResultaterBarn, vilkårResultaterSøker ->
                    slåSammenHvisMulig(vilkårResultaterBarn, vilkårResultaterSøker)?.toList()
                }
        }

        PersonType.ANNENPART -> throw Feil("Ikke implementert for annenpart")
    }
}

private fun slåSammenHvisMulig(
    venstre: Iterable<VilkårResultat>?,
    høyre: Iterable<VilkårResultat>?,
) = when {
    venstre == null -> høyre
    høyre == null -> venstre
    else -> høyre + venstre
}

private fun Iterable<VilkårResultat>.filtrerErIkkeOrdinærtFor(persontype: PersonType): List<VilkårResultat> {
    val ordinæreVilkårForPerson = Vilkår.hentOrdinæreVilkårFor(persontype)

    return this.filterNot { ordinæreVilkårForPerson.contains(it.vilkårType) }
}

private fun lagGrunnlagForVilkårOgAndel(
    personHarRettPåUtbetalingIPeriode: Boolean?,
    vilkårResultater: List<VilkårResultatForVedtaksperiode>?,
    person: Person,
    andeler: Iterable<AndelForVedtaksperiode>?,
    måned: Tidspunkt<Måned>,
) = if (personHarRettPåUtbetalingIPeriode == true) {
    if (andeler == null) {
        secureLogger.info(
            "Andeler må finnes for innvilgede vedtaksperioder, men det var ikke andeler i ${
                måned.tilYearMonthEllerNull()?.tilMånedÅr() ?: "uendelig ${måned.uendelighet}"
            } for $person",
        )
    }

    VedtaksperiodeGrunnlagForPersonVilkårInnvilget(
        vilkårResultaterForVedtaksperiode = vilkårResultater
            ?: error("vilkårResultatene burde alltid finnes om vi har innvilget vedtaksperiode."),
        person = person,
        andeler = andeler
            ?: error(
                "Andeler må finnes for innvilgede vedtaksperioder, men det var ikke andeler i ${
                    måned.tilYearMonthEllerNull()?.tilMånedÅr() ?: "uendelig ${måned.uendelighet}"
                }",
            ),
    )
} else {
    VedtaksperiodeGrunnlagForPersonVilkårIkkeInnvilget(
        vilkårResultaterForVedtaksperiode = vilkårResultater ?: emptyList(),
        person = person,
    )
}

private fun lagGrunnlagMedKompetanse(
    vedtaksperiodeGrunnlagForPerson: VedtaksperiodeGrunnlagForPerson?,
    kompetanse: KompetanseForVedtaksperiode?,
) = when (vedtaksperiodeGrunnlagForPerson) {
    is VedtaksperiodeGrunnlagForPersonVilkårInnvilget -> vedtaksperiodeGrunnlagForPerson.copy(kompetanse = kompetanse)
    is VedtaksperiodeGrunnlagForPersonVilkårIkkeInnvilget -> vedtaksperiodeGrunnlagForPerson
    null -> null
}

private fun lagGrunnlagMedEndretUtbetalingAndel(
    vedtaksperiodeGrunnlagForPerson: VedtaksperiodeGrunnlagForPerson?,
    endretUtbetalingAndel: EndretUtbetalingAndelForVedtaksperiode?,
) = when (vedtaksperiodeGrunnlagForPerson) {
    is VedtaksperiodeGrunnlagForPersonVilkårInnvilget -> vedtaksperiodeGrunnlagForPerson.copy(endretUtbetalingAndel = endretUtbetalingAndel)
    is VedtaksperiodeGrunnlagForPersonVilkårIkkeInnvilget -> vedtaksperiodeGrunnlagForPerson
    null -> null
}

private fun lagGrunnlagMedOvergangsstønad(
    vedtaksperiodeGrunnlagForPerson: VedtaksperiodeGrunnlagForPerson?,
    overgangsstønad: OvergangsstønadForVedtaksperiode?,
) = when (vedtaksperiodeGrunnlagForPerson) {
    is VedtaksperiodeGrunnlagForPersonVilkårInnvilget -> vedtaksperiodeGrunnlagForPerson.copy(overgangsstønad = overgangsstønad)
    is VedtaksperiodeGrunnlagForPersonVilkårIkkeInnvilget -> vedtaksperiodeGrunnlagForPerson
    null -> null
}

// TODO: Kan dette erstattes ved å se på hvorvidt det er andeler eller ikke i stedet?
private fun Tidslinje<List<VilkårResultat>, Måned>.tilHarRettPåUtbetalingTidslinje(
    person: Person,
    søker: Person,
    vilkårRolle: PersonType,
): Tidslinje<Boolean, Måned> = this.map { vilkårResultater ->
    if (vilkårResultater.isNullOrEmpty()) {
        null
    } else {
        when (vilkårRolle) {
            PersonType.SØKER -> vilkårResultater.filtrerPåAktør(søker.aktør).all { it.erOppfylt() }

            PersonType.BARN -> {
                val barnSineVilkårErOppfylt = vilkårResultater.filtrerPåAktør(person.aktør)
                    .alleOrdinæreVilkårErOppfylt(
                        PersonType.BARN,
                        FagsakType.NORMAL,
                    )
                val søkerSineVilkårErOppfylt = vilkårResultater.filtrerPåAktør(søker.aktør)
                    .alleOrdinæreVilkårErOppfylt(
                        PersonType.SØKER,
                        FagsakType.NORMAL,
                    )

                barnSineVilkårErOppfylt && søkerSineVilkårErOppfylt
            }

            PersonType.ANNENPART -> throw Feil("Ikke implementert for annenpart")
        }
    }
}

fun List<AndelTilkjentYtelse>.tilAndelerForVedtaksPeriodeTidslinje(): Tidslinje<Iterable<AndelForVedtaksperiode>, Måned> =
    this.tilTidslinjerPerAktørOgType()
        .values
        .map { tidslinje -> tidslinje.mapIkkeNull { it }.slåSammenLike() }
        .kombiner { it }

// Vi trenger dette for å kunne begrunne nye perioder med småbarnstillegg som vi ikke hadde i forrige behandling
fun List<InternPeriodeOvergangsstønad>.tilPeriodeOvergangsstønadForVedtaksperiodeTidslinje(
    erUtbetalingSmåbarnstilleggTidslinje: Tidslinje<Boolean, Måned>,
) = this
    .map { OvergangsstønadForVedtaksperiode(it) }
    .map { Periode(it.fom.tilMånedTidspunkt(), it.tom.tilMånedTidspunkt(), it) }
    .tilTidslinje()
    .kombinerMed(erUtbetalingSmåbarnstilleggTidslinje) { overgangsstønad, erUtbetalingSmåbarnstillegg ->
        overgangsstønad.takeIf { erUtbetalingSmåbarnstillegg == true }
    }

private fun Tidslinje<List<VilkårResultat>, Måned>.tilVilkårResultaterForVedtaksPeriodeTidslinje() =
    this.map { vilkårResultater -> vilkårResultater?.map { VilkårResultatForVedtaksperiode(it) } }

@JvmName("internPeriodeOvergangsstønaderFiltrerPåAktør")
fun List<InternPeriodeOvergangsstønad>.filtrerPåAktør(aktør: Aktør) =
    this.filter { it.personIdent == aktør.aktivFødselsnummer() }

@JvmName("andelerTilkjentYtelserFiltrerPåAktør")
fun List<AndelTilkjentYtelse>.filtrerPåAktør(aktør: Aktør) =
    this.filter { andelTilkjentYtelse -> andelTilkjentYtelse.aktør == aktør }

@JvmName("endredeUtbetalingerFiltrerPåAktør")
fun List<IUtfyltEndretUtbetalingAndel>.filtrerPåAktør(aktør: Aktør) =
    this.filter { endretUtbetaling -> endretUtbetaling.person.aktør == aktør }

@JvmName("utfyltKompetanseFiltrerPåAktør")
fun List<UtfyltKompetanse>.filtrerPåAktør(aktør: Aktør) =
    this.filter { it.barnAktører.contains(aktør) }

@JvmName("vilkårResultatFiltrerPåAktør")
fun List<VilkårResultat>.filtrerPåAktør(aktør: Aktør) =
    filter { it.personResultat?.aktør == aktør }

private fun Periode<VedtaksperiodeGrunnlagForPerson, Måned>.erInnvilgetEllerEksplisittAvslag(): Boolean {
    val grunnlagForPerson = innhold ?: return false

    val erInnvilget = grunnlagForPerson is VedtaksperiodeGrunnlagForPersonVilkårInnvilget
    val erEksplisittAvslag =
        grunnlagForPerson.vilkårResultaterForVedtaksperiode.any { it.erEksplisittAvslagPåSøknad == true }

    return erInnvilget || erEksplisittAvslag
}

private fun List<AndelTilkjentYtelse>.hentErUtbetalingSmåbarnstilleggTidslinje(): Tidslinje<Boolean, Måned> {
    return tilAndelerForVedtaksPeriodeTidslinje().hentErUtbetalingSmåbarnstilleggTidslinje()
}

fun Tidslinje<Iterable<AndelForVedtaksperiode>, Måned>.hentErUtbetalingSmåbarnstilleggTidslinje() =
    this.mapIkkeNull { andelerIPeriode ->
        andelerIPeriode.any {
            it.type == YtelseType.SMÅBARNSTILLEGG && it.kalkulertUtbetalingsbeløp > 0
        }
    }
