package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.Bruker
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.OppdaterJournalpostRequest
import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.dokarkiv.AvsenderMottaker
import no.nav.familie.kontrakter.felles.journalpost.DokumentInfo
import no.nav.familie.kontrakter.felles.journalpost.Dokumentstatus
import no.nav.familie.kontrakter.felles.journalpost.LogiskVedlegg
import no.nav.familie.kontrakter.felles.journalpost.Sak
import java.time.LocalDateTime

data class RestOppdaterJournalpost(
    val avsender: NavnOgIdent,
    val bruker: NavnOgIdent,
    val datoMottatt: LocalDateTime?,
    val dokumentTittel: String,
    val dokumentInfoId: String,
    val knyttTilFagsak: Boolean,
    val tilknyttedeBehandlingIder: List<String>,
    val eksisterendeLogiskeVedlegg: List<LogiskVedlegg>,
    val logiskeVedlegg: List<LogiskVedlegg>,
    val navIdent: String,
) {
    fun oppdaterMedDokumentOgSak(sak: Sak): OppdaterJournalpostRequest {
        val dokument = DokumentInfo(
            dokumentInfoId = this.dokumentInfoId,
            tittel = this.dokumentTittel,
            brevkode = null,
            dokumentstatus = Dokumentstatus.FERDIGSTILT,
            dokumentvarianter = null,
            logiskeVedlegg = null,
        )

        return OppdaterJournalpostRequest(
            avsenderMottaker = AvsenderMottaker(
                id = this.avsender.id,
                idType = if (this.avsender.id != "") BrukerIdType.FNR else null,
                navn = this.avsender.navn,
            ),
            bruker = Bruker(
                this.bruker.id,
                navn = this.bruker.navn,
            ),
            sak = sak,
            tittel = this.dokumentTittel,
            dokumenter = listOf(dokument),
        )
    }
}

data class NavnOgIdent(
    val navn: String,
    val id: String,
)
