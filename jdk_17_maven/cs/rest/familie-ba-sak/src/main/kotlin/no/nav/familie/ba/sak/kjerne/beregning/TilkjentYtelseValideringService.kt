package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValidering.finnAktørIderMedUgyldigEtterbetalingsperiode
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.barn
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class TilkjentYtelseValideringService(
    private val totrinnskontrollService: TotrinnskontrollService,
    private val beregningService: BeregningService,
    private val persongrunnlagService: PersongrunnlagService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
) {
    fun validerAtIngenUtbetalingerOverstiger100Prosent(behandling: Behandling) {
        if (behandling.erMigrering() || behandling.erTekniskEndring() || behandling.erSatsendring()) return
        val totrinnskontroll = totrinnskontrollService.hentAktivForBehandling(behandling.id)

        if (totrinnskontroll?.godkjent == true) {
            validerAtBarnIkkeFårFlereUtbetalingerSammePeriode(behandling)
        }
    }

    fun validerAtBarnIkkeFårFlereUtbetalingerSammePeriode(behandling: Behandling) {
        val tilkjentYtelse = beregningService.hentTilkjentYtelseForBehandling(behandlingId = behandling.id)

        val søkerOgBarn = persongrunnlagService.hentSøkerOgBarnPåBehandlingThrows(behandlingId = behandling.id)

        val barnMedAndreRelevanteTilkjentYtelser = søkerOgBarn.barn().map {
            Pair(
                it,
                beregningService.hentRelevanteTilkjentYtelserForBarn(it.aktør, behandling.fagsak.id),
            )
        }

        secureLogger.info("Andeler tilkjent ytelse i inneværende behandling: " + tilkjentYtelse.andelerTilkjentYtelse)
        secureLogger.info(
            "Barn og deres andeler tilkjent ytelse fra andre fagsaker: " + barnMedAndreRelevanteTilkjentYtelser.map {
                "${it.first} -> ${it.second}"
            },
        )

        TilkjentYtelseValidering.validerAtBarnIkkeFårFlereUtbetalingerSammePeriode(
            behandlendeBehandlingTilkjentYtelse = tilkjentYtelse,
            barnMedAndreRelevanteTilkjentYtelser = barnMedAndreRelevanteTilkjentYtelser,
            søkerOgBarn = søkerOgBarn,
        )
    }

    fun validerIngenAndelerTilkjentYtelseMedSammeOffsetIBehandling(behandlingId: Long) {
        val tilkjentYtelse = beregningService.hentTilkjentYtelseForBehandling(behandlingId = behandlingId)

        if (tilkjentYtelse.harAndelerTilkjentYtelseMedSammeOffset()) {
            secureLogger.info("Behandling har flere andeler med likt offset: ${tilkjentYtelse.andelerTilkjentYtelse}")
            throw Feil("Behandling $behandlingId har andel tilkjent ytelse med offset lik en annen andel i behandlingen.")
        }
    }

    private fun TilkjentYtelse.harAndelerTilkjentYtelseMedSammeOffset(): Boolean {
        val periodeOffsetForAndeler = this.andelerTilkjentYtelse.mapNotNull { it.periodeOffset }

        return periodeOffsetForAndeler.size != periodeOffsetForAndeler.distinct().size
    }

    fun barnetrygdLøperForAnnenForelder(behandling: Behandling, barna: List<Person>): Boolean {
        return barna.any {
            beregningService.hentRelevanteTilkjentYtelserForBarn(barnAktør = it.aktør, fagsakId = behandling.fagsak.id)
                .isNotEmpty()
        }
    }

    fun finnAktørerMedUgyldigEtterbetalingsperiode(
        behandlingId: Long,
    ): List<Aktør> {
        val tilkjentYtelse = beregningService.hentTilkjentYtelseForBehandling(behandlingId = behandlingId)

        val forrigeBehandling = behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(
            behandling = behandlingHentOgPersisterService.hent(behandlingId),
        )
        val forrigeAndelerTilkjentYtelse =
            forrigeBehandling?.let { beregningService.hentOptionalTilkjentYtelseForBehandling(behandlingId = it.id) }
                ?.andelerTilkjentYtelse

        return finnAktørIderMedUgyldigEtterbetalingsperiode(
            forrigeAndelerTilkjentYtelse = forrigeAndelerTilkjentYtelse ?: emptyList(),
            andelerTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.toList(),
            kravDato = tilkjentYtelse.behandling.opprettetTidspunkt,
        )
    }

    companion object {

        val logger = LoggerFactory.getLogger(TilkjentYtelseValideringService::class.java)
    }
}
