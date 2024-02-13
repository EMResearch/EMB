package no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.ArrayNode
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.NullablePeriode
import no.nav.familie.ba.sak.kjerne.brev.domene.BrevBegrunnelseGrunnlagMedPersoner
import no.nav.familie.ba.sak.kjerne.brev.domene.RestBehandlingsgrunnlagForBrev

sealed interface IVedtakBegrunnelse {

    val sanityApiNavn: String
    val vedtakBegrunnelseType: VedtakBegrunnelseType
    val kanDelesOpp: Boolean

    fun delOpp(
        restBehandlingsgrunnlagForBrev: RestBehandlingsgrunnlagForBrev,
        triggesAv: TriggesAv,
        periode: NullablePeriode,
    ): List<BrevBegrunnelseGrunnlagMedPersoner>

    fun enumnavnTilString(): String

    companion object {
        fun konverterTilEnumVerdi(string: String): IVedtakBegrunnelse {
            val splittet = string.split('$')
            val type = splittet[0]
            val enumNavn = splittet[1]
            return when (type) {
                EØSStandardbegrunnelse::class.simpleName -> EØSStandardbegrunnelse.valueOf(enumNavn)
                Standardbegrunnelse::class.simpleName -> Standardbegrunnelse.valueOf(enumNavn)
                else -> throw Feil("Fikk en begrunnelse med ugyldig type: hverken EØSStandardbegrunnelse eller Standardbegrunnelse: $this")
            }
        }
    }
}

fun IVedtakBegrunnelse.erAvslagUregistrerteBarnBegrunnelse() =
    this in setOf(Standardbegrunnelse.AVSLAG_UREGISTRERT_BARN, EØSStandardbegrunnelse.AVSLAG_EØS_UREGISTRERT_BARN)

class IVedtakBegrunnelseDeserializer : StdDeserializer<List<IVedtakBegrunnelse>>(List::class.java) {
    override fun deserialize(jsonParser: JsonParser?, p1: DeserializationContext?): List<IVedtakBegrunnelse> {
        val node: ArrayNode = jsonParser!!.codec.readTree(jsonParser)
        return node
            .map { it.asText() }
            .map { IVedtakBegrunnelse.konverterTilEnumVerdi(it) }
    }
}

@Converter
class IVedtakBegrunnelseListConverter :
    AttributeConverter<List<IVedtakBegrunnelse>, String> {

    override fun convertToDatabaseColumn(vedtakbegrunnelser: List<IVedtakBegrunnelse>) =
        vedtakbegrunnelser.joinToString(";") { it.enumnavnTilString() }

    override fun convertToEntityAttribute(string: String?): List<IVedtakBegrunnelse> =
        if (string.isNullOrBlank()) {
            emptyList()
        } else {
            string.split(";")
                .map { IVedtakBegrunnelse.konverterTilEnumVerdi(it) }
        }
}
