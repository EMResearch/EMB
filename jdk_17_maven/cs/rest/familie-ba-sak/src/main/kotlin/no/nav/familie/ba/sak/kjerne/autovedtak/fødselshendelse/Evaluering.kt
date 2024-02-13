package no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse

interface EvalueringÅrsak {

    fun hentBeskrivelse(): String

    fun hentMetrikkBeskrivelse(): String

    fun hentIdentifikator(): String
}

data class Evaluering(
    val resultat: Resultat,
    val evalueringÅrsaker: List<EvalueringÅrsak>,
    val begrunnelse: String,
    val beskrivelse: String = "",
    val identifikator: String = "",
) {

    companion object {

        fun oppfylt(evalueringÅrsak: EvalueringÅrsak) = Evaluering(
            Resultat.OPPFYLT,
            listOf(evalueringÅrsak),
            evalueringÅrsak.hentBeskrivelse(),
        )

        fun ikkeOppfylt(evalueringÅrsak: EvalueringÅrsak) = Evaluering(
            Resultat.IKKE_OPPFYLT,
            listOf(evalueringÅrsak),
            evalueringÅrsak.hentBeskrivelse(),
        )

        fun ikkeVurdert(evalueringÅrsak: EvalueringÅrsak) = Evaluering(
            Resultat.IKKE_VURDERT,
            listOf(evalueringÅrsak),
            evalueringÅrsak.hentBeskrivelse(),
        )
    }
}

enum class Resultat {
    OPPFYLT,
    IKKE_OPPFYLT,
    IKKE_VURDERT,
}

fun List<Evaluering>.erOppfylt() = this.all { it.resultat == Resultat.OPPFYLT }
