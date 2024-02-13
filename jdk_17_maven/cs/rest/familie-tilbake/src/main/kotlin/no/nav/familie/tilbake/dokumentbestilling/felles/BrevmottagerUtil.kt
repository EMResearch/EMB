package no.nav.familie.tilbake.dokumentbestilling.felles

import no.nav.familie.kontrakter.felles.tilbakekreving.Vergetype
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.kontrakter.felles.tilbakekreving.Verge as VergeDto
import no.nav.familie.tilbake.behandling.domain.Verge as DomainVerge

object BrevmottagerUtil {

    fun getAnnenMottagersNavn(brevmetadata: Brevmetadata): String? {
        if (brevmetadata.annenMottakersNavn != null) {
            return brevmetadata.annenMottakersNavn
        }

        val mottagernavn: String = brevmetadata.mottageradresse.mottagernavn
        val brukernavn = brevmetadata.sakspartsnavn
        val vergenavn = brevmetadata.vergenavn

        return if (mottagernavn.equals(brukernavn, ignoreCase = true)) {
            if (brevmetadata.finnesVerge) vergenavn else ""
        } else {
            brukernavn
        }
    }

    fun getVergenavn(verge: DomainVerge?, adresseinfo: Adresseinfo): String {
        return if (Vergetype.ADVOKAT == verge?.type) {
            adresseinfo.annenMottagersNavn!! // Når verge er advokat, viser vi verge navn som "Virksomhet navn v/ verge navn"
        } else {
            verge?.navn ?: ""
        }
    }

    fun getVergenavn(verge: VergeDto?, adresseinfo: Adresseinfo): String {
        return if (Vergetype.ADVOKAT == verge?.vergetype) {
            adresseinfo.annenMottagersNavn!! // Når verge er advokat, viser vi verge navn som "Virksomhet navn v/ verge navn"
        } else {
            verge?.navn ?: ""
        }
    }

    fun utledBrevmottager(
        behandling: Behandling,
        fagsak: Fagsak,
    ): Brevmottager {
        return if (behandling.harVerge) Brevmottager.VERGE else if (fagsak.institusjon != null) Brevmottager.INSTITUSJON else Brevmottager.BRUKER
    }
}
