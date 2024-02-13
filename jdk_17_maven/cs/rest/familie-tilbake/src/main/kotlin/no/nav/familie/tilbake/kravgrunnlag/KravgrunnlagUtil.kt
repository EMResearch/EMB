package no.nav.familie.tilbake.kravgrunnlag

import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.JAXBException
import jakarta.xml.bind.Unmarshaller
import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.common.exceptionhandler.UgyldigKravgrunnlagFeil
import no.nav.familie.tilbake.kravgrunnlag.domain.Klassetype
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsbeløp433
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagDto
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagMelding
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagPeriodeDto
import no.nav.tilbakekreving.status.v1.EndringKravOgVedtakstatus
import no.nav.tilbakekreving.status.v1.KravOgVedtakstatus
import org.apache.commons.lang3.builder.DiffBuilder
import org.apache.commons.lang3.builder.ToStringStyle
import java.io.StringReader
import java.math.BigDecimal
import java.util.SortedMap
import javax.xml.XMLConstants
import javax.xml.validation.SchemaFactory

object KravgrunnlagUtil {

    private val jaxbContext: JAXBContext = JAXBContext.newInstance(DetaljertKravgrunnlagMelding::class.java)
    private val statusmeldingJaxbContext: JAXBContext = JAXBContext.newInstance(EndringKravOgVedtakstatus::class.java)

    fun finnFeilutbetalingPrPeriode(kravgrunnlag: Kravgrunnlag431): SortedMap<Månedsperiode, BigDecimal> {
        val feilutbetalingPrPeriode = mutableMapOf<Månedsperiode, BigDecimal>()
        for (kravgrunnlagPeriode432 in kravgrunnlag.perioder) {
            val feilutbetaltBeløp = kravgrunnlagPeriode432.beløp
                .filter { Klassetype.FEIL == it.klassetype }
                .sumOf(Kravgrunnlagsbeløp433::nyttBeløp)
            if (feilutbetaltBeløp.compareTo(BigDecimal.ZERO) != 0) {
                feilutbetalingPrPeriode[kravgrunnlagPeriode432.periode] = feilutbetaltBeløp
            }
        }
        return feilutbetalingPrPeriode.toSortedMap(Comparator.comparing(Månedsperiode::fom).thenComparing(Månedsperiode::tom))
    }

    fun unmarshalKravgrunnlag(kravgrunnlagXML: String): DetaljertKravgrunnlagDto {
        return try {
            val jaxbUnmarshaller: Unmarshaller = jaxbContext.createUnmarshaller()

            // satt xsd for å validere mottatt xml
            val schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
            val kravgrunnlagSchema =
                schemaFactory.newSchema(this.javaClass.classLoader.getResource("xsd/kravgrunnlag_detalj.xsd"))
            jaxbUnmarshaller.schema = kravgrunnlagSchema

            (jaxbUnmarshaller.unmarshal(StringReader(kravgrunnlagXML)) as DetaljertKravgrunnlagMelding).detaljertKravgrunnlag
        } catch (e: JAXBException) {
            throw UgyldigKravgrunnlagFeil(melding = "Mottatt kravgrunnlagXML er ugyldig! Den feiler med $e")
        }
    }

    fun unmarshalStatusmelding(statusmeldingXml: String): KravOgVedtakstatus {
        return try {
            val jaxbUnmarshaller: Unmarshaller = statusmeldingJaxbContext.createUnmarshaller()

            // satt xsd for å validere mottatt xml
            val schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
            val statusmeldingSchema =
                schemaFactory.newSchema(this.javaClass.classLoader.getResource("xsd/krav_og_vedtakstatus.xsd"))
            jaxbUnmarshaller.schema = statusmeldingSchema

            (jaxbUnmarshaller.unmarshal(StringReader(statusmeldingXml)) as EndringKravOgVedtakstatus).kravOgVedtakstatus
        } catch (e: JAXBException) {
            throw UgyldigKravgrunnlagFeil(melding = "Mottatt statusmeldingXML er ugyldig! Den feiler med $e")
        }
    }

    fun tilYtelsestype(fagområdekode: String): Ytelsestype {
        return Ytelsestype.values().firstOrNull { it.kode == fagområdekode }
            ?: throw IllegalArgumentException("Ukjent Ytelsestype for $fagområdekode")
    }

    fun sammenlignKravgrunnlag(mottattKravgrunnlag: DetaljertKravgrunnlagDto, hentetKravgrunnlag: DetaljertKravgrunnlagDto): String {
        val builder = DiffBuilder(mottattKravgrunnlag, hentetKravgrunnlag, ToStringStyle.JSON_STYLE)
            .append("kravgrunnlagId", mottattKravgrunnlag.kravgrunnlagId, hentetKravgrunnlag.kravgrunnlagId)
            .append("vedtakId", mottattKravgrunnlag.vedtakId, hentetKravgrunnlag.vedtakId)
            .append("kodeStatusKrav", mottattKravgrunnlag.kodeStatusKrav, hentetKravgrunnlag.kodeStatusKrav)
            .append("kodeFagomraade", mottattKravgrunnlag.kodeFagomraade, hentetKravgrunnlag.kodeFagomraade)
            .append("fagsystemId", mottattKravgrunnlag.fagsystemId, hentetKravgrunnlag.fagsystemId)
            .append("datoVedtakFagsystem", mottattKravgrunnlag.datoVedtakFagsystem, hentetKravgrunnlag.datoVedtakFagsystem)
            .append("vedtakIdOmgjort", mottattKravgrunnlag.vedtakIdOmgjort, hentetKravgrunnlag.vedtakIdOmgjort)
            .append("vedtakGjelderId", mottattKravgrunnlag.vedtakGjelderId, hentetKravgrunnlag.vedtakGjelderId)
            .append("typeGjelderId", mottattKravgrunnlag.typeGjelderId, hentetKravgrunnlag.typeGjelderId)
            .append("utbetalesTilId", mottattKravgrunnlag.utbetalesTilId, hentetKravgrunnlag.utbetalesTilId)
            .append("typeUtbetId", mottattKravgrunnlag.typeUtbetId, hentetKravgrunnlag.typeUtbetId)
            .append("kontrollfelt", mottattKravgrunnlag.kontrollfelt, hentetKravgrunnlag.kontrollfelt)
            .append("referanse", mottattKravgrunnlag.referanse, hentetKravgrunnlag.referanse)

        val mottattPerioder = mottattKravgrunnlag.tilbakekrevingsPeriode.sortedBy { it.periode.fom }
        val hentetPerioder = hentetKravgrunnlag.tilbakekrevingsPeriode.sortedBy { it.periode.fom }
        val differanser = sammenlignPerioder(mottattPerioder, hentetPerioder)
        val differanseBuilder = StringBuilder()
        if (differanser.isNotEmpty()) {
            differanseBuilder.append("Mangler periode ${differanser.map { konvertPeriode(it) }}.")
        }

        val perioder = mottattPerioder.zip(hentetPerioder)
        perioder.forEach {
            val periode = konvertPeriode(it.first)
            builder.append("periode", periode, konvertPeriode(it.second))
                .append("belopSkattMnd", it.first.belopSkattMnd, it.second.belopSkattMnd)

            val beløper = it.first.tilbakekrevingsBelop.sortedBy { beløp -> beløp.typeKlasse }
                .zip(it.second.tilbakekrevingsBelop.sortedBy { beløp -> beløp.typeKlasse })

            beløper.forEach { beløp ->
                builder.append("kodeKlasse", beløp.first.kodeKlasse, beløp.second.kodeKlasse)
                    .append("kodeKlasse", beløp.first.kodeKlasse, beløp.second.kodeKlasse)
                    .append("$periode->belopOpprUtbet", beløp.first.belopOpprUtbet, beløp.second.belopOpprUtbet)
                    .append("$periode->belopNy", beløp.first.belopNy, beløp.second.belopNy)
                    .append("$periode->belopUinnkrevd", beløp.first.belopUinnkrevd, beløp.second.belopUinnkrevd)
                    .append(
                        "$periode->belopTilbakekreves",
                        beløp.first.belopTilbakekreves,
                        beløp.second.belopTilbakekreves,
                    )
                    .append("$periode->skattProsent", beløp.first.skattProsent, beløp.second.skattProsent)
            }
        }

        return differanseBuilder.append(builder.build().toString()).toString()
    }

    private fun konvertPeriode(periodeDto: DetaljertKravgrunnlagPeriodeDto): Månedsperiode {
        return Månedsperiode(periodeDto.periode.fom, periodeDto.periode.tom)
    }

    private fun sammenlignPerioder(
        mottattPerioder: List<DetaljertKravgrunnlagPeriodeDto>,
        hentetPerioder: List<DetaljertKravgrunnlagPeriodeDto>,
    ): List<DetaljertKravgrunnlagPeriodeDto> {
        if (mottattPerioder.size == hentetPerioder.size) {
            return mottattPerioder.filter { hentetPerioder.none { mindre -> mindre.periode.fom == it.periode.fom } }
        }
        val størrePerioder = if (mottattPerioder.size > hentetPerioder.size) mottattPerioder else hentetPerioder
        val mindrePerioder = if (mottattPerioder.size < hentetPerioder.size) mottattPerioder else hentetPerioder

        return størrePerioder.filter { mindrePerioder.none { mindre -> mindre.periode.fom == it.periode.fom } }
    }
}
