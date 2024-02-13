package no.nav.familie.ba.sak.integrasjoner.journalføring

import no.nav.familie.ba.sak.common.MDCOperations
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.DEFAULT_JOURNALFØRENDE_ENHET
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.http.client.RessursException
import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.dokarkiv.AvsenderMottaker
import no.nav.familie.kontrakter.felles.dokarkiv.Dokumenttype
import no.nav.familie.kontrakter.felles.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Dokument
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Filtype
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Førsteside
import no.nav.familie.kontrakter.felles.journalpost.Bruker
import no.nav.familie.kontrakter.felles.journalpost.JournalposterForBrukerRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

@Service
class UtgåendeJournalføringService(
    private val integrasjonClient: IntegrasjonClient,
) {

    fun journalførManueltBrev(
        fnr: String,
        fagsakId: String,
        journalførendeEnhet: String,
        brev: ByteArray,
        dokumenttype: Dokumenttype,
        førsteside: Førsteside?,
        avsenderMottaker: AvsenderMottaker? = null,
        tilManuellMottakerEllerVerge: Boolean = false,
    ): String {
        return journalførDokument(
            fnr = fnr,
            fagsakId = fagsakId,
            journalførendeEnhet = journalførendeEnhet,
            brev = listOf(
                Dokument(
                    dokument = brev,
                    filtype = Filtype.PDFA,
                    dokumenttype = dokumenttype,
                ),
            ),
            førsteside = førsteside,
            avsenderMottaker = avsenderMottaker,
            tilManuellMottakerEllerVerge = tilManuellMottakerEllerVerge,
        )
    }

    fun journalførDokument(
        fnr: String,
        fagsakId: String,
        journalførendeEnhet: String? = null,
        brev: List<Dokument>,
        vedlegg: List<Dokument> = emptyList(),
        førsteside: Førsteside? = null,
        behandlingId: Long? = null,
        avsenderMottaker: AvsenderMottaker? = null,
        tilManuellMottakerEllerVerge: Boolean = false,
    ): String {
        if (journalførendeEnhet == DEFAULT_JOURNALFØRENDE_ENHET) {
            logger.warn("Informasjon om enhet mangler på bruker og er satt til fallback-verdi, $DEFAULT_JOURNALFØRENDE_ENHET")
        }

        val eksternReferanseId =
            genererEksternReferanseIdForJournalpost(fagsakId, behandlingId, tilManuellMottakerEllerVerge)

        val journalpostId = try {
            val journalpost = integrasjonClient.journalførDokument(
                ArkiverDokumentRequest(
                    fnr = fnr,
                    avsenderMottaker = avsenderMottaker,
                    forsøkFerdigstill = true,
                    hoveddokumentvarianter = brev,
                    vedleggsdokumenter = vedlegg,
                    fagsakId = fagsakId,
                    journalførendeEnhet = journalførendeEnhet,
                    førsteside = førsteside,
                    eksternReferanseId = eksternReferanseId,
                ),
            )

            if (!journalpost.ferdigstilt) {
                error("Klarte ikke ferdigstille journalpost med id ${journalpost.journalpostId}")
            }

            journalpost.journalpostId
        } catch (ressursException: RessursException) {
            when (ressursException.httpStatus) {
                HttpStatus.CONFLICT -> {
                    logger.warn(
                        "Klarte ikke journalføre dokument på fagsak=$fagsakId fordi det allerede finnes en journalpost " +
                            "med eksternReferanseId=$eksternReferanseId. Bruker eksisterende journalpost.",
                    )

                    hentEksisterendeJournalpost(eksternReferanseId, fnr)
                }
                else -> throw ressursException
            }
        }

        return journalpostId
    }

    private fun hentEksisterendeJournalpost(
        eksternReferanseId: String,
        fnr: String,
    ): String = integrasjonClient.hentJournalposterForBruker(
        JournalposterForBrukerRequest(
            brukerId = Bruker(id = fnr, type = BrukerIdType.FNR),
            antall = 50,
        ),
    ).single { it.eksternReferanseId == eksternReferanseId }.journalpostId

    fun genererEksternReferanseIdForJournalpost(fagsakId: String, behandlingId: Long?, tilVerge: Boolean) =
        "${fagsakId}_${behandlingId}${if (tilVerge) "_verge" else ""}_${MDCOperations.getCallId()}"

    companion object {

        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}
