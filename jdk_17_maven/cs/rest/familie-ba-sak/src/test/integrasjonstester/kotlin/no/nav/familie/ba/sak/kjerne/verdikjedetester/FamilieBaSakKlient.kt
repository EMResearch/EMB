package no.nav.familie.ba.sak.kjerne.verdikjedetester

import no.nav.familie.ba.sak.ekstern.restDomene.RestEndretUtbetalingAndel
import no.nav.familie.ba.sak.ekstern.restDomene.RestFagsak
import no.nav.familie.ba.sak.ekstern.restDomene.RestHentFagsakForPerson
import no.nav.familie.ba.sak.ekstern.restDomene.RestJournalføring
import no.nav.familie.ba.sak.ekstern.restDomene.RestMinimalFagsak
import no.nav.familie.ba.sak.ekstern.restDomene.RestPersonResultat
import no.nav.familie.ba.sak.ekstern.restDomene.RestPutVedtaksperiodeMedStandardbegrunnelser
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.ekstern.restDomene.RestTilbakekreving
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.DEFAULT_JOURNALFØRENDE_ENHET
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.RestHenleggBehandlingInfo
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.brev.domene.ManueltBrevRequest
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRequest
import no.nav.familie.ba.sak.kjerne.fagsak.RestBeslutningPåVedtak
import no.nav.familie.ba.sak.kjerne.logg.Logg
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.RestUtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.felles.Ressurs
import org.springframework.http.HttpHeaders
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriUtils.encodePath
import java.net.URI
import java.time.LocalDate

class FamilieBaSakKlient(
    private val baSakUrl: String,
    restOperations: RestOperations,
    private val headers: HttpHeaders,
) : AbstractRestClient(restOperations, "familie-ba-sak") {

    fun opprettFagsak(søkersIdent: String): Ressurs<RestMinimalFagsak> {
        val uri = URI.create("$baSakUrl/api/fagsaker")

        return postForEntity(
            uri,
            FagsakRequest(
                personIdent = søkersIdent,
            ),
            headers,
        )
    }

    fun hentFagsak(fagsakId: Long): Ressurs<RestFagsak> {
        val uri = URI.create("$baSakUrl/api/fagsaker/$fagsakId")

        return getForEntity(
            uri,
            headers,
        )
    }

    fun hentMinimalFagsakPåPerson(personIdent: String): Ressurs<RestMinimalFagsak> {
        val uri = URI.create("$baSakUrl/api/fagsaker/hent-fagsak-paa-person")

        return postForEntity(
            uri,
            RestHentFagsakForPerson(personIdent),
            headers,
        )
    }

    fun journalfør(
        journalpostId: String,
        oppgaveId: String,
        journalførendeEnhet: String,
        restJournalføring: RestJournalføring,
    ): Ressurs<String> {
        val uri =
            URI.create(encodePath("$baSakUrl/api/journalpost/$journalpostId/journalfør/$oppgaveId") + "?journalfoerendeEnhet=$journalførendeEnhet")
        return postForEntity(
            uri,
            restJournalføring,
            headers,
        )
    }

    fun behandlingsresultatStegOgGåVidereTilNesteSteg(behandlingId: Long): Ressurs<RestUtvidetBehandling> {
        val uri = URI.create("$baSakUrl/api/behandlinger/$behandlingId/steg/behandlingsresultat")

        return postForEntity(uri, "", headers)
    }

    fun henleggSøknad(
        behandlingId: Long,
        restHenleggBehandlingInfo: RestHenleggBehandlingInfo,
    ): Ressurs<RestUtvidetBehandling> {
        val uri = URI.create("$baSakUrl/api/behandlinger/$behandlingId/steg/henlegg")
        return putForEntity(uri, restHenleggBehandlingInfo, headers)
    }

    fun hentBehandlingslogg(behandlingId: Long): Ressurs<List<Logg>> {
        val uri = URI.create("$baSakUrl/api/logg/$behandlingId")
        return getForEntity(uri, headers)
    }

    fun opprettBehandling(
        søkersIdent: String,
        behandlingType: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
        behandlingÅrsak: BehandlingÅrsak = BehandlingÅrsak.SØKNAD,
        behandlingUnderkategori: BehandlingUnderkategori = BehandlingUnderkategori.ORDINÆR,
        fagsakId: Long,
    ): Ressurs<RestUtvidetBehandling> {
        val uri = URI.create("$baSakUrl/api/behandlinger")

        return postForEntity(
            uri,
            NyBehandling(
                kategori = BehandlingKategori.NASJONAL,
                underkategori = behandlingUnderkategori,
                søkersIdent = søkersIdent,
                behandlingType = behandlingType,
                behandlingÅrsak = behandlingÅrsak,
                søknadMottattDato = if (behandlingÅrsak == BehandlingÅrsak.SØKNAD) LocalDate.now() else null,
                fagsakId = fagsakId,
            ),
            headers,
        )
    }

    fun registrererSøknad(
        behandlingId: Long,
        restRegistrerSøknad: RestRegistrerSøknad,
    ): Ressurs<RestUtvidetBehandling> {
        val uri =
            URI.create(encodePath("$baSakUrl/api/behandlinger/$behandlingId/steg/registrer-søknad", "UTF-8"))

        return postForEntity(uri, restRegistrerSøknad, headers)
    }

    fun putVilkår(
        behandlingId: Long,
        vilkårId: Long,
        restPersonResultat: RestPersonResultat,
    ): Ressurs<RestUtvidetBehandling> {
        val uri = URI.create(encodePath("$baSakUrl/api/vilkaarsvurdering/$behandlingId/$vilkårId"))

        return putForEntity(uri, restPersonResultat, headers)
    }

    fun validerVilkårsvurdering(behandlingId: Long): Ressurs<RestUtvidetBehandling> {
        val uri = URI.create(encodePath("$baSakUrl/api/behandlinger/$behandlingId/steg/vilkårsvurdering"))
        return postForEntity(uri, "", headers)
    }

    fun lagreTilbakekrevingOgGåVidereTilNesteSteg(
        behandlingId: Long,
        restTilbakekreving: RestTilbakekreving,
    ): Ressurs<RestUtvidetBehandling> {
        val uri = URI.create("$baSakUrl/api/behandlinger/$behandlingId/steg/tilbakekreving")

        return postForEntity(uri, restTilbakekreving, headers)
    }

    fun oppdaterVedtaksperiodeMedStandardbegrunnelser(
        vedtaksperiodeId: Long,
        restPutVedtaksperiodeMedStandardbegrunnelser: RestPutVedtaksperiodeMedStandardbegrunnelser,
    ): Ressurs<List<RestUtvidetVedtaksperiodeMedBegrunnelser>> {
        val uri = URI.create("$baSakUrl/api/vedtaksperioder/standardbegrunnelser/$vedtaksperiodeId")

        return putForEntity(uri, restPutVedtaksperiodeMedStandardbegrunnelser, headers)
    }

    fun sendTilBeslutter(behandlingId: Long): Ressurs<RestUtvidetBehandling> {
        val uri =
            URI.create("$baSakUrl/api/behandlinger/$behandlingId/steg/send-til-beslutter?behandlendeEnhet=$DEFAULT_JOURNALFØRENDE_ENHET")

        return postForEntity(uri, "", headers)
    }

    fun leggTilEndretUtbetalingAndel(
        behandlingId: Long,
        restEndretUtbetalingAndel: RestEndretUtbetalingAndel,
    ): Ressurs<RestUtvidetBehandling> {
        val uriPost = URI.create("$baSakUrl/api/endretutbetalingandel/$behandlingId")
        val restUtvidetBehandling = postForEntity<Ressurs<RestUtvidetBehandling>>(uriPost, "", headers)

        val endretUtbetalingAndelId =
            restUtvidetBehandling.data!!.endretUtbetalingAndeler.first { it.tom == null && it.fom == null }.id
        val uriPut = URI.create("$baSakUrl/api/endretutbetalingandel/$behandlingId/$endretUtbetalingAndelId")

        return putForEntity(uriPut, restEndretUtbetalingAndel, headers)
    }

    fun fjernEndretUtbetalingAndel(
        behandlingId: Long,
        endretUtbetalingAndelId: Long,
    ): Ressurs<RestUtvidetBehandling> {
        val uri = URI.create("$baSakUrl/api/endretutbetalingandel/$behandlingId/$endretUtbetalingAndelId")

        return deleteForEntity(uri, "", headers)
    }

    fun iverksettVedtak(
        behandlingId: Long,
        restBeslutningPåVedtak: RestBeslutningPåVedtak,
        beslutterHeaders: HttpHeaders,
    ): Ressurs<RestUtvidetBehandling> {
        val uri = URI.create("$baSakUrl/api/behandlinger/$behandlingId/steg/iverksett-vedtak")

        return postForEntity(uri, restBeslutningPåVedtak, beslutterHeaders)
    }

    fun forhaandsvisHenleggelseBrev(behandlingId: Long, manueltBrevRequest: ManueltBrevRequest): Ressurs<ByteArray> {
        val uri = URI.create("$baSakUrl/api/dokument/forhaandsvis-brev/$behandlingId")
        return postForEntity(uri, manueltBrevRequest, headers)
    }

    fun encodePath(path: String): String {
        return encodePath(path, "UTF-8")
    }
}
