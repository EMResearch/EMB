package no.nav.familie.tilbake.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import java.net.URI

@Configuration
class IntegrasjonerConfig(
    @Value("\${FAMILIE_INTEGRASJONER_URL}") val integrasjonUri: URI,
    @Value("\${application.name}") val applicationName: String,
) {

    companion object {

        const val PATH_PING = "internal/status/isAlive"
        const val PATH_ORGANISASJON = "api/organisasjon"
        const val PATH_SAKSBEHANDLER = "api/saksbehandler"
        const val PATH_TILGANGSSJEKK = "api/tilgang/personer"
        const val PATH_ARKIVER = "api/arkiv/v4"
        const val PATH_DISTRIBUER = "api/dist/v1"
        const val PATH_SFTP = "api/adramatch/avstemming"
        const val PATH_OPPGAVE = "api/oppgave"
        const val PATH_NAVKONTOR = "api/arbeidsfordeling/nav-kontor"

        const val PATH_JOURNALPOST = "api/journalpost"
        const val PATH_HENTDOKUMENT = "api/journalpost/hentdokument"
    }
}
