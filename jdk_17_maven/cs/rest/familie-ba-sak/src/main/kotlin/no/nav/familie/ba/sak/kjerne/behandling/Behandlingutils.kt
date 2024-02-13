package no.nav.familie.ba.sak.kjerne.behandling

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import java.time.YearMonth

object Behandlingutils {

    fun hentSisteBehandlingSomErIverksatt(iverksatteBehandlinger: List<Behandling>): Behandling? {
        return iverksatteBehandlinger
            .filter { it.steg == StegType.BEHANDLING_AVSLUTTET }
            .maxByOrNull { it.aktivertTidspunkt }
    }

    fun hentForrigeBehandlingSomErVedtatt(
        behandlinger: List<Behandling>,
        behandlingFørFølgende: Behandling,
    ): Behandling? {
        return behandlinger
            .filter { it.aktivertTidspunkt.isBefore(behandlingFørFølgende.aktivertTidspunkt) && it.steg == StegType.BEHANDLING_AVSLUTTET && !it.erHenlagt() }
            .maxByOrNull { it.aktivertTidspunkt }
    }

    fun hentForrigeIverksatteBehandling(
        iverksatteBehandlinger: List<Behandling>,
        behandlingFørFølgende: Behandling,
    ): Behandling? {
        return hentIverksatteBehandlinger(
            iverksatteBehandlinger,
            behandlingFørFølgende,
        ).maxByOrNull { it.aktivertTidspunkt }
    }

    fun hentIverksatteBehandlinger(
        iverksatteBehandlinger: List<Behandling>,
        behandlingFørFølgende: Behandling,
    ): List<Behandling> {
        return iverksatteBehandlinger
            .filter { it.aktivertTidspunkt.isBefore(behandlingFørFølgende.aktivertTidspunkt) && it.steg == StegType.BEHANDLING_AVSLUTTET }
    }

    fun harBehandlingsårsakAlleredeKjørt(
        behandlingÅrsak: BehandlingÅrsak,
        behandlinger: List<Behandling>,
        måned: YearMonth,
    ): Boolean {
        return behandlinger.any {
            it.aktivertTidspunkt.toLocalDate().toYearMonth() == måned && it.opprettetÅrsak == behandlingÅrsak
        }
    }

    fun validerhenleggelsestype(henleggÅrsak: HenleggÅrsak, tekniskVedlikeholdToggel: Boolean, behandlingId: Long) {
        if (!tekniskVedlikeholdToggel && henleggÅrsak == HenleggÅrsak.TEKNISK_VEDLIKEHOLD) {
            throw Feil(
                "Teknisk vedlikehold henleggele er ikke påslått for " +
                    "${SikkerhetContext.hentSaksbehandlerNavn()}. Kan ikke henlegge behandling $behandlingId.",
            )
        }
    }

    fun validerBehandlingIkkeSendtTilEksterneTjenester(behandling: Behandling) {
        if (behandling.harUtførtSteg(StegType.IVERKSETT_MOT_OPPDRAG)) {
            throw FunksjonellFeil("Kan ikke henlegge behandlingen. Den er allerede sendt til økonomi.")
        }
        if (behandling.harUtførtSteg(StegType.DISTRIBUER_VEDTAKSBREV)) {
            throw FunksjonellFeil("Kan ikke henlegge behandlingen. Brev er allerede distribuert.")
        }
        if (behandling.harUtførtSteg(StegType.JOURNALFØR_VEDTAKSBREV)) {
            throw FunksjonellFeil("Kan ikke henlegge behandlingen. Brev er allerede journalført.")
        }
    }
}
