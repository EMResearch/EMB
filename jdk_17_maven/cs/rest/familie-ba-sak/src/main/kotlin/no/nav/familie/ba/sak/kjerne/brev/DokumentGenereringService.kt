package no.nav.familie.ba.sak.kjerne.brev

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.brev.domene.ManueltBrevRequest
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brev
import no.nav.familie.ba.sak.kjerne.brev.domene.tilBrev
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.sikkerhet.SaksbehandlerContext
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.lang.Exception

@Service
class DokumentGenereringService(
    private val persongrunnlagService: PersongrunnlagService,
    private val brevService: BrevService,
    private val brevKlient: BrevKlient,
    private val integrasjonClient: IntegrasjonClient,
    private val saksbehandlerContext: SaksbehandlerContext,
) {

    fun genererBrevForVedtak(vedtak: Vedtak): ByteArray {
        try {
            if (!vedtak.behandling.skalBehandlesAutomatisk && vedtak.behandling.steg > StegType.BESLUTTE_VEDTAK) {
                throw FunksjonellFeil("Ikke tillatt å generere brev etter at behandlingen er sendt fra beslutter")
            }

            val målform = persongrunnlagService.hentSøkersMålform(vedtak.behandling.id)
            val vedtaksbrev =
                when (vedtak.behandling.opprettetÅrsak) {
                    BehandlingÅrsak.DØDSFALL_BRUKER -> brevService.hentDødsfallbrevData(vedtak)
                    BehandlingÅrsak.KORREKSJON_VEDTAKSBREV -> brevService.hentKorreksjonbrevData(vedtak)
                    else -> brevService.hentVedtaksbrevData(vedtak)
                }
            return brevKlient.genererBrev(målform.tilSanityFormat(), vedtaksbrev)
        } catch (feil: Throwable) {
            if (feil is FunksjonellFeil) throw feil

            throw Feil(
                message = "Klarte ikke generere vedtaksbrev på behandling ${vedtak.behandling}: ${feil.message}",
                frontendFeilmelding = "Det har skjedd en feil, og brevet er ikke sendt. Prøv igjen, og ta kontakt med brukerstøtte hvis problemet vedvarer.",
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                throwable = feil,
            )
        }
    }

    fun genererManueltBrev(
        manueltBrevRequest: ManueltBrevRequest,
        erForhåndsvisning: Boolean = false,
    ): ByteArray {
        try {
            val brev: Brev =
                manueltBrevRequest.tilBrev(saksbehandlerContext.hentSaksbehandlerSignaturTilBrev()) { integrasjonClient.hentLandkoderISO2() }
            return brevKlient.genererBrev(
                målform = manueltBrevRequest.mottakerMålform.tilSanityFormat(),
                brev = brev,
            )
        } catch (exception: Exception) {
            if (exception is Feil || exception is FunksjonellFeil) {
                throw exception
            }

            throw Feil(
                message = "Klarte ikke generere brev for ${manueltBrevRequest.brevmal}. ${exception.message}",
                frontendFeilmelding = "${if (erForhåndsvisning) "Det har skjedd en feil" else "Det har skjedd en feil, og brevet er ikke sendt"}. Prøv igjen, og ta kontakt med brukerstøtte hvis problemet vedvarer.",
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                throwable = exception,
            )
        }
    }
}
