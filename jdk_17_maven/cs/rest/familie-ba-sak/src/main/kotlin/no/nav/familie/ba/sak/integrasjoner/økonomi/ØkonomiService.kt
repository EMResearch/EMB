package no.nav.familie.ba.sak.integrasjoner.økonomi

import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValideringService
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.utbetalingsoppdrag
import no.nav.familie.ba.sak.kjerne.simulering.KontrollerNyUtbetalingsgeneratorService
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.http.client.RessursException
import no.nav.familie.kontrakter.felles.oppdrag.OppdragId
import no.nav.familie.kontrakter.felles.oppdrag.OppdragStatus
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.unleash.UnleashContextFields
import no.nav.familie.unleash.UnleashService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

@Service
class ØkonomiService(
    private val økonomiKlient: ØkonomiKlient,
    private val beregningService: BeregningService,
    private val tilkjentYtelseValideringService: TilkjentYtelseValideringService,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val kontrollerNyUtbetalingsgeneratorService: KontrollerNyUtbetalingsgeneratorService,
    private val utbetalingsoppdragGeneratorService: UtbetalingsoppdragGeneratorService,
    private val unleashService: UnleashService,

) {
    private val sammeOppdragSendtKonflikt = Metrics.counter("familie.ba.sak.samme.oppdrag.sendt.konflikt")

    fun oppdaterTilkjentYtelseMedUtbetalingsoppdragOgIverksett(
        vedtak: Vedtak,
        saksbehandlerId: String,
        andelTilkjentYtelseForUtbetalingsoppdragFactory: AndelTilkjentYtelseForUtbetalingsoppdragFactory,
    ): Utbetalingsoppdrag {
        val oppdatertBehandling = vedtak.behandling

        val brukNyUtbetalingsoppdragGenerator = unleashService.isEnabled(
            FeatureToggleConfig.BRUK_NY_UTBETALINGSGENERATOR,
            mapOf(UnleashContextFields.FAGSAK_ID to vedtak.behandling.fagsak.id.toString()),
        )

        if (!brukNyUtbetalingsoppdragGenerator) {
            kontrollerNyUtbetalingsgeneratorService.kontrollerNyUtbetalingsgenerator(
                vedtak = vedtak,
                saksbehandlerId = saksbehandlerId,
            )
        }

        val utbetalingsoppdrag: Utbetalingsoppdrag =
            if (brukNyUtbetalingsoppdragGenerator) {
                logger.info("Bruker ny utbetalingsgenerator for behandling ${vedtak.behandling.id}")
                utbetalingsoppdragGeneratorService.genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                    vedtak,
                    saksbehandlerId,
                ).utbetalingsoppdrag.tilRestUtbetalingsoppdrag()
            } else {
                val utbetalingsoppdrag =
                    utbetalingsoppdragGeneratorService.genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                        vedtak,
                        saksbehandlerId,
                        andelTilkjentYtelseForUtbetalingsoppdragFactory,
                    )

                beregningService.oppdaterTilkjentYtelseMedUtbetalingsoppdrag(oppdatertBehandling, utbetalingsoppdrag)
                utbetalingsoppdrag
            }

        tilkjentYtelseValideringService.validerIngenAndelerTilkjentYtelseMedSammeOffsetIBehandling(behandlingId = vedtak.behandling.id)

        iverksettOppdrag(utbetalingsoppdrag, oppdatertBehandling.id)
        return utbetalingsoppdrag
    }

    private fun iverksettOppdrag(utbetalingsoppdrag: Utbetalingsoppdrag, behandlingId: Long) {
        if (!utbetalingsoppdrag.skalIverksettesMotOppdrag()) {
            logger.warn(
                "Iverksetter ikke noe mot oppdrag. " +
                    "Ingen utbetalingsperioder for behandlingId=$behandlingId",
            )
            return
        }
        try {
            økonomiKlient.iverksettOppdrag(utbetalingsoppdrag)
        } catch (exception: Exception) {
            if (exception is RessursException &&
                exception.httpStatus == HttpStatus.CONFLICT
            ) {
                sammeOppdragSendtKonflikt.increment()
                logger.info("Bypasset feil med HttpKode 409 ved iverksetting mot økonomi for fagsak ${utbetalingsoppdrag.saksnummer}")
                return
            } else {
                throw exception
            }
        }
    }

    fun hentStatus(oppdragId: OppdragId, behandlingId: Long): OppdragStatus =
        if (tilkjentYtelseRepository.findByBehandling(behandlingId).skalIverksettesMotOppdrag()) {
            økonomiKlient.hentStatus(oppdragId)
        } else {
            OppdragStatus.KVITTERT_OK
        }

    companion object {

        val logger = LoggerFactory.getLogger(ØkonomiService::class.java)
    }
}

fun Utbetalingsoppdrag.skalIverksettesMotOppdrag(): Boolean = utbetalingsperiode.isNotEmpty()

private fun TilkjentYtelse.skalIverksettesMotOppdrag(): Boolean =
    this.utbetalingsoppdrag()?.skalIverksettesMotOppdrag() ?: false
