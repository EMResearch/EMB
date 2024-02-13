package no.nav.familie.ba.sak.integrasjoner.pdl.domene

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

class PdlUtenlandskAdressseResponse(val person: PdlUtenlandskAdresssePerson?)

class PdlUtenlandskAdresssePerson(val bostedsadresse: List<PdlUtenlandskAdresssePersonBostedsadresse>)
class PdlUtenlandskAdresssePersonBostedsadresse(val utenlandskAdresse: PdlUtenlandskAdresssePersonUtenlandskAdresse?)

@JsonIgnoreProperties(ignoreUnknown = true)
class PdlUtenlandskAdresssePersonUtenlandskAdresse(
    val landkode: String,
)
