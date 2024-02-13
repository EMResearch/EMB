package no.nav.familie.tilbake.behandling

import no.nav.familie.tilbake.behandling.domain.Varsel
import no.nav.familie.tilbake.behandling.domain.Varselsperiode
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.faktaomfeilutbetaling.FaktaFeilutbetalingService
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class VarselService(
    private val behandlingRepository: BehandlingRepository,
    private val kravgrunnlagRepository: KravgrunnlagRepository,
    private val faktaFeilutbetalingService: FaktaFeilutbetalingService,
) {

    fun lagre(behandlingId: UUID, varseltekst: String, varselbeløp: Long) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val varselsperioder: Set<Varselsperiode> = if (kravgrunnlagRepository.existsByBehandlingIdAndAktivTrue(behandlingId)) {
            val perioder = faktaFeilutbetalingService.hentFaktaomfeilutbetaling(behandlingId).feilutbetaltePerioder
            perioder.map { Varselsperiode(fom = it.periode.fom, tom = it.periode.tom) }.toSet()
        } else {
            behandling.aktivtVarsel?.perioder?.map { Varselsperiode(fom = it.fom, tom = it.tom) }?.toSet()
                ?: error("Aktivt varsel har ikke med varselsperioder")
        }

        val varsler = behandling.varsler.map { it.copy(aktiv = false) } +
            Varsel(
                varseltekst = varseltekst,
                varselbeløp = varselbeløp,
                perioder = varselsperioder,
            )
        val copy = behandling.copy(varsler = varsler.toSet())
        behandlingRepository.update(copy)
    }
}
