package no.nav.familie.tilbake.oppgave

import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgavePrioritet
import no.nav.familie.tilbake.config.FeatureToggleService
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.domain.Klassetype
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
class OppgavePrioritetService(
    private val kravgrunnlagRepository: KravgrunnlagRepository,
    private val featureToggleService: FeatureToggleService,
) {

    fun utledOppgaveprioritet(behandlingId: UUID, oppgave: Oppgave? = null): OppgavePrioritet {
        val finnesKravgrunnlag = kravgrunnlagRepository.existsByBehandlingIdAndAktivTrue(behandlingId)

        return if (finnesKravgrunnlag) {
            val kravgrunnlag = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)

            val feilutbetaltBeløp = utledFeilutbetaling(kravgrunnlag)

            when {
                feilutbetaltBeløp < BigDecimal(10_000) -> OppgavePrioritet.LAV
                feilutbetaltBeløp > BigDecimal(70_000) -> OppgavePrioritet.HOY
                else -> OppgavePrioritet.NORM
            }
        } else {
            oppgave?.prioritet ?: OppgavePrioritet.NORM
        }
    }

    private fun utledFeilutbetaling(kravgrunnlag: Kravgrunnlag431) =
        kravgrunnlag.perioder.sumOf { periode ->
            periode.beløp.filter { beløp -> beløp.klassetype == Klassetype.FEIL }.sumOf { it.nyttBeløp }
        }
}
