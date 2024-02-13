package no.nav.familie.ba.sak.cucumber

import io.cucumber.datatable.DataTable
import no.nav.familie.ba.sak.cucumber.domeneparser.Domenebegrep
import no.nav.familie.ba.sak.cucumber.domeneparser.parseLong
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag

object ValideringUtil {

    fun assertSjekkBehandlingIder(dataTable: DataTable, utbetalingsoppdrag: Map<Long, Utbetalingsoppdrag>) {
        val eksisterendeBehandlingId = utbetalingsoppdrag.filter {
            it.value.utbetalingsperiode.isNotEmpty()
        }.keys
        val forventedeBehandlingId = dataTable.asMaps().map { parseLong(Domenebegrep.BEHANDLING_ID, it) }.toSet()
        val ukontrollerteBehandlingId = eksisterendeBehandlingId.filterNot { forventedeBehandlingId.contains(it) }
        if (ukontrollerteBehandlingId.isNotEmpty() && erUkontrollerteUtbetalingsoppdragTomme(
                ukontrollerteBehandlingId,
                utbetalingsoppdrag,
            )
        ) {
            error("Har ikke kontrollert behandlingene:$ukontrollerteBehandlingId")
        }
    }

    private fun erUkontrollerteUtbetalingsoppdragTomme(
        ukontrollerteBehandlingId: List<Long>,
        utbetalingsoppdrag: Map<Long, Utbetalingsoppdrag>,
    ): Boolean =
        utbetalingsoppdrag
            .filterKeys {
                ukontrollerteBehandlingId.contains(it)
            }
            .any { it.value.utbetalingsperiode.isNotEmpty() }
}
