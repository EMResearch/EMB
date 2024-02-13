package no.nav.familie.ba.sak.integrasjoner.organisasjon

import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.kontrakter.felles.organisasjon.Organisasjon
import org.springframework.stereotype.Service

@Service
class OrganisasjonService(private val integrasjonClient: IntegrasjonClient) {

    fun hentOrganisasjon(orgnummer: String): Organisasjon {
        val organisasjon = integrasjonClient.hentOrganisasjon(orgnummer)
        return organisasjon
    }
}
