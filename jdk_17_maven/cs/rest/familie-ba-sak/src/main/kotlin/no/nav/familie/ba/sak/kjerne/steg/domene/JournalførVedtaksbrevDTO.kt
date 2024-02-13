package no.nav.familie.ba.sak.kjerne.steg.domene

import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.prosessering.domene.Task

data class JournalførVedtaksbrevDTO(val vedtakId: Long, val task: Task)

data class MottakerInfo(
    val brukerId: String,
    val brukerIdType: BrukerIdType?,
    val erInstitusjonVerge: Boolean, // Feltet brukes kun for institiusjon med verge
    val navn: String? = null, // Feltet brukes for å sette riktig mottaker navn når brev sendes både til verge og bruker
    val manuellAdresseInfo: ManuellAdresseInfo? = null,
)

fun MottakerInfo.toList() = listOf(this)

data class ManuellAdresseInfo(
    val adresselinje1: String,
    val adresselinje2: String? = null,
    val postnummer: String,
    val poststed: String,
    val landkode: String,
)
