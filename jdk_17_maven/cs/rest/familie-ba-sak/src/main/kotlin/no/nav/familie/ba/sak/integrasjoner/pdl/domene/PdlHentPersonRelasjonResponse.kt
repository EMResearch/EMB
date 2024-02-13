package no.nav.familie.ba.sak.integrasjoner.pdl.domene

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.familie.kontrakter.felles.personopplysning.ForelderBarnRelasjon

class PdlHentPersonRelasjonerResponse(val person: PdlPersonRelasjonData?)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PdlPersonRelasjonData(
    val forelderBarnRelasjon: List<ForelderBarnRelasjon> = emptyList(),
)
