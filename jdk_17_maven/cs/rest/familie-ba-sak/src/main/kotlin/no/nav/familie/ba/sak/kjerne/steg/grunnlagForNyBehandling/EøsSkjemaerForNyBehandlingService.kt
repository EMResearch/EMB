package no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling

import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløpService
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.ValutakursService
import org.springframework.stereotype.Service

@Service
class EøsSkjemaerForNyBehandlingService(
    private val kompetanseService: KompetanseService,
    private val utenlandskPeriodebeløpService: UtenlandskPeriodebeløpService,
    private val valutakursService: ValutakursService,
) {

    fun kopierEøsSkjemaer(behandlingId: BehandlingId, forrigeBehandlingSomErVedtattId: BehandlingId?) {
        if (forrigeBehandlingSomErVedtattId != null) {
            kompetanseService.kopierOgErstattKompetanser(
                fraBehandlingId = forrigeBehandlingSomErVedtattId,
                tilBehandlingId = behandlingId,
            )
            utenlandskPeriodebeløpService.kopierOgErstattUtenlandskPeriodebeløp(
                fraBehandlingId = forrigeBehandlingSomErVedtattId,
                tilBehandlingId = behandlingId,
            )
            valutakursService.kopierOgErstattValutakurser(
                fraBehandlingId = forrigeBehandlingSomErVedtattId,
                tilBehandlingId = behandlingId,
            )
        }
    }
}
