package no.nav.familie.ba.sak.kjerne.tidslinje.util

import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.eøs.util.SkjemaBuilder
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidspunkt

class KompetanseBuilder(
    startMåned: Tidspunkt<Måned> = jan(2020),
    behandlingId: BehandlingId = BehandlingId(1),
) : SkjemaBuilder<Kompetanse, KompetanseBuilder>(startMåned, behandlingId) {

    fun medKompetanse(
        k: String,
        vararg barn: Person,
        annenForeldersAktivitetsland: String? = null,
        erAnnenForelderOmfattetAvNorskLovgivning: Boolean? = false,
    ) =
        medSkjema(k, barn.toList()) {
            when (it) {
                '-' -> Kompetanse.NULL.copy(
                    annenForeldersAktivitetsland = annenForeldersAktivitetsland,
                    erAnnenForelderOmfattetAvNorskLovgivning = erAnnenForelderOmfattetAvNorskLovgivning,
                )

                'S' -> Kompetanse.NULL.copy(
                    resultat = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND,
                    annenForeldersAktivitetsland = annenForeldersAktivitetsland,
                    erAnnenForelderOmfattetAvNorskLovgivning = erAnnenForelderOmfattetAvNorskLovgivning,
                ).fyllUt()

                'P' -> Kompetanse.NULL.copy(
                    resultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
                    annenForeldersAktivitetsland = annenForeldersAktivitetsland,
                    erAnnenForelderOmfattetAvNorskLovgivning = erAnnenForelderOmfattetAvNorskLovgivning,
                ).fyllUt()

                else -> null
            }
        }

    fun byggKompetanser(): Collection<Kompetanse> = bygg()
}

fun Kompetanse.fyllUt() = this.copy(
    erAnnenForelderOmfattetAvNorskLovgivning = erAnnenForelderOmfattetAvNorskLovgivning ?: false,
    resultat = resultat ?: KompetanseResultat.NORGE_ER_SEKUNDÆRLAND,
    annenForeldersAktivitetsland = annenForeldersAktivitetsland ?: "DK",
    barnetsBostedsland = barnetsBostedsland ?: "NO",
    søkersAktivitet = søkersAktivitet,
    annenForeldersAktivitet = annenForeldersAktivitet,
    søkersAktivitetsland = søkersAktivitetsland ?: "SE",
)
