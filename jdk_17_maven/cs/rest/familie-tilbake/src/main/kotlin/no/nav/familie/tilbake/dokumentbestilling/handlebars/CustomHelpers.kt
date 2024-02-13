package no.nav.familie.tilbake.dokumentbestilling.handlebars

import com.github.jknack.handlebars.Context
import com.github.jknack.handlebars.Helper
import com.github.jknack.handlebars.Options
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.tilbake.common.DatoUtil.DATO_FORMAT_DATO_MÅNEDSNAVN_ÅR
import org.apache.commons.lang3.StringUtils
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class SwitchHelper : Helper<Any> {

    override fun apply(variabel: Any, options: Options): Any {
        val variabelnavn: MutableList<String> = ArrayList()
        val variabelverdier: MutableList<Any> = ArrayList()
        variabelnavn.add("__condition_fulfilled")
        variabelverdier.add(0)
        variabelnavn.add("__condition_variable")
        variabelverdier.add(if (options.hash.isEmpty()) variabel else options.hash)
        val ctx: Context = Context.newBlockParamContext(options.context, variabelnavn, variabelverdier)
        val resultat: String = options.fn.apply(ctx)
        val antall = ctx.get("__condition_fulfilled") as Int
        if (Integer.valueOf(1) == antall) {
            return resultat
        }
        throw IllegalArgumentException(
            "Switch-case må treffe i 1 case, men traff i " + antall +
                " med verdien " + ctx.get("__condition_variable"),
        )
    }
}

class CaseHelper : Helper<Any?> {

    override fun apply(caseKonstant: Any?, options: Options): Any {
        val konstant = if (options.hash.isEmpty()) caseKonstant else options.hash

        @Suppress("UNCHECKED_CAST")
        val model = options.context.model() as MutableMap<String, Any>
        val conditionVariable = model["__condition_variable"]
        if (konstant == conditionVariable) {
            val antall = model["__condition_fulfilled"] as Int?
            model["__condition_fulfilled"] = antall!! + 1
            return options.fn()
        }
        return options.inverse()
    }
}

class VariableHelper : Helper<Any?> {

    override fun apply(context: Any?, options: Options): Any {
        val variabelnavn: MutableList<String> = ArrayList()
        val variabelverdier: MutableList<Any> = ArrayList()
        for ((key, value) in options.hash.entries) {
            variabelnavn.add(key)
            variabelverdier.add(value)
        }
        val ctx: Context = Context.newBlockParamContext(options.context, variabelnavn, variabelverdier)
        return options.fn.apply(ctx)
    }
}

class MapLookupHelper : Helper<Any> {

    override fun apply(context: Any?, options: Options): Any {
        val key = context.toString()
        val defaultVerdi: Any? = options.param(0, null)
        return options.hash(key, defaultVerdi)
            ?: throw IllegalArgumentException("Fant ikke verdi for " + key + " i " + options.hash)
    }
}

class DatoHelper : Helper<Any> {

    private val format = DATO_FORMAT_DATO_MÅNEDSNAVN_ÅR

    override fun apply(context: Any, options: Options?): Any {
        val date = objectMapper.convertValue(context, LocalDate::class.java)
        return format.format(date)
    }
}

class KortdatoHelper : Helper<Any> {

    private val format = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    override fun apply(context: Any, options: Options?): Any {
        val date = objectMapper.convertValue(context, LocalDate::class.java)
        return format.format(date)
    }
}

class MånedHelper : Helper<Any> {

    private val format = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("NO"))

    override fun apply(context: Any, options: Options?): Any {
        val date = objectMapper.convertValue(context, YearMonth::class.java)
        return format.format(date)
    }
}

class StorBokstavHelper : Helper<String> {

    override fun apply(context: String, options: Options?): Any {
        return StringUtils.capitalize(context)
    }
}

class KroneFormattererMedTusenskille : Helper<Any> {

    override fun apply(context: Any?, options: Options?): Any {
        if (context == null) {
            return "ERROR"
        }
        val key = context.toString()

        return formatterKronerMedTusenskille(BigDecimal(key), utf8nonBreakingSpace)
    }

    companion object {
        val utf8nonBreakingSpace = '\u00A0'

        fun formatterKronerMedTusenskille(verdi: BigDecimal, space: Char): String {
            val beløp = verdi
            val beløpMedTusenskille = medTusenskille(beløp, space)
            val benevning = if (beløp.compareTo(BigDecimal.ONE) == 0) "krone" else "kroner"
            return beløpMedTusenskille + space + benevning
        }

        fun medTusenskille(verdi: BigDecimal, tusenskille: Char): String {
            val symbols = DecimalFormatSymbols.getInstance()
            symbols.groupingSeparator = tusenskille
            val formatter = DecimalFormat("###,###", symbols)
            return formatter.format(verdi.toLong())
        }
    }
}
