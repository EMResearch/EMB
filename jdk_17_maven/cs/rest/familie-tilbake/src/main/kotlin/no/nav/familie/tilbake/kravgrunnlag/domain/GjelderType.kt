package no.nav.familie.tilbake.kravgrunnlag.domain

enum class GjelderType(val navn: String) {

    PERSON("Person"),
    ORGANISASJON("Organisasjon"),
    SAMHANDLER("Samhandler"),
    APPLIKASJONSBRUKER("Applikasjonsbruker"),
    ;

    companion object {

        fun fraKode(kode: String): GjelderType {
            for (gjelderType in values()) {
                if (gjelderType.name == kode) {
                    return gjelderType
                }
            }
            throw IllegalArgumentException("Ukjent GjelderType $kode")
        }
    }
}
