package no.nav.familie.tilbake.kravgrunnlag.batch

import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype.BARNETILSYN
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype.BARNETRYGD
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype.KONTANTSTØTTE
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype.OVERGANGSSTØNAD
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype.SKOLEPENGER
import no.nav.familie.leader.LeaderClient
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.behandling.FagsystemUtil
import no.nav.familie.tilbake.config.PropertyName
import no.nav.familie.tilbake.kravgrunnlag.ØkonomiXmlMottattService
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.data.domain.Pageable
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.Properties

@Service
class HåndterGamleKravgrunnlagBatch(
    private val mottattXmlService: ØkonomiXmlMottattService,
    private val taskService: TaskService,
    private val environment: Environment,
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Scheduled(cron = "\${CRON_HÅNDTER_GAMMEL_KRAVGRUNNLAG}")
    @Transactional
    fun utfør() {
        if (LeaderClient.isLeader() != true && !environment.activeProfiles.any {
                it.contains("local") ||
                    it.contains("integrasjonstest")
            }
        ) {
            return
        }

        logger.info("Starter HåndterGamleKravgrunnlagBatch..")
        logger.info("Henter kravgrunnlag som er eldre enn $ALDERSGRENSE_I_UKER uker")
        val mottattXmlIdsMedYtelse = mottattXmlService.hentFrakobletGamleMottattXmlIds(
            beregnBestemtDato(BARNETRYGD),
            beregnBestemtDato(BARNETILSYN),
            beregnBestemtDato(OVERGANGSSTØNAD),
            beregnBestemtDato(SKOLEPENGER),
            beregnBestemtDato(KONTANTSTØTTE),
        )

        if (mottattXmlIdsMedYtelse.isNotEmpty()) {
            logger.info(
                "Det finnes ${mottattXmlIdsMedYtelse.size} kravgrunnlag som er eldre enn " +
                    "$ALDERSGRENSE_I_UKER uker fra dagens dato",
            )

            val alleFeiledeTasker = taskService.finnTasksMedStatus(
                listOf(
                    Status.FEILET,
                    Status.KLAR_TIL_PLUKK,
                    Status.MANUELL_OPPFØLGING,
                ),
                Pageable.unpaged(),
            )
            mottattXmlIdsMedYtelse.forEach { mottattXmlIdOgYtelse ->
                val finnesTask = alleFeiledeTasker.any {
                    it.payload == mottattXmlIdOgYtelse.id.toString() &&
                        (it.type == HåndterGammelKravgrunnlagTask.TYPE || it.type == HentFagsystemsbehandlingTask.TYPE)
                }
                if (!finnesTask) {
                    val fagsystem = FagsystemUtil.hentFagsystemFraYtelsestype(mottattXmlIdOgYtelse.ytelsestype)
                    taskService.save(
                        Task(
                            type = HentFagsystemsbehandlingTask.TYPE,
                            payload = mottattXmlIdOgYtelse.id.toString(),
                            properties = Properties().apply {
                                setProperty(
                                    PropertyName.FAGSYSTEM,
                                    fagsystem.name,
                                )
                            },
                        ),
                    )
                } else {
                    logger.info(
                        "Det finnes allerede en feilet HåndterGammelKravgrunnlagTask " +
                            "eller HentFagsystemsbehandlingTask " +
                            "på det samme kravgrunnlaget med id ${mottattXmlIdOgYtelse.id}",
                    )
                }
            }
        } else {
            logger.info("Det finnes ingen kravgrunnlag som er eldre enn $ALDERSGRENSE_I_UKER uker fra dagens dato")
        }
        logger.info("Stopper HåndterGamleKravgrunnlagBatch..")
    }

    private fun beregnBestemtDato(ytelsestype: Ytelsestype): LocalDate {
        return LocalDate.now().minusWeeks(ALDERSGRENSE_I_UKER.getValue(ytelsestype))
    }

    companion object {

        val ALDERSGRENSE_I_UKER = mapOf<Ytelsestype, Long>(
            BARNETRYGD to 8,
            BARNETILSYN to 8,
            OVERGANGSSTØNAD to 8,
            SKOLEPENGER to 8,
            KONTANTSTØTTE to 8,
        )
    }
}
