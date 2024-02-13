package no.nav.familie.ba.sak.kjerne.småbarnstilleggkorrigering

import jakarta.transaction.Transactional
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.opprettBooleanTidslinje
import no.nav.familie.ba.sak.common.tilMånedÅr
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.AndelTilkjentYtelseForTidslinje
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.beregning.oppdaterTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.satstypeTidslinje
import no.nav.familie.ba.sak.kjerne.beregning.tilAndelerTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.tilTryggTidslinjeForSøkersYtelse
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.harIkkeOverlappMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.harOverlappMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerUtenNullMed
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.YearMonth

@Service
class SmåbarnstilleggKorrigeringService(
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val loggService: LoggService,
) {
    @Transactional
    fun leggTilSmåbarnstilleggPåBehandling(årMåned: YearMonth, behandling: Behandling): List<AndelTilkjentYtelse> {
        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandling(behandlingId = behandling.id)
        val andelTilkjentYtelser = tilkjentYtelse.andelerTilkjentYtelse

        val småbarnstilleggTidslinje = andelTilkjentYtelser.tilTryggTidslinjeForSøkersYtelse(YtelseType.SMÅBARNSTILLEGG)
        val skalOpprettesTidslinje = opprettBooleanTidslinje(årMåned, årMåned)

        if (småbarnstilleggTidslinje.harOverlappMed(skalOpprettesTidslinje)) {
            throw FunksjonellFeil("Det er ikke mulig å legge til småbarnstillegg for ${årMåned.tilMånedÅr()} fordi det allerede finnes småbarnstillegg for denne perioden")
        }

        val nyeSmåbarnstillegg = skalOpprettesTidslinje
            .kombinerUtenNullMed(satstypeTidslinje(SatsType.SMA)) { _, sats ->
                AndelTilkjentYtelseForTidslinje(
                    aktør = behandling.fagsak.aktør,
                    ytelseType = YtelseType.SMÅBARNSTILLEGG,
                    prosent = BigDecimal(100),
                    sats = sats,
                    beløp = sats,
                )
            }.tilAndelerTilkjentYtelse(tilkjentYtelse)

        andelTilkjentYtelser.addAll(nyeSmåbarnstillegg)

        loggService.opprettSmåbarnstilleggLogg(behandling, "Småbarnstillegg for ${årMåned.tilMånedÅr()} lagt til")

        return nyeSmåbarnstillegg
    }

    @Transactional
    fun fjernSmåbarnstilleggPåBehandling(årMåned: YearMonth, behandling: Behandling): List<AndelTilkjentYtelse> {
        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandling(behandlingId = behandling.id)

        val småbarnstilleggTidslinje = tilkjentYtelse.andelerTilkjentYtelse
            .tilTryggTidslinjeForSøkersYtelse(YtelseType.SMÅBARNSTILLEGG)
        val skalFjernesTidslinje = opprettBooleanTidslinje(årMåned, årMåned)

        if (småbarnstilleggTidslinje.harIkkeOverlappMed(skalFjernesTidslinje)) {
            throw FunksjonellFeil("Det er ikke mulig å fjerne småbarnstillegg for ${årMåned.tilMånedÅr()} fordi det ikke finnes småbarnstillegg for denne perioden")
        }

        val nyeSmåbarnstilleggAndeler =
            småbarnstilleggTidslinje.kombinerMed(skalFjernesTidslinje) { andel, skalFjernes ->
                when (skalFjernes) {
                    true -> null
                    else -> andel
                }
            }.tilAndelerTilkjentYtelse(tilkjentYtelse)

        val andelerTilkjentYtelserUtenomSmåbarnstillegg = tilkjentYtelse.andelerTilkjentYtelse
            .filter { it.type != YtelseType.SMÅBARNSTILLEGG }

        val oppdaterteAndeler = andelerTilkjentYtelserUtenomSmåbarnstillegg + nyeSmåbarnstilleggAndeler
        tilkjentYtelseRepository.oppdaterTilkjentYtelse(tilkjentYtelse, oppdaterteAndeler)

        loggService.opprettSmåbarnstilleggLogg(behandling, "Småbarnstillegg for ${årMåned.tilMånedÅr()} fjernet")

        return nyeSmåbarnstilleggAndeler
    }
}
