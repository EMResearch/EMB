package no.nav.familie.ba.sak.kjerne.brev.domene

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.Utils.storForbokstav
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseAktivitet
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.vedtak.domene.MinimertRestPerson

data class MinimertKompetanse(
    val søkersAktivitet: KompetanseAktivitet,
    val søkersAktivitetsland: LandNavn?,
    val annenForeldersAktivitet: KompetanseAktivitet,
    val annenForeldersAktivitetslandNavn: LandNavn?,
    val barnetsBostedslandNavn: LandNavn,
    val resultat: KompetanseResultat,
    val personer: List<MinimertRestPerson>,
)

fun Kompetanse.tilMinimertKompetanse(
    personopplysningGrunnlag: PersonopplysningGrunnlag,
    landkoderISO2: Map<String, String>,
): MinimertKompetanse {
    this.validerFelterErSatt()

    val barnetsBostedslandNavn = this.barnetsBostedsland!!.tilLandNavn(landkoderISO2)
    val annenForeldersAktivitetslandNavn = this.annenForeldersAktivitetsland?.tilLandNavn(landkoderISO2)
    val sokersAktivitetslandNavn = this.søkersAktivitetsland?.tilLandNavn(landkoderISO2)

    return MinimertKompetanse(
        søkersAktivitet = this.søkersAktivitet!!,
        søkersAktivitetsland = sokersAktivitetslandNavn,
        annenForeldersAktivitet = this.annenForeldersAktivitet!!,
        annenForeldersAktivitetslandNavn = annenForeldersAktivitetslandNavn,
        barnetsBostedslandNavn = barnetsBostedslandNavn,
        resultat = this.resultat!!,
        personer = this.barnAktører.map { aktør ->
            val fødselsdato = personopplysningGrunnlag.barna.find { it.aktør == aktør }?.fødselsdato
                ?: throw Feil("Fant ikke aktør i personopplysninggrunnlaget")
            MinimertRestPerson(
                personIdent = aktør.aktivFødselsnummer(),
                fødselsdato = fødselsdato,
                type = PersonType.BARN,
            )
        },
    )
}

data class LandNavn(val navn: String)

private fun String.tilLandNavn(landkoderISO2: Map<String, String>): LandNavn {
    if (this.length != 2) {
        throw Feil("LandkoderISO2 forventer en landkode med to tegn")
    }

    val landNavn = (
        landkoderISO2[this]
            ?: throw Feil("Fant ikke navn for landkode $this ")
        )

    return LandNavn(landNavn.storForbokstav())
}
