package no.nav.familie.tilbake.integration.familie

import no.nav.familie.http.client.AbstractPingableRestClient
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.Fil
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.dokarkiv.ArkiverDokumentResponse
import no.nav.familie.kontrakter.felles.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.dokdist.DistribuerJournalpostRequest
import no.nav.familie.kontrakter.felles.dokdist.Distribusjonstidspunkt
import no.nav.familie.kontrakter.felles.dokdist.Distribusjonstype
import no.nav.familie.kontrakter.felles.dokdist.ManuellAdresse
import no.nav.familie.kontrakter.felles.getDataOrThrow
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.JournalposterForBrukerRequest
import no.nav.familie.kontrakter.felles.navkontor.NavKontorEnhet
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.MappeDto
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgaveResponse
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import no.nav.familie.kontrakter.felles.organisasjon.Organisasjon
import no.nav.familie.kontrakter.felles.saksbehandler.Saksbehandler
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import no.nav.familie.tilbake.config.IntegrasjonerConfig
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpHeaders
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class IntegrasjonerClient(
    @Qualifier("azure") restOperations: RestOperations,
    private val integrasjonerConfig: IntegrasjonerConfig,
) :
    AbstractPingableRestClient(restOperations, "familie.integrasjoner") {

    override val pingUri: URI =
        UriComponentsBuilder.fromUri(integrasjonerConfig.integrasjonUri).path(IntegrasjonerConfig.PATH_PING).build().toUri()

    private val arkiverUri: URI = UriComponentsBuilder.fromUri(integrasjonerConfig.integrasjonUri)
        .pathSegment(IntegrasjonerConfig.PATH_ARKIVER)
        .build()
        .toUri()

    private val distribuerUri: URI = UriComponentsBuilder.fromUri(integrasjonerConfig.integrasjonUri)
        .pathSegment(IntegrasjonerConfig.PATH_DISTRIBUER)
        .build()
        .toUri()

    private val sftpUri: URI = UriComponentsBuilder.fromUri(integrasjonerConfig.integrasjonUri)
        .pathSegment(IntegrasjonerConfig.PATH_SFTP)
        .build()
        .toUri()

    private val tilgangssjekkUri = UriComponentsBuilder.fromUri(integrasjonerConfig.integrasjonUri)
        .pathSegment(IntegrasjonerConfig.PATH_TILGANGSSJEKK)
        .build()
        .toUri()

    private fun hentSaksbehandlerUri(id: String) = UriComponentsBuilder.fromUri(integrasjonerConfig.integrasjonUri)
        .pathSegment(IntegrasjonerConfig.PATH_SAKSBEHANDLER, id)
        .build()
        .toUri()

    private val opprettOppgaveUri = UriComponentsBuilder.fromUri(integrasjonerConfig.integrasjonUri)
        .pathSegment(IntegrasjonerConfig.PATH_OPPGAVE, "opprett")
        .build()
        .toUri()

    private fun patchOppgaveUri(oppgave: Oppgave) = UriComponentsBuilder.fromUri(integrasjonerConfig.integrasjonUri)
        .pathSegment(IntegrasjonerConfig.PATH_OPPGAVE, oppgave.id!!.toString(), "oppdater")
        .build()
        .toUri()
    private fun tilordneOppgaveNyEnhetUri(oppgaveId: Long, nyEnhet: String, fjernMappeFraOppgave: Boolean) = UriComponentsBuilder.fromUri(integrasjonerConfig.integrasjonUri)
        .pathSegment(IntegrasjonerConfig.PATH_OPPGAVE, oppgaveId.toString(), "enhet", nyEnhet)
        .queryParam("fjernMappeFraOppgave", fjernMappeFraOppgave)
        .build()
        .toUri()

    private val finnoppgaverUri = UriComponentsBuilder.fromUri(integrasjonerConfig.integrasjonUri)
        .pathSegment(IntegrasjonerConfig.PATH_OPPGAVE, "v4")
        .build()
        .toUri()

    private fun ferdigstillOppgaveUri(oppgaveId: Long) = UriComponentsBuilder.fromUri(integrasjonerConfig.integrasjonUri)
        .pathSegment(IntegrasjonerConfig.PATH_OPPGAVE, oppgaveId.toString(), "ferdigstill")
        .build()
        .toUri()

    private fun hentOrganisasjonUri(organisasjonsnummer: String) =
        UriComponentsBuilder.fromUri(integrasjonerConfig.integrasjonUri)
            .pathSegment(IntegrasjonerConfig.PATH_ORGANISASJON, organisasjonsnummer)
            .build()
            .toUri()

    private fun validerOrganisasjonUri(organisasjonsnummer: String) =
        UriComponentsBuilder.fromUri(integrasjonerConfig.integrasjonUri)
            .pathSegment(IntegrasjonerConfig.PATH_ORGANISASJON, organisasjonsnummer, "valider")
            .build()
            .toUri()

    private fun hentJournalpostUri() = UriComponentsBuilder.fromUri(integrasjonerConfig.integrasjonUri)
        .pathSegment(IntegrasjonerConfig.PATH_JOURNALPOST)
        .build()
        .toUri()

    private fun hentJournalpostHentDokumentUri(journalpostId: String, dokumentInfoId: String) =
        UriComponentsBuilder.fromUri(integrasjonerConfig.integrasjonUri)
            .pathSegment(IntegrasjonerConfig.PATH_HENTDOKUMENT, journalpostId, dokumentInfoId)
            .build()
            .toUri()

    private fun hentNavkontorUri(enhetsId: String) = UriComponentsBuilder.fromUri(integrasjonerConfig.integrasjonUri)
        .pathSegment(IntegrasjonerConfig.PATH_NAVKONTOR, enhetsId)
        .build()
        .toUri()

    private fun finnMapperUri(enhetNr: String): URI = UriComponentsBuilder.fromUri(integrasjonerConfig.integrasjonUri)
        .pathSegment(IntegrasjonerConfig.PATH_OPPGAVE, "mappe", "finn", enhetNr)
        .build()
        .toUri()

    fun arkiver(arkiverDokumentRequest: ArkiverDokumentRequest): ArkiverDokumentResponse {
        val response = postForEntity<Ressurs<ArkiverDokumentResponse>>(arkiverUri, arkiverDokumentRequest)
        return response.getDataOrThrow()
    }

    fun sendFil(fil: Fil) {
        putForEntity<Any>(sftpUri, fil)
    }

    fun distribuerJournalpost(
        journalpostId: String,
        fagsystem: Fagsystem,
        distribusjonstype: Distribusjonstype,
        distribusjonstidspunkt: Distribusjonstidspunkt,
        manuellAdresse: ManuellAdresse? = null,
    ): String {
        val request = DistribuerJournalpostRequest(
            journalpostId,
            fagsystem,
            integrasjonerConfig.applicationName,
            distribusjonstype,
            distribusjonstidspunkt,
            manuellAdresse,
        )
        return postForEntity<Ressurs<String>>(distribuerUri, request).getDataOrThrow()
    }

    fun hentDokument(dokumentInfoId: String, journalpostId: String): ByteArray {
        return getForEntity<Ressurs<ByteArray>>(hentJournalpostHentDokumentUri(journalpostId, dokumentInfoId)).getDataOrThrow()
    }

    fun hentOrganisasjon(organisasjonsnummer: String): Organisasjon {
        return getForEntity<Ressurs<Organisasjon>>(hentOrganisasjonUri(organisasjonsnummer)).getDataOrThrow()
    }

    fun validerOrganisasjon(organisasjonsnummer: String): Boolean {
        return try {
            getForEntity<Ressurs<Boolean>>(validerOrganisasjonUri(organisasjonsnummer)).data == true
        } catch (e: Exception) {
            log.error("Organisasjonsnummeret $organisasjonsnummer er ikke gyldig. Feiler med ${e.message}")
            false
        }
    }

    fun hentSaksbehandler(id: String): Saksbehandler {
        return getForEntity<Ressurs<Saksbehandler>>(hentSaksbehandlerUri(id)).getDataOrThrow()
    }

    fun opprettOppgave(opprettOppgave: OpprettOppgaveRequest): OppgaveResponse {
        return postForEntity<Ressurs<OppgaveResponse>>(opprettOppgaveUri, opprettOppgave).getDataOrThrow()
    }

    fun patchOppgave(patchOppgave: Oppgave): OppgaveResponse {
        val uri = patchOppgaveUri(patchOppgave)
        return patchForEntity<Ressurs<OppgaveResponse>>(uri, patchOppgave).getDataOrThrow()
    }

    internal fun tilordneOppgaveNyEnhet(oppgaveId: Long, nyEnhet: String, fjernMappeFraOppgave: Boolean): OppgaveResponse {
        val uri = tilordneOppgaveNyEnhetUri(oppgaveId, nyEnhet, fjernMappeFraOppgave)
        return patchForEntity<Ressurs<OppgaveResponse>>(uri, "", HttpHeaders().medContentTypeJsonUTF8()).getDataOrThrow()
    }

    fun finnOppgaver(finnOppgaveRequest: FinnOppgaveRequest): FinnOppgaveResponseDto {
        return postForEntity<Ressurs<FinnOppgaveResponseDto>>(finnoppgaverUri, finnOppgaveRequest).getDataOrThrow()
    }

    fun ferdigstillOppgave(oppgaveId: Long) {
        patchForEntity<Ressurs<OppgaveResponse>>(ferdigstillOppgaveUri(oppgaveId), "")
    }

    @Cacheable("mappeCache")
    fun finnMapper(enhet: String): List<MappeDto> {
        val respons = getForEntity<Ressurs<List<MappeDto>>>(finnMapperUri(enhet))
        return respons.getDataOrThrow()
    }

    fun hentNavkontor(enhetsId: String): NavKontorEnhet {
        return getForEntity<Ressurs<NavKontorEnhet>>(hentNavkontorUri(enhetsId)).getDataOrThrow()
    }

    /*
     * Sjekker personene i behandlingen er egen ansatt, kode 6 eller kode 7. Og om saksbehandler har rettigheter til å behandle
     * slike personer.
     */
    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delayExpression = "5000"),
    )
    fun sjekkTilgangTilPersoner(personIdenter: List<String>): List<Tilgang> {
        return postForEntity(tilgangssjekkUri, personIdenter)
    }

    fun hentJournalposterForBruker(journalposterForBrukerRequest: JournalposterForBrukerRequest): List<Journalpost> {
        secureLogger.info(
            "henter journalposter for bruker med ident ${journalposterForBrukerRequest.brukerId} " +
                "og data $journalposterForBrukerRequest",
        )

        return postForEntity<Ressurs<List<Journalpost>>>(hentJournalpostUri(), journalposterForBrukerRequest).getDataOrThrow()
    }
}

fun HttpHeaders.medContentTypeJsonUTF8(): HttpHeaders {
    this.add("Content-Type", "application/json;charset=UTF-8")
    this.acceptCharset = listOf(Charsets.UTF_8)
    return this
}
