package no.nav.familie.ba.sak.kjerne.eøs.util

import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Dødsfall
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Dag
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.DagTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Uendelighet
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilLocalDate
import no.nav.familie.ba.sak.kjerne.tidslinje.tidsrom.TidspunktClosedRange
import no.nav.familie.ba.sak.kjerne.tidslinje.tidsrom.rangeTo
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.mapIkkeNull
import no.nav.familie.ba.sak.kjerne.tidslinje.util.UtdypendeVilkårRegelverkResultat
import no.nav.familie.ba.sak.kjerne.tidslinje.util.VilkårsvurderingBuilder
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import java.time.LocalDate

val barn get() = PersonType.BARN
val søker get() = PersonType.SØKER
infix fun PersonType.født(tidspunkt: Tidspunkt<Dag>) =
    tilfeldigPerson(personType = this, fødselsdato = tidspunkt.tilLocalDate())

internal infix fun Person.død(tidspunkt: Tidspunkt<Dag>) = this.copy(
    dødsfall = Dødsfall(
        person = this,
        dødsfallDato = tidspunkt.tilLocalDate(),
        dødsfallAdresse = null,
        dødsfallPostnummer = null,
        dødsfallPoststed = null,
    ),
)

internal val uendelig: Tidspunkt<Dag> = DagTidspunkt(LocalDate.now(), Uendelighet.FREMTID)
internal fun Person.under18år() = DagTidspunkt.med(this.fødselsdato)
    .rangeTo(DagTidspunkt.med(this.fødselsdato.plusYears(18).minusDays(1)))

val vilkårsvurdering get() = VilkårsvurderingBuilder<Dag>()
infix fun Vilkår.og(vilkår: Vilkår) = listOf(this, vilkår)
infix fun List<Vilkår>.og(vilkår: Vilkår) = this + vilkår
infix fun <T : Tidsenhet> Vilkår.i(tidsrom: TidspunktClosedRange<T>) = oppfyltUtdypendeVilkår(this, null) i tidsrom
infix fun <T : Tidsenhet> UtdypendeVilkårRegelverkResultat.i(tidsrom: TidspunktClosedRange<T>) =
    tidsrom.tilTidslinje { this }

infix fun <T : Tidsenhet> List<Vilkår>.oppfylt(tidsrom: TidspunktClosedRange<T>) = this.map {
    oppfyltUtdypendeVilkår(it, null) i tidsrom
}

infix fun <T : Tidsenhet> Vilkår.oppfylt(tidsrom: TidspunktClosedRange<T>) =
    oppfyltUtdypendeVilkår(this, null) i tidsrom

infix fun <T : Tidsenhet> Tidslinje<UtdypendeVilkårRegelverkResultat, T>.etter(regelverk: Regelverk) =
    this.mapIkkeNull { it.copy(regelverk = regelverk) }

infix fun <T : Tidsenhet> Tidslinje<UtdypendeVilkårRegelverkResultat, T>.med(utdypendeVilkår: UtdypendeVilkårsvurdering) =
    this.mapIkkeNull { it.copy(utdypendeVilkårsvurderinger = it.utdypendeVilkårsvurderinger + utdypendeVilkår) }

infix fun VilkårsvurderingBuilder<Dag>.der(person: Person) = this.forPerson(person, DagTidspunkt.nå())
infix fun VilkårsvurderingBuilder.PersonResultatBuilder<Dag>.har(vilkår: Tidslinje<UtdypendeVilkårRegelverkResultat, Dag>) =
    this.medUtdypendeVilkår(vilkår)

infix fun VilkårsvurderingBuilder.PersonResultatBuilder<Dag>.har(vilkår: Iterable<Tidslinje<UtdypendeVilkårRegelverkResultat, Dag>>) =
    vilkår.map { this.medUtdypendeVilkår(it) }.last()

infix fun VilkårsvurderingBuilder.PersonResultatBuilder<Dag>.og(vilkår: Tidslinje<UtdypendeVilkårRegelverkResultat, Dag>) =
    har(vilkår)

infix fun VilkårsvurderingBuilder.PersonResultatBuilder<Dag>.og(vilkår: Iterable<Tidslinje<UtdypendeVilkårRegelverkResultat, Dag>>) =
    har(vilkår)

infix fun VilkårsvurderingBuilder.PersonResultatBuilder<Dag>.der(person: Person) =
    this.forPerson(person, DagTidspunkt.nå())

fun oppfyltUtdypendeVilkår(vilkår: Vilkår, regelverk: Regelverk? = null) =
    UtdypendeVilkårRegelverkResultat(
        vilkår = vilkår,
        resultat = Resultat.OPPFYLT,
        regelverk = regelverk,
    )
