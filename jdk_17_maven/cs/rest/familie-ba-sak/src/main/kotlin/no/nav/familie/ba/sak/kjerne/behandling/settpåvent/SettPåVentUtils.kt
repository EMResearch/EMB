package no.nav.familie.ba.sak.kjerne.behandling.settpåvent

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import java.time.LocalDate

fun validerBehandlingKanSettesPåVent(
    gammelSettPåVent: SettPåVent?,
    frist: LocalDate,
    behandling: Behandling,
) {
    if (gammelSettPåVent != null) {
        throw FunksjonellFeil(
            melding = "Behandling ${behandling.id} er allerede satt på vent.",
            frontendFeilmelding = "Behandlingen er allerede satt på vent.",
        )
    }

    validerFristErFremITiden(behandling, frist)

    if (behandling.status != BehandlingStatus.UTREDES) {
        throw FunksjonellFeil(
            melding = "Behandling ${behandling.id} har status=${behandling.status} og kan ikke settes på vent.",
            frontendFeilmelding = "Behandlingen må ha status utredes for å kunne settes på vent",
        )
    }

    if (!behandling.aktiv) {
        throw Feil(
            "Behandling ${behandling.id} er ikke aktiv og kan ikke settes på vent.",
        )
    }
}

fun validerFristErFremITiden(
    behandling: Behandling,
    frist: LocalDate,
) {
    if (frist.isBefore(LocalDate.now())) {
        throw FunksjonellFeil(
            melding = "Frist for å vente på behandling ${behandling.id} er satt før dagens dato.",
            frontendFeilmelding = "Fristen er satt før dagens dato.",
        )
    }
}

fun validerKanGjenopptaBehandling(behandling: Behandling) {
    val status = behandling.status
    if (status != BehandlingStatus.SATT_PÅ_VENT) {
        val melding = "Behandling ${behandling.id} har status=$status og kan ikke gjenopptas."
        if (status == BehandlingStatus.SATT_PÅ_MASKINELL_VENT) {
            throw FunksjonellFeil(
                melding = melding,
                frontendFeilmelding = "Behandlingen er under maskinell vent, og kan gjenopptas senere.",
            )
        } else {
            throw Feil(
                message = melding,
                frontendFeilmelding = "Behandlingen må ha status satt på vent for å kunne gjenopptas.",
            )
        }
    }
}
