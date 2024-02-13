package no.nav.familie.ba.sak.kjerne.brev

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.kontrakter.felles.objectMapper
import org.springframework.core.io.ClassPathResource
import java.io.BufferedReader

data class LandkodeISO2(
    val code: String,
    val name: String,
)

fun hentLandkoderISO2(): Map<String, String> {
    val landkoder =
        ClassPathResource("landkoder/landkoder.json").inputStream.bufferedReader().use(BufferedReader::readText)

    return objectMapper.readValue<List<LandkodeISO2>>(landkoder)
        .associate { it.code to it.name }
}

val LANDKODER = hentLandkoderISO2()
