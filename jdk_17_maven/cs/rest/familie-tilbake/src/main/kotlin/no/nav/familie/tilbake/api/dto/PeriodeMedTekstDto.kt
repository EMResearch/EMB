package no.nav.familie.tilbake.api.dto

import jakarta.validation.constraints.Size
import no.nav.familie.kontrakter.felles.Datoperiode

class PeriodeMedTekstDto(
    val periode: Datoperiode,
    @Size(max = 4000, message = "Fritekst for fakta er for lang")
    val faktaAvsnitt: String? = null,
    @Size(max = 4000, message = "Fritekst for foreldelse er for lang")
    val foreldelseAvsnitt: String? = null,
    @Size(max = 4000, message = "Fritekst for vilkår er for lang")
    val vilkårAvsnitt: String? = null,
    @Size(max = 4000, message = "Fritekst for særlige grunner er for lang")
    val særligeGrunnerAvsnitt: String? = null,
    @Size(max = 4000, message = "Fritekst for særlige grunner annet er for lang")
    val særligeGrunnerAnnetAvsnitt: String? = null,
)
