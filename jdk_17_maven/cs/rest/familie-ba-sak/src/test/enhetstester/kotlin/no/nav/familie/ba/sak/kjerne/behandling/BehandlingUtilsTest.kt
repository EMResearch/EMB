package no.nav.familie.ba.sak.kjerne.behandling

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.randomAktør
import no.nav.familie.ba.sak.kjerne.behandling.Behandlingutils.validerBehandlingIkkeSendtTilEksterneTjenester
import no.nav.familie.ba.sak.kjerne.behandling.behandlingstema.bestemKategoriVedOpprettelse
import no.nav.familie.ba.sak.kjerne.behandling.behandlingstema.bestemUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.behandlingstema.utledLøpendeUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.domene.tilstand.BehandlingStegTilstand
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.steg.BehandlingStegStatus
import no.nav.familie.ba.sak.kjerne.steg.StegType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class BehandlingUtilsTest {
    @Test
    fun `Skal velge ordinær ved FGB`() {
        assertEquals(
            BehandlingUnderkategori.ORDINÆR,
            bestemUnderkategori(
                overstyrtUnderkategori = BehandlingUnderkategori.ORDINÆR,
                underkategoriFraLøpendeBehandling = null,
            ),
        )
    }

    @Test
    fun `Skal velge utvidet ved FGB`() {
        assertEquals(
            BehandlingUnderkategori.UTVIDET,
            bestemUnderkategori(
                overstyrtUnderkategori = BehandlingUnderkategori.UTVIDET,
                underkategoriFraLøpendeBehandling = null,
            ),
        )
    }

    @Test
    fun `Skal velge utvidet når løpende er ordinær, og inneværende er utvidet`() {
        assertEquals(
            BehandlingUnderkategori.UTVIDET,
            bestemUnderkategori(
                overstyrtUnderkategori = null,
                underkategoriFraLøpendeBehandling = BehandlingUnderkategori.ORDINÆR,
                underkategoriFraInneværendeBehandling = BehandlingUnderkategori.UTVIDET,
            ),
        )
    }

    @Test
    fun `Skal beholde utvidet når løpende er utvidet, og ny er ordinær`() {
        assertEquals(
            BehandlingUnderkategori.UTVIDET,
            bestemUnderkategori(
                overstyrtUnderkategori = BehandlingUnderkategori.ORDINÆR,
                underkategoriFraLøpendeBehandling = BehandlingUnderkategori.UTVIDET,
                underkategoriFraInneværendeBehandling = BehandlingUnderkategori.ORDINÆR,
            ),
        )
    }

    @Test
    fun `Skal velge utvidet ved RV når FGB er utvidet`() {
        assertEquals(
            BehandlingUnderkategori.UTVIDET,
            bestemUnderkategori(
                overstyrtUnderkategori = BehandlingUnderkategori.ORDINÆR,
                underkategoriFraLøpendeBehandling = BehandlingUnderkategori.UTVIDET,
            ),
        )
    }

    @Test
    fun `Skal velge ordinær ved RV når FGB er ordinær`() {
        assertEquals(
            BehandlingUnderkategori.ORDINÆR,
            bestemUnderkategori(
                overstyrtUnderkategori = BehandlingUnderkategori.ORDINÆR,
                underkategoriFraLøpendeBehandling = BehandlingUnderkategori.ORDINÆR,
            ),
        )
    }

    @Test
    fun `Skal velge utvidet ved RV når FGB er ordinær`() {
        assertEquals(
            BehandlingUnderkategori.UTVIDET,
            bestemUnderkategori(
                overstyrtUnderkategori = BehandlingUnderkategori.UTVIDET,
                underkategoriFraLøpendeBehandling = BehandlingUnderkategori.ORDINÆR,
            ),
        )
    }

    @Test
    fun `Skal velge den løpende underkategorien ved 'endre migreringsdato'`() {
        assertEquals(
            BehandlingUnderkategori.UTVIDET,
            bestemUnderkategori(
                overstyrtUnderkategori = null,
                underkategoriFraLøpendeBehandling = BehandlingUnderkategori.UTVIDET,
            ),
        )
        assertEquals(
            BehandlingUnderkategori.ORDINÆR,
            bestemUnderkategori(
                overstyrtUnderkategori = null,
                underkategoriFraLøpendeBehandling = BehandlingUnderkategori.ORDINÆR,
            ),
        )
    }

    @Test
    fun `Skal returnere utvidet hvis det eksisterer en løpende utvidet-sak`() {
        val søkerAktørId = randomAktør()

        val behandling = lagBehandling()

        val tilkjentYtelse = TilkjentYtelse(
            behandling = behandling,
            opprettetDato = LocalDate.now(),
            endretDato = LocalDate.now(),
        )

        val andelTilkjentYtelse = listOf(
            AndelTilkjentYtelse(
                behandlingId = behandling.id,
                type = YtelseType.UTVIDET_BARNETRYGD,
                tilkjentYtelse = tilkjentYtelse,
                aktør = søkerAktørId,
                kalkulertUtbetalingsbeløp = 1054,
                nasjonaltPeriodebeløp = 1054,
                sats = 123,
                stønadFom = YearMonth.of(2015, 6),
                stønadTom = YearMonth.now().plusYears(5),
                prosent = BigDecimal(2),
            ),
        )

        val løpendeUndekategori = utledLøpendeUnderkategori(andelTilkjentYtelse)

        assertEquals(BehandlingUnderkategori.UTVIDET, løpendeUndekategori)
    }

    @Test
    fun `Skal returnere ordinær hvis det eksisterer en utvidet-sak som er avsluttet`() {
        val søkerAktørId = randomAktør()

        val behandling = lagBehandling()

        val tilkjentYtelse = TilkjentYtelse(
            behandling = behandling,
            opprettetDato = LocalDate.now(),
            endretDato = LocalDate.now(),
        )

        val andelTilkjentYtelse = listOf(
            AndelTilkjentYtelse(
                behandlingId = behandling.id,
                type = YtelseType.UTVIDET_BARNETRYGD,
                tilkjentYtelse = tilkjentYtelse,
                aktør = søkerAktørId,
                kalkulertUtbetalingsbeløp = 1054,
                nasjonaltPeriodebeløp = 1054,
                sats = 123,
                stønadFom = YearMonth.of(2015, 6),
                stønadTom = YearMonth.now().minusYears(1),
                prosent = BigDecimal(2),
            ),
        )

        val løpendeUndekategori = utledLøpendeUnderkategori(andelTilkjentYtelse)

        assertEquals(BehandlingUnderkategori.ORDINÆR, løpendeUndekategori)
    }

    @Test
    fun `Skal returnere ordinær hvis det eksisterer en løpende ordinær-sak`() {
        val søkerAktørId = randomAktør()

        val behandling = lagBehandling()

        val tilkjentYtelse = TilkjentYtelse(
            behandling = behandling,
            opprettetDato = LocalDate.now(),
            endretDato = LocalDate.now(),
        )

        val andelTilkjentYtelse = listOf(
            AndelTilkjentYtelse(
                behandlingId = behandling.id,
                type = YtelseType.ORDINÆR_BARNETRYGD,
                tilkjentYtelse = tilkjentYtelse,
                aktør = søkerAktørId,
                kalkulertUtbetalingsbeløp = 1054,
                nasjonaltPeriodebeløp = 1054,
                sats = 123,
                stønadFom = YearMonth.of(2015, 6),
                stønadTom = YearMonth.now().plusYears(2),
                prosent = BigDecimal(2),
            ),
        )

        val løpendeUndekategori = utledLøpendeUnderkategori(andelTilkjentYtelse)

        assertEquals(BehandlingUnderkategori.ORDINÆR, løpendeUndekategori)
    }

    @Test
    fun `Skal finne ut at omregningsbehandling allerede har kjørt inneværende måned`() {
        val fgb = lagBehandling()
        val omregning6År = lagBehandling(
            årsak = BehandlingÅrsak.OMREGNING_6ÅR,
        )
        val behandlingsårsakHarAlleredeKjørt = Behandlingutils.harBehandlingsårsakAlleredeKjørt(
            behandlingÅrsak = BehandlingÅrsak.OMREGNING_6ÅR,
            behandlinger = listOf(fgb, omregning6År),
            måned = YearMonth.now(),
        )

        assertTrue(behandlingsårsakHarAlleredeKjørt)
    }

    @Test
    fun `Skal finne ut at omregningsbehandling ikke har kjørt inneværende måned`() {
        val fgb = lagBehandling()
        val behandlingsårsakHarAlleredeKjørt = Behandlingutils.harBehandlingsårsakAlleredeKjørt(
            behandlingÅrsak = BehandlingÅrsak.OMREGNING_6ÅR,
            behandlinger = listOf(fgb),
            måned = YearMonth.now(),
        )

        assertFalse(behandlingsårsakHarAlleredeKjørt)
    }

    @Test
    fun `Skal kaste feil etter at vedtaksbrev er distribuert`() {
        val fgb = lagBehandling()
        fgb.behandlingStegTilstand.clear()
        fgb.behandlingStegTilstand.add(
            BehandlingStegTilstand(
                behandlingSteg = StegType.DISTRIBUER_VEDTAKSBREV,
                behandlingStegStatus = BehandlingStegStatus.UTFØRT,
                behandling = fgb,
            ),
        )

        assertThrows<FunksjonellFeil> { validerBehandlingIkkeSendtTilEksterneTjenester(fgb) }
    }

    @Test
    fun `Skal kaste feil etter iverksetting mot økonomi`() {
        val fgb = lagBehandling()
        fgb.behandlingStegTilstand.clear()
        fgb.behandlingStegTilstand.add(
            BehandlingStegTilstand(
                behandlingSteg = StegType.IVERKSETT_MOT_OPPDRAG,
                behandlingStegStatus = BehandlingStegStatus.UTFØRT,
                behandling = fgb,
            ),
        )

        assertThrows<FunksjonellFeil> { validerBehandlingIkkeSendtTilEksterneTjenester(fgb) }
    }

    @Test
    fun `Skal kaste feil etter at brev er journalført`() {
        val fgb = lagBehandling()
        fgb.behandlingStegTilstand.clear()
        fgb.behandlingStegTilstand.add(
            BehandlingStegTilstand(
                behandlingSteg = StegType.JOURNALFØR_VEDTAKSBREV,
                behandlingStegStatus = BehandlingStegStatus.UTFØRT,
                behandling = fgb,
            ),
        )

        assertThrows<FunksjonellFeil> { validerBehandlingIkkeSendtTilEksterneTjenester(fgb) }
    }

    @Test
    fun `skal få gitt behandlingskategori ved opprettelse av FGB`() {
        assertEquals(
            BehandlingKategori.EØS,
            bestemKategoriVedOpprettelse(
                overstyrtKategori = BehandlingKategori.EØS,
                behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                behandlingÅrsak = BehandlingÅrsak.SØKNAD,
                kategoriFraLøpendeBehandling = BehandlingKategori.NASJONAL, // default verdi
            ),
        )
    }

    @Test
    fun `skal få gitt behandlingskategori ved opprettelse av Revurdering med søknad`() {
        assertEquals(
            BehandlingKategori.EØS,
            bestemKategoriVedOpprettelse(
                overstyrtKategori = BehandlingKategori.EØS,
                behandlingType = BehandlingType.REVURDERING,
                behandlingÅrsak = BehandlingÅrsak.SØKNAD,
                kategoriFraLøpendeBehandling = BehandlingKategori.NASJONAL, // default verdi
            ),
        )
    }

    @Test
    fun `skal bruke overstyrt kategori ved opprettelse av migreringsbehandling med helmanuell migrering`() {
        val overstyrtKategori = BehandlingKategori.EØS
        assertEquals(
            overstyrtKategori,
            bestemKategoriVedOpprettelse(
                overstyrtKategori = overstyrtKategori,
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                behandlingÅrsak = BehandlingÅrsak.HELMANUELL_MIGRERING,
                kategoriFraLøpendeBehandling = BehandlingKategori.NASJONAL, // default verdi
            ),
        )
    }

    @Test
    fun `skal få NASJONAL kategori ved opprettelse av automatisk migreringsbehandling `() {
        assertEquals(
            BehandlingKategori.NASJONAL,
            bestemKategoriVedOpprettelse(
                overstyrtKategori = BehandlingKategori.NASJONAL,
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                behandlingÅrsak = BehandlingÅrsak.MIGRERING,
                kategoriFraLøpendeBehandling = BehandlingKategori.NASJONAL, // default verdi
            ),
        )
    }

    @Test
    fun `skal få EØS kategori ved opprettelse av automatisk migreringsbehandling `() {
        assertEquals(
            BehandlingKategori.EØS,
            bestemKategoriVedOpprettelse(
                overstyrtKategori = BehandlingKategori.EØS,
                behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
                behandlingÅrsak = BehandlingÅrsak.MIGRERING,
                kategoriFraLøpendeBehandling = BehandlingKategori.NASJONAL, // default verdi
            ),
        )
    }

    @Test
    fun `skal få EØS kategori ved opprettelse av revurdering når siste behandling er EØS og har løpende utbetaling `() {
        assertEquals(
            BehandlingKategori.EØS,
            bestemKategoriVedOpprettelse(
                overstyrtKategori = null,
                behandlingType = BehandlingType.REVURDERING,
                behandlingÅrsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
                kategoriFraLøpendeBehandling = BehandlingKategori.EØS,
            ),
        )
    }

    @Test
    fun `skal få NASJONAL kategori ved opprettelse av revurdering når siste behandling er NASJONAL og har løpende utbetaling `() {
        assertEquals(
            BehandlingKategori.NASJONAL,
            bestemKategoriVedOpprettelse(
                overstyrtKategori = null,
                behandlingType = BehandlingType.REVURDERING,
                behandlingÅrsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
                kategoriFraLøpendeBehandling = BehandlingKategori.NASJONAL,
            ),
        )
    }
}
