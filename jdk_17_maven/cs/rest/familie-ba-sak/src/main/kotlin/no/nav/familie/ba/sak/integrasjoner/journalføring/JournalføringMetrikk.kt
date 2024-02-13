package no.nav.familie.ba.sak.integrasjoner.journalføring

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.ekstern.restDomene.RestJournalføring
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import org.springframework.stereotype.Component

@Component
class JournalføringMetrikk {

    private val antallGenerellSak: Counter = Metrics.counter("journalfoering.behandling", "behandlingstype", "Fagsak")

    private val antallTilBehandling = BehandlingType.values().associateWith {
        Metrics.counter("journalfoering.behandling", "behandlingstype", it.visningsnavn)
    }

    private val journalpostTittelMap = mapOf(
        "søknad om ordinær barnetrygd" to "Søknad om ordinær barnetrygd",
        "søknad om barnetrygd ordinær" to "Søknad om ordinær barnetrygd",
        "søknad om utvidet barnetrygd" to "Søknad om utvidet barnetrygd",
        "søknad om barnetrygd utvidet" to "Søknad om utvidet barnetrygd",
        "ettersendelse til søknad om ordinær barnetrygd" to "Ettersendelse til søknad om ordinær barnetrygd",
        "ettersendelse til søknad om barnetrygd ordinær" to "Ettersendelse til søknad om ordinær barnetrygd",
        "ettersendelse til søknad om utvidet barnetrygd" to "Ettersendelse til søknad om utvidet barnetrygd",
        "ettersendelse til søknad om barnetrygd utvidet" to "Ettersendelse til søknad om utvidet barnetrygd",
        "tilleggskjema eøs" to "Tilleggskjema EØS",
    )

    private val antallJournalpostTittel = journalpostTittelMap.values.toSet().associateWith {
        Metrics.counter(
            "journalfoering.journalpost",
            "tittel",
            it,
        )
    }

    private val antallJournalpostTittelFritekst =
        Metrics.counter("journalfoering.journalpost", "tittel", "Fritekst")

    fun tellManuellJournalføringsmetrikker(
        journalpost: Journalpost?,
        oppdatert: RestJournalføring,
        behandlinger: List<Behandling>,
    ) {
        if (oppdatert.knyttTilFagsak) {
            behandlinger.forEach {
                antallTilBehandling[it.type]?.increment()
            }
        } else {
            antallGenerellSak.increment()
        }

        val tittelLower = oppdatert.journalpostTittel?.lowercase()
        val kjentTittel = journalpostTittelMap[tittelLower]
        if (kjentTittel != null) {
            antallJournalpostTittel[kjentTittel]?.increment()
        } else {
            antallJournalpostTittelFritekst.increment()
        }
    }
}
