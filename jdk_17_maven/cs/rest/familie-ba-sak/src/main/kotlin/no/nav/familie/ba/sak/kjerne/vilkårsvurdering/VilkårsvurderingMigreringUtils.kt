package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import no.nav.familie.ba.sak.common.til18ÅrsVilkårsdato
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import java.time.LocalDate

object VilkårsvurderingMigreringUtils {

    fun utledPeriodeFom(
        oppfylteVilkårResultaterForType: List<VilkårResultat>,
        vilkår: Vilkår,
        person: Person,
        nyMigreringsdato: LocalDate,
    ): LocalDate {
        val forrigeVilkårsPeriodeFom =
            oppfylteVilkårResultaterForType.minWithOrNull(VilkårResultat.VilkårResultatComparator)?.periodeFom
        return when {
            person.fødselsdato.isAfter(nyMigreringsdato) ||
                vilkår.gjelderAlltidFraBarnetsFødselsdato() -> person.fødselsdato

            forrigeVilkårsPeriodeFom != null &&
                forrigeVilkårsPeriodeFom.isBefore(nyMigreringsdato) -> forrigeVilkårsPeriodeFom

            else -> nyMigreringsdato
        }
    }

    fun utledPeriodeTom(
        oppfylteVilkårResultaterForType: List<VilkårResultat>,
        vilkår: Vilkår,
        periodeFom: LocalDate,
    ): LocalDate? {
        val forrigeVilkårsPeriodeTom: LocalDate? =
            oppfylteVilkårResultaterForType.minWithOrNull(VilkårResultat.VilkårResultatComparator)?.periodeTom
        return when (vilkår) {
            Vilkår.UNDER_18_ÅR -> periodeFom.til18ÅrsVilkårsdato()
            else -> forrigeVilkårsPeriodeTom
        }
    }

    fun finnVilkårResultaterMedNyPeriodePgaNyMigreringsdato(
        oppfylteVilkårResultaterForPerson: List<VilkårResultat>,
        person: Person,
        nyMigreringsdato: LocalDate,
    ): List<VilkårResultatMedNyPeriode> {
        val vilkårTyperForPerson = oppfylteVilkårResultaterForPerson
            .map { it.vilkårType }

        return vilkårTyperForPerson.map { vilkår ->

            val oppfylteVilkårResultaterForType = oppfylteVilkårResultaterForPerson.filter { it.vilkårType == vilkår }

            val fom = utledPeriodeFom(
                oppfylteVilkårResultaterForType = oppfylteVilkårResultaterForType,
                vilkår = vilkår,
                person = person,
                nyMigreringsdato = nyMigreringsdato,
            )

            val tom: LocalDate? =
                utledPeriodeTom(
                    oppfylteVilkårResultaterForType,
                    vilkår,
                    fom,
                )

            // Når vi endrer migreringsdato flyttes den alltid bakover. Vilkårresultatet som forskyves vil derfor alltid være det med lavest periodeFom
            val vilkårResultatSomForskyves =
                oppfylteVilkårResultaterForType.minBy { it.periodeFom!! }
            VilkårResultatMedNyPeriode(vilkårResultatSomForskyves, fom, tom)
        }
    }
}

data class VilkårResultatMedNyPeriode(val vilkårResultat: VilkårResultat, val fom: LocalDate, val tom: LocalDate?)
