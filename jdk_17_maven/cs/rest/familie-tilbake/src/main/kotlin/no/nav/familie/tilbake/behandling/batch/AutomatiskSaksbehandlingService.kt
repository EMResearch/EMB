package no.nav.familie.tilbake.behandling.batch

import no.nav.familie.kontrakter.felles.Regelverk
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Saksbehandlingstype
import no.nav.familie.tilbake.behandling.steg.StegService
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevsporingRepository
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.domain.Klassetype
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsbeløp433
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
class AutomatiskSaksbehandlingService(
    private val behandlingRepository: BehandlingRepository,
    private val fagsakRepository: FagsakRepository,
    private val kravgrunnlagRepository: KravgrunnlagRepository,
    private val brevsporingRepository: BrevsporingRepository,
    private val stegService: StegService,
    @Value("\${AUTOMATISK_SAKSBEHANDLING_ALDERGRENSE_BARNETRYGD}")
    private val alderGrenseBarnetrygd: Long,
    @Value("\${AUTOMATISK_SAKSBEHANDLING_ALDERGRENSE_BARNETILSYN}")
    private val alderGrenseBarnetilsyn: Long,
    @Value("\${AUTOMATISK_SAKSBEHANDLING_ALDERGRENSE_OVERGANGSSTØNAD}")
    private val alderGrenseOvergangsstønad: Long,
    @Value("\${AUTOMATISK_SAKSBEHANDLING_ALDERGRENSE_SKOLEPENGER}")
    private val alderGrenseSkolepenger: Long,
    @Value("\${AUTOMATISK_SAKSBEHANDLING_ALDERGRENSE_KONTANTSTØTTE}")
    private val alderGrenseKontantstøtte: Long,
) {

    fun hentAlleBehandlingerSomKanBehandleAutomatisk(): List<Behandling> {
        val behandlinger =
            behandlingRepository.finnAlleBehandlingerKlarForSaksbehandling().filter { it.regelverk != Regelverk.EØS }
        return behandlinger.filter {
            val fagsak = fagsakRepository.findByIdOrThrow(it.fagsakId)
            val bestemtDato = LocalDate.now().minusWeeks(ALDERSGRENSE_I_UKER.getValue(fagsak.ytelsestype))
            val kravgrunnlag = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(it.id)
            val kontrollFelt = LocalDate.parse(
                kravgrunnlag.kontrollfelt,
                DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS"),
            )
            val sumNyttBeløp: BigDecimal = kravgrunnlag.perioder.sumOf { periode ->
                periode.beløp.filter { beløp -> beløp.klassetype == Klassetype.FEIL }
                    .sumOf(Kravgrunnlagsbeløp433::nyttBeløp)
            }

            kontrollFelt < bestemtDato &&
                sumNyttBeløp < Constants.MAKS_FEILUTBETALTBELØP_PER_YTELSE.getValue(fagsak.ytelsestype) &&
                // behandlinger som ikke sendte brev
                !brevsporingRepository.existsByBehandlingId(it.id)
        }
    }

    @Transactional
    fun oppdaterBehandling(behandlingId: UUID) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        behandlingRepository.update(
            behandling.copy(
                saksbehandlingstype = Saksbehandlingstype
                    .AUTOMATISK_IKKE_INNKREVING_LAVT_BELØP,
                ansvarligSaksbehandler = "VL",
            ),
        )
    }

    @Transactional
    fun behandleAutomatisk(behandlingId: UUID) {
        stegService.håndterStegAutomatisk(behandlingId)
    }

    private val ALDERSGRENSE_I_UKER = mapOf(
        Ytelsestype.BARNETRYGD to alderGrenseBarnetrygd,
        Ytelsestype.BARNETILSYN to alderGrenseBarnetilsyn,
        Ytelsestype.OVERGANGSSTØNAD to alderGrenseOvergangsstønad,
        Ytelsestype.SKOLEPENGER to alderGrenseSkolepenger,
        Ytelsestype.KONTANTSTØTTE to alderGrenseKontantstøtte,
    )
}

fun main() {
    val dato = LocalDate.parse(
        "2022-02-10-18.43.15.192503",
        DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS"),
    )
    print(dato < LocalDate.now())
}
