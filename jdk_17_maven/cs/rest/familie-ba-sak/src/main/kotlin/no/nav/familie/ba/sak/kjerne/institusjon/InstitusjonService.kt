package no.nav.familie.ba.sak.kjerne.institusjon

import no.nav.familie.ba.sak.integrasjoner.samhandler.SamhandlerKlient
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.kontrakter.ba.tss.SamhandlerInfo
import org.springframework.stereotype.Service

@Service
class InstitusjonService(
    val fagsakRepository: FagsakRepository,
    val samhandlerKlient: SamhandlerKlient,
    val institusjonRepository: InstitusjonRepository,
) {

    fun hentEllerOpprettInstitusjon(orgNummer: String, tssEksternId: String?): Institusjon {
        return institusjonRepository.findByOrgNummer(orgNummer) ?: institusjonRepository.saveAndFlush(
            Institusjon(
                orgNummer = orgNummer,
                tssEksternId = tssEksternId,
            ),
        )
    }

    fun hentSamhandler(orgNummer: String): SamhandlerInfo {
        return samhandlerKlient.hentSamhandler(orgNummer)
    }

    fun søkSamhandlere(navn: String?, postnummer: String?, område: String?): List<SamhandlerInfo> {
        val komplettSamhandlerListe = mutableListOf<SamhandlerInfo>()
        var side = 0
        do {
            val søkeresultat = samhandlerKlient.søkSamhandlere(navn, postnummer, område, side)
            side++
            komplettSamhandlerListe.addAll(søkeresultat.samhandlere)
        } while (søkeresultat.finnesMerInfo)

        return komplettSamhandlerListe
    }
}
