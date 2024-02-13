package no.nav.familie.ba.sak.integrasjoner.journalføring.domene

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.familie.kontrakter.felles.dokarkiv.AvsenderMottaker
import no.nav.familie.kontrakter.felles.journalpost.DokumentInfo
import no.nav.familie.kontrakter.felles.journalpost.Sak

@JsonInclude(JsonInclude.Include.NON_NULL)
data class OppdaterJournalpostRequest(
    val avsenderMottaker: AvsenderMottaker?,
    val bruker: Bruker,
    val tema: String? = "BAR",
    val tittel: String? = null,
    val sak: Sak? = null,
    val dokumenter: List<DokumentInfo>? = null,
)

class Bruker(
    val id: String,
    val idType: IdType? = IdType.FNR,
    val navn: String,
)

enum class IdType {
    FNR, ORGNR, AKTOERID
}

enum class Sakstype(val type: String) {
    FAGSAK("FAGSAK"),
    GENERELL_SAK("GENERELL_SAK"),
}

enum class FagsakSystem {
    BA,
}
