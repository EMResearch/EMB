package no.nav.familie.ba.sak.integrasjoner.pdl.domene

class PdlHentIdenterResponse(val pdlIdenter: PdlIdenter?)

data class PdlIdenter(val identer: List<IdentInformasjon>)

data class IdentInformasjon(
    val ident: String,
    val historisk: Boolean,
    val gruppe: String,
)
