package no.nav.familie.ba.sak.kjerne.autovedtak.omregning

import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakStegService
import no.nav.familie.ba.sak.kjerne.autovedtak.OmregningBrevData
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.StartSatsendring
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.småbarnstillegg.PeriodeOvergangsstønadGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.småbarnstillegg.PeriodeOvergangsstønadGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.prosessering.error.RekjørSenereException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.YearMonth

@Service
class AutobrevOpphørSmåbarnstilleggService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val autovedtakBrevService: AutovedtakBrevService,
    private val autovedtakStegService: AutovedtakStegService,
    private val persongrunnlagService: PersongrunnlagService,
    private val periodeOvergangsstønadGrunnlagRepository: PeriodeOvergangsstønadGrunnlagRepository,
    private val startSatsendring: StartSatsendring,
) {
    @Transactional
    fun kjørBehandlingOgSendBrevForOpphørAvSmåbarnstillegg(fagsakId: Long) {
        val behandling =
            behandlingHentOgPersisterService.finnAktivForFagsak(fagsakId = fagsakId)
                ?: error("Fant ikke aktiv behandling")

        val personopplysningGrunnlag: PersonopplysningGrunnlag =
            persongrunnlagService.hentAktivThrows(behandling.id)

        val listePeriodeOvergangsstønadGrunnlag: List<PeriodeOvergangsstønadGrunnlag> =
            periodeOvergangsstønadGrunnlagRepository.findByBehandlingId(behandlingId = behandling.id)

        val behandlingsårsak = BehandlingÅrsak.OMREGNING_SMÅBARNSTILLEGG

        val standardbegrunnelse =
            if (yngsteBarnFylteTreÅrForrigeMåned(personopplysningGrunnlag = personopplysningGrunnlag)) {
                Standardbegrunnelse.REDUKSJON_SMÅBARNSTILLEGG_IKKE_LENGER_BARN_UNDER_TRE_ÅR
            } else if (overgangstønadOpphørteForrigeMåned(listePeriodeOvergangsstønadGrunnlag = listePeriodeOvergangsstønadGrunnlag)) {
                Standardbegrunnelse.REDUKSJON_SMÅBARNSTILLEGG_IKKE_LENGER_FULL_OVERGANGSSTØNAD
            } else {
                logger.info(
                    "For fagsak $fagsakId ble verken yngste barn 3 år forrige måned eller har overgangsstønad som utløper denne måneden. " +
                        "Avbryter sending av autobrev for opphør av småbarnstillegg.",
                )
                return
            }

        if (!autovedtakBrevService.skalAutobrevBehandlingOpprettes(
                fagsakId = fagsakId,
                behandlingsårsak = behandlingsårsak,
                standardbegrunnelser = listOf(standardbegrunnelse),
            )
        ) {
            return
        }

        if (startSatsendring.sjekkOgOpprettSatsendringVedGammelSats(fagsakId)) {
            throw RekjørSenereException(
                "Satsedring skal kjøre ferdig før man behandler autobrev småbarnstillegg",
                LocalDateTime.now().plusHours(1),
            )
        }

        autovedtakStegService.kjørBehandlingOmregning(
            mottakersAktør = behandling.fagsak.aktør,
            behandlingsdata = OmregningBrevData(
                aktør = behandling.fagsak.aktør,
                behandlingsårsak = behandlingsårsak,
                standardbegrunnelse = standardbegrunnelse,
                fagsakId = fagsakId,
            ),
        )
    }

    fun overgangstønadOpphørteForrigeMåned(listePeriodeOvergangsstønadGrunnlag: List<PeriodeOvergangsstønadGrunnlag>): Boolean =
        listePeriodeOvergangsstønadGrunnlag.any { periodeOvergangsstønadGrunnlag ->
            periodeOvergangsstønadGrunnlag.tom.toYearMonth() == YearMonth.now().minusMonths(1)
        } && listePeriodeOvergangsstønadGrunnlag.none { periodeOvergangsstønadGrunnlag -> periodeOvergangsstønadGrunnlag.fom.toYearMonth() == YearMonth.now() }

    fun yngsteBarnFylteTreÅrForrigeMåned(personopplysningGrunnlag: PersonopplysningGrunnlag): Boolean {
        val yngsteBarnSinFødselsdato: YearMonth =
            personopplysningGrunnlag.yngsteBarnSinFødselsdato.toYearMonth()

        return yngsteBarnSinFødselsdato.plusYears(3) == YearMonth.now().minusMonths(1)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AutobrevOpphørSmåbarnstilleggService::class.java)
    }
}
