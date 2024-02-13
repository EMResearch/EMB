package no.nav.familie.tilbake.faktaomfeilutbetaling

import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.tilbake.api.dto.BehandlingsstegFaktaDto
import no.nav.familie.tilbake.api.dto.FaktaFeilutbetalingDto
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.FaktaFeilutbetaling
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.FaktaFeilutbetalingsperiode
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsestype
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsesundertype
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class FaktaFeilutbetalingService(
    private val behandlingRepository: BehandlingRepository,
    private val faktaFeilutbetalingRepository: FaktaFeilutbetalingRepository,
    private val kravgrunnlagRepository: KravgrunnlagRepository,
) {

    @Transactional(readOnly = true)
    fun hentFaktaomfeilutbetaling(behandlingId: UUID): FaktaFeilutbetalingDto {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val faktaFeilutbetaling = hentAktivFaktaOmFeilutbetaling(behandlingId)
        val kravgrunnlag = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
        return FaktaFeilutbetalingMapper
            .tilRespons(
                faktaFeilutbetaling = faktaFeilutbetaling,
                kravgrunnlag = kravgrunnlag,
                revurderingsvedtaksdato = behandling.aktivFagsystemsbehandling.revurderingsvedtaksdato,
                varsletData = behandling.aktivtVarsel,
                fagsystemsbehandling = behandling.aktivFagsystemsbehandling,
            )
    }

    @Transactional
    fun lagreFaktaomfeilutbetaling(behandlingId: UUID, behandlingsstegFaktaDto: BehandlingsstegFaktaDto) {
        deaktiverEksisterendeFaktaOmFeilutbetaling(behandlingId)

        val feilutbetaltePerioder: Set<FaktaFeilutbetalingsperiode> = behandlingsstegFaktaDto.feilutbetaltePerioder.map {
            FaktaFeilutbetalingsperiode(
                periode = Månedsperiode(it.periode.fom, it.periode.tom),
                hendelsestype = it.hendelsestype,
                hendelsesundertype = it.hendelsesundertype,
            )
        }.toSet()

        faktaFeilutbetalingRepository.insert(
            FaktaFeilutbetaling(
                behandlingId = behandlingId,
                perioder = feilutbetaltePerioder,
                begrunnelse = behandlingsstegFaktaDto.begrunnelse,
            ),
        )
    }

    @Transactional
    fun lagreFastFaktaForAutomatiskSaksbehandling(behandlingId: UUID) {
        val feilutbetaltePerioder = hentFaktaomfeilutbetaling(behandlingId).feilutbetaltePerioder.map {
            FaktaFeilutbetalingsperiode(
                periode = it.periode.toMånedsperiode(),
                hendelsestype = Hendelsestype.ANNET,
                hendelsesundertype = Hendelsesundertype.ANNET_FRITEKST,
            )
        }.toSet()
        faktaFeilutbetalingRepository.insert(
            FaktaFeilutbetaling(
                behandlingId = behandlingId,
                perioder = feilutbetaltePerioder,
                begrunnelse = Constants.AUTOMATISK_SAKSBEHANDLING_BEGUNNLESE,
            ),
        )
    }

    fun hentAktivFaktaOmFeilutbetaling(behandlingId: UUID): FaktaFeilutbetaling? {
        return faktaFeilutbetalingRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
    }

    @Transactional
    fun deaktiverEksisterendeFaktaOmFeilutbetaling(behandlingId: UUID) {
        faktaFeilutbetalingRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)?.copy(aktiv = false)?.let {
            faktaFeilutbetalingRepository.update(it)
        }
    }
}
