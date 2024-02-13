package no.nav.familie.ba.sak.kjerne.vedtak.feilutbetaltValuta

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.config.FeatureToggleConfig.Companion.FEILUTBETALT_VALUTA_PR_MND
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.ekstern.restDomene.RestFeilutbetaltValuta
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FeilutbetaltValutaService(
    @Autowired
    private val feilutbetaltValutaRepository: FeilutbetaltValutaRepository,

    @Autowired
    private val loggService: LoggService,

    @Autowired
    private val featureToggleService: FeatureToggleService,

) {

    private fun finnFeilutbetaltValutaThrows(id: Long): FeilutbetaltValuta {
        return feilutbetaltValutaRepository.finnFeilutbetaltValuta(id) ?: throw Feil("Finner ikke feilutbetalt valuta med id=$id")
    }

    @Transactional
    fun leggTilFeilutbetaltValutaPeriode(feilutbetaltValuta: RestFeilutbetaltValuta, behandlingId: Long): Long {
        val lagret = feilutbetaltValutaRepository.save(
            FeilutbetaltValuta(
                behandlingId = behandlingId,
                fom = feilutbetaltValuta.fom,
                tom = feilutbetaltValuta.tom,
                feilutbetaltBeløp = feilutbetaltValuta.feilutbetaltBeløp,
                erPerMåned = feilutbetaltValuta.erPerMåned ?: featureToggleService.isEnabled(FEILUTBETALT_VALUTA_PR_MND),
            ),
        )
        loggService.loggFeilutbetaltValutaPeriodeLagtTil(behandlingId = behandlingId, feilutbetaltValuta = lagret)
        return lagret.id
    }

    @Transactional
    fun fjernFeilutbetaltValutaPeriode(id: Long, behandlingId: Long) {
        loggService.loggFeilutbetaltValutaPeriodeFjernet(
            behandlingId = behandlingId,
            feilutbetaltValuta = finnFeilutbetaltValutaThrows(id),
        )
        feilutbetaltValutaRepository.deleteById(id)
    }

    fun hentFeilutbetaltValutaPerioder(behandlingId: Long) =
        feilutbetaltValutaRepository.finnFeilutbetaltValutaForBehandling(behandlingId = behandlingId).map { tilRest(it) }

    private fun tilRest(it: FeilutbetaltValuta) =
        RestFeilutbetaltValuta(
            id = it.id,
            fom = it.fom,
            tom = it.tom,
            feilutbetaltBeløp = it.feilutbetaltBeløp,
            erPerMåned = it.erPerMåned,
        )

    @Transactional
    fun oppdatertFeilutbetaltValutaPeriode(feilutbetaltValuta: RestFeilutbetaltValuta, id: Long) {
        val periode = feilutbetaltValutaRepository.findById(id).orElseThrow { Feil("Finner ikke feilutbetalt valuta med id=${feilutbetaltValuta.id}") }

        periode.fom = feilutbetaltValuta.fom
        periode.tom = feilutbetaltValuta.tom
        periode.feilutbetaltBeløp = feilutbetaltValuta.feilutbetaltBeløp
        periode.erPerMåned = feilutbetaltValuta.erPerMåned ?: featureToggleService.isEnabled(FEILUTBETALT_VALUTA_PR_MND)
    }
}
