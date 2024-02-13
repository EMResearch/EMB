package no.nav.familie.ba.sak.kjerne.tidslinje.util

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.tilPersonEnkelSøkerOgBarn
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseUtils
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.VilkårRegelverkResultat
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.VilkårsvurderingTidslinjer
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilDagEllerFørsteDagIPerioden
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilDagEllerSisteDagIPerioden
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.mapIkkeNull
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering

data class VilkårsvurderingBuilder<T : Tidsenhet>(
    val behandling: Behandling = lagBehandling(),
    private val vilkårsvurdering: Vilkårsvurdering = Vilkårsvurdering(behandling = behandling),
) {
    val personresultater: MutableSet<PersonResultat> = mutableSetOf()
    val personer: MutableSet<Person> = mutableSetOf()

    fun forPerson(person: Person, startTidspunkt: Tidspunkt<T>): PersonResultatBuilder<T> {
        return PersonResultatBuilder(this, startTidspunkt, person)
    }

    fun byggVilkårsvurdering(): Vilkårsvurdering {
        vilkårsvurdering.personResultater = personresultater
        return vilkårsvurdering
    }

    fun byggPersonopplysningGrunnlag(): PersonopplysningGrunnlag {
        return lagTestPersonopplysningGrunnlag(behandling.id, *personer.toTypedArray())
    }

    data class PersonResultatBuilder<T : Tidsenhet>(
        val vilkårsvurderingBuilder: VilkårsvurderingBuilder<T>,
        val startTidspunkt: Tidspunkt<T>,
        private val person: Person = tilfeldigPerson(),
        private val vilkårsresultatTidslinjer: MutableList<Tidslinje<UtdypendeVilkårRegelverkResultat, T>> = mutableListOf(),
    ) {
        fun medVilkår(v: String, vararg vilkår: Vilkår): PersonResultatBuilder<T> {
            vilkårsresultatTidslinjer.addAll(
                vilkår.map { v.tilUtdypendeVilkårRegelverkResultatTidslinje(it, startTidspunkt) },
            )
            return this
        }

        fun medVilkår(tidslinje: Tidslinje<VilkårRegelverkResultat, T>): PersonResultatBuilder<T> {
            vilkårsresultatTidslinjer.add(
                tidslinje.mapIkkeNull { UtdypendeVilkårRegelverkResultat(it.vilkår, it.resultat, it.regelverk) },
            )
            return this
        }

        fun medUtdypendeVilkår(tidslinje: Tidslinje<UtdypendeVilkårRegelverkResultat, T>): PersonResultatBuilder<T> {
            vilkårsresultatTidslinjer.add(tidslinje)
            return this
        }

        fun forPerson(person: Person, startTidspunkt: Tidspunkt<T>): PersonResultatBuilder<T> {
            return byggPerson().forPerson(person, startTidspunkt)
        }

        fun byggVilkårsvurdering(): Vilkårsvurdering = byggPerson().byggVilkårsvurdering()
        fun byggPersonopplysningGrunnlag(): PersonopplysningGrunnlag = byggPerson().byggPersonopplysningGrunnlag()

        fun byggPerson(): VilkårsvurderingBuilder<T> {
            val personResultat = PersonResultat(
                vilkårsvurdering = vilkårsvurderingBuilder.vilkårsvurdering,
                aktør = person.aktør,
            )

            val vilkårresultater = vilkårsresultatTidslinjer.flatMap {
                it.perioder()
                    .filter { it.innhold != null }
                    .flatMap { periode -> periode.tilVilkårResultater(personResultat) }
            }

            personResultat.vilkårResultater.addAll(vilkårresultater)
            vilkårsvurderingBuilder.personresultater.add(personResultat)
            vilkårsvurderingBuilder.personer.add(person)

            return vilkårsvurderingBuilder
        }
    }
}

internal fun <T : Tidsenhet> Periode<UtdypendeVilkårRegelverkResultat, T>.tilVilkårResultater(personResultat: PersonResultat): Collection<VilkårResultat> {
    return listOf(
        VilkårResultat(
            personResultat = personResultat,
            vilkårType = this.innhold?.vilkår!!,
            resultat = this.innhold?.resultat!!,
            vurderesEtter = this.innhold?.regelverk,
            periodeFom = this.fraOgMed.tilDagEllerFørsteDagIPerioden().tilLocalDateEllerNull(),
            periodeTom = this.tilOgMed.tilDagEllerSisteDagIPerioden().tilLocalDateEllerNull(),
            begrunnelse = "En begrunnelse",
            sistEndretIBehandlingId = personResultat.vilkårsvurdering.behandling.id,
            utdypendeVilkårsvurderinger = this.innhold?.utdypendeVilkårsvurderinger ?: emptyList(),
        ),
    )
}

fun <T : Tidsenhet> VilkårsvurderingBuilder<T>.byggVilkårsvurderingTidslinjer() =
    VilkårsvurderingTidslinjer(this.byggVilkårsvurdering(), this.byggPersonopplysningGrunnlag().tilPersonEnkelSøkerOgBarn())

fun <T : Tidsenhet> VilkårsvurderingBuilder.PersonResultatBuilder<T>.byggVilkårsvurderingTidslinjer() =
    this.byggPerson().byggVilkårsvurderingTidslinjer()

fun <T : Tidsenhet> VilkårsvurderingBuilder<T>.byggTilkjentYtelse() =
    TilkjentYtelseUtils.beregnTilkjentYtelse(
        vilkårsvurdering = this.byggVilkårsvurdering(),
        personopplysningGrunnlag = this.byggPersonopplysningGrunnlag(),
        fagsakType = FagsakType.NORMAL,

    )

data class UtdypendeVilkårRegelverkResultat(
    val vilkår: Vilkår,
    val resultat: Resultat?,
    val regelverk: Regelverk?,
    val utdypendeVilkårsvurderinger: List<UtdypendeVilkårsvurdering> = emptyList(),
) {
    constructor(
        vilkår: Vilkår,
        resultat: Resultat?,
        regelverk: Regelverk?,
        vararg utdypendeVilkårsvurdering: UtdypendeVilkårsvurdering,
    ) : this(vilkår, resultat, regelverk, utdypendeVilkårsvurdering.toList())
}
