package no.nav.familie.tilbake.dokumentbestilling.handlebars

import com.fasterxml.jackson.databind.JsonNode
import com.github.jknack.handlebars.Context
import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.JsonNodeValueResolver
import com.github.jknack.handlebars.Template
import com.github.jknack.handlebars.context.JavaBeanValueResolver
import com.github.jknack.handlebars.context.MapValueResolver
import com.github.jknack.handlebars.helper.ConditionalHelpers
import com.github.jknack.handlebars.io.ClassPathTemplateLoader
import no.nav.familie.kontrakter.felles.Språkkode
import no.nav.familie.tilbake.dokumentbestilling.handlebars.dto.Språkstøtte
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.Locale

object FellesTekstformaterer {

    private val TEMPLATE_CACHE: MutableMap<String, Template> = HashMap()

    private val OM = ObjectMapperForUtvekslingAvDataMedHandlebars.INSTANCE

    fun lagBrevtekst(data: Språkstøtte, filsti: String): String {
        val template = getTemplate(data.språkkode, filsti)
        return applyTemplate(data, template)
    }

    fun lagDeltekst(data: Språkstøtte, filsti: String): String {
        val template = getTemplateFraPartial(data.språkkode, filsti)
        return applyTemplate(data, template)
    }

    private fun getTemplate(språkkode: Språkkode, filsti: String): Template {
        val språkstøttetFilsti: String = lagSpråkstøttetFilsti(filsti, språkkode)
        if (TEMPLATE_CACHE.containsKey(språkstøttetFilsti)) {
            return TEMPLATE_CACHE[språkstøttetFilsti]!!
        }
        TEMPLATE_CACHE[språkstøttetFilsti] =
            opprettTemplate(språkstøttetFilsti)
        return TEMPLATE_CACHE[språkstøttetFilsti]!!
    }

    private fun getTemplateFraPartial(språkkode: Språkkode, partial: String): Template {
        val språkstøttetFilsti: String = lagSpråkstøttetFilsti(partial, språkkode)
        if (TEMPLATE_CACHE.containsKey(språkstøttetFilsti)) {
            return TEMPLATE_CACHE[språkstøttetFilsti]!!
        }
        TEMPLATE_CACHE[språkstøttetFilsti] = opprettTemplateFraPartials(
            lagSpråkstøttetFilsti("vedtak/vedtak_felles", språkkode),
            språkstøttetFilsti,
        )
        return TEMPLATE_CACHE[språkstøttetFilsti]!!
    }

    private fun opprettTemplate(språkstøttetFilsti: String): Template {
        return opprettHandlebarsKonfigurasjon().compile(språkstøttetFilsti)
    }

    private fun opprettTemplateFraPartials(vararg partials: String): Template {
        val partialString = partials.joinToString("") { "{{> $it}}\n" }
        return try {
            opprettHandlebarsKonfigurasjon().compileInline(partialString)
        } catch (e: IOException) {
            error("Klarte ikke å kompilere partial template $partials")
        }
    }

    private fun applyTemplate(data: Any, template: Template): String {
        return try {
            // Går via JSON for å
            // 1. tilrettelegger for å flytte generering til PDF etc til ekstern applikasjon
            // 2. ha egen navngiving på variablene i template for enklere å lese template
            // 3. unngår at template feiler når variable endrer navn
            val jsonNode: JsonNode = OM.valueToTree(data)
            val context = Context.newBuilder(jsonNode)
                .resolver(JsonNodeValueResolver.INSTANCE, JavaBeanValueResolver.INSTANCE, MapValueResolver.INSTANCE)
                .build()
            template.apply(context).trim()
        } catch (e: IOException) {
            throw IllegalStateException("Feil ved tekstgenerering.")
        }
    }

    private fun opprettHandlebarsKonfigurasjon(): Handlebars {
        val loader = ClassPathTemplateLoader().apply {
            charset = StandardCharsets.UTF_8
            prefix = "/templates/"
            suffix = ".hbs"
        }

        return Handlebars(loader).apply {
            charset = StandardCharsets.UTF_8
            setInfiniteLoops(false)
            setPrettyPrint(true)
            registerHelpers(ConditionalHelpers::class.java)
            registerHelper("kroner", KroneFormattererMedTusenskille())
            registerHelper("dato", DatoHelper())
            registerHelper("kortdato", KortdatoHelper())
            registerHelper("måned", MånedHelper())
            registerHelper("storForbokstav", StorBokstavHelper())
            registerHelper("switch", SwitchHelper())
            registerHelper("case", CaseHelper())
            registerHelper("var", VariableHelper())
            registerHelper("lookup-map", MapLookupHelper())
        }
    }

    private fun lagSpråkstøttetFilsti(filsti: String, språkkode: Språkkode): String {
        return String.format("%s/%s", språkkode.name.lowercase(Locale.getDefault()), filsti)
    }
}
