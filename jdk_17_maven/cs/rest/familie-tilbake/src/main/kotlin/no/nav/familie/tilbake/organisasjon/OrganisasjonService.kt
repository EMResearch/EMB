package no.nav.familie.tilbake.organisasjon

import no.nav.familie.kontrakter.felles.organisasjon.Organisasjon
import no.nav.familie.tilbake.api.dto.InstitusjonDto
import no.nav.familie.tilbake.dokumentbestilling.felles.header.Institusjon
import no.nav.familie.tilbake.integration.familie.IntegrasjonerClient
import org.springframework.stereotype.Service

@Service
class OrganisasjonService(private val integrasjonerClient: IntegrasjonerClient) {

    fun mapTilInstitusjonDto(orgnummer: String): InstitusjonDto {
        val organisasjon = hentOrganisasjon(orgnummer)
        return InstitusjonDto(organisasjonsnummer = orgnummer, navn = organisasjon.navn)
    }

    fun mapTilInstitusjonForBrevgenerering(orgnummer: String): Institusjon {
        val organisasjon = hentOrganisasjon(orgnummer)
        return Institusjon(organisasjonsnummer = orgnummer, navn = organisasjon.navn)
    }

    private fun hentOrganisasjon(orgnummer: String): Organisasjon {
        val organisasjon = integrasjonerClient.hentOrganisasjon(orgnummer)
        return organisasjon
    }
}
