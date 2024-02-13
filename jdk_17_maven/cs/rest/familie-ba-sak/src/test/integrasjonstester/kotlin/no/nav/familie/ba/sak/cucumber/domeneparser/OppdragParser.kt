package no.nav.familie.ba.sak.cucumber.domeneparser

import io.cucumber.datatable.DataTable
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.cucumber.domeneparser.DomeneparserUtil.groupByBehandlingId
import no.nav.familie.ba.sak.integrasjoner.økonomi.YtelsetypeBA
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.Personident
import no.nav.familie.felles.utbetalingsgenerator.domain.AndelDataLongId
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import org.assertj.core.api.Assertions.assertThat
import java.time.LocalDate

object OppdragParser {

    fun mapTilkjentYtelse(
        dataTable: DataTable,
        behandlinger: Map<Long, Behandling>,
        tilkjentYtelseId: Long = 0,
    ): List<TilkjentYtelse> {
        var index = 0
        var tilkjentYtelseId = tilkjentYtelseId
        return dataTable.groupByBehandlingId().map { (behandlingId, rader) ->

            val behandling = behandlinger.getValue(behandlingId)
            val andeler = parseAndelder(behandling, rader, index)
            index += andeler.size

            val tilkjentYtelse = TilkjentYtelse(
                id = tilkjentYtelseId++,
                behandling = behandling,
                stønadFom = null,
                stønadTom = null,
                opphørFom = null,
                opprettetDato = LocalDate.now(),
                endretDato = LocalDate.now(),
                utbetalingsoppdrag = null,
                andelerTilkjentYtelse = andeler,
            )
            andeler.forEach { it.tilkjentYtelse = tilkjentYtelse }

            tilkjentYtelse
        }
    }

    private fun parseAndelder(
        behandling: Behandling,
        rader: List<Map<String, String>>,
        forrigeAndelId: Int,
    ): MutableSet<AndelTilkjentYtelse> {
        val erUtenAndeler = (parseValgfriBoolean(DomenebegrepTilkjentYtelse.UTEN_ANDELER, rader.first()) ?: false)
        var andelId = forrigeAndelId
        return if (erUtenAndeler) {
            emptySet<AndelTilkjentYtelse>().toMutableSet()
        } else {
            rader.map { mapAndelTilkjentYtelse(it, behandling, andelId++) }.toMutableSet()
        }
    }

    fun mapForventetUtbetalingsoppdrag(
        dataTable: DataTable,
    ): List<ForventetUtbetalingsoppdrag> {
        return dataTable.groupByBehandlingId().map { (behandlingId, rader) ->
            val rad = rader.first()
            validerAlleKodeEndringerLike(rader)
            ForventetUtbetalingsoppdrag(
                behandlingId = behandlingId,
                kodeEndring = parseEnum(DomenebegrepUtbetalingsoppdrag.KODE_ENDRING, rad),
                utbetalingsperiode = rader.map { mapForventetUtbetalingsperiode(it) },
            )
        }
    }

    fun mapForventetBeståendeAndeler(dataTable: DataTable): List<AndelDataLongId> {
        return dataTable.asMaps().map { rad ->
            AndelDataLongId(
                id = parseLong(Domenebegrep.ID, rad),
                fom = parseÅrMåned(Domenebegrep.FRA_DATO, rad),
                tom = parseÅrMåned(Domenebegrep.TIL_DATO, rad),
                beløp = parseInt(DomenebegrepTilkjentYtelse.BELØP, rad),
                personIdent = lagFødselsnummer("1"),
                type = YtelsetypeBA.ORDINÆR_BARNETRYGD,
                periodeId = parseValgfriLong(DomenebegrepUtbetalingsoppdrag.PERIODE_ID, rad),
                forrigePeriodeId = parseValgfriLong(DomenebegrepUtbetalingsoppdrag.FORRIGE_PERIODE_ID, rad),
                kildeBehandlingId = parseValgfriLong(DomenebegrepTilkjentYtelse.KILDEBEHANDLING_ID, rad),
            )
        }
    }

    private fun mapForventetUtbetalingsperiode(it: Map<String, String>) =
        ForventetUtbetalingsperiode(
            erEndringPåEksisterendePeriode = parseBoolean(DomenebegrepUtbetalingsoppdrag.ER_ENDRING, it),
            periodeId = parseLong(DomenebegrepUtbetalingsoppdrag.PERIODE_ID, it),
            forrigePeriodeId = parseValgfriLong(DomenebegrepUtbetalingsoppdrag.FORRIGE_PERIODE_ID, it),
            sats = parseInt(DomenebegrepUtbetalingsoppdrag.BELØP, it),
            ytelse = parseValgfriEnum<YtelseType>(DomenebegrepUtbetalingsoppdrag.YTELSE_TYPE, it)
                ?: YtelseType.ORDINÆR_BARNETRYGD,
            fom = parseÅrMåned(Domenebegrep.FRA_DATO, it).atDay(1),
            tom = parseÅrMåned(Domenebegrep.TIL_DATO, it).atEndOfMonth(),
            opphør = parseValgfriÅrMåned(DomenebegrepUtbetalingsoppdrag.OPPHØRSDATO, it)?.atDay(1),
            kildebehandlingId = parseValgfriLong(DomenebegrepTilkjentYtelse.KILDEBEHANDLING_ID, it),
        )

    private fun validerAlleKodeEndringerLike(rader: List<Map<String, String>>) {
        rader.map { parseEnum<Utbetalingsoppdrag.KodeEndring>(DomenebegrepUtbetalingsoppdrag.KODE_ENDRING, it) }
            .zipWithNext().forEach {
                assertThat(it.first).isEqualTo(it.second)
                    .withFailMessage("Alle kodeendringer for en og samme oppdrag må være lik ${it.first} -> ${it.second}")
            }
    }

    private fun mapAndelTilkjentYtelse(
        rad: Map<String, String>,
        behandling: Behandling,
        andelId: Int,
    ): AndelTilkjentYtelse {
        val ytelseType =
            parseValgfriEnum(DomenebegrepTilkjentYtelse.YTELSE_TYPE, rad) ?: YtelseType.ORDINÆR_BARNETRYGD
        return lagAndelTilkjentYtelse(
            fom = parseÅrMåned(Domenebegrep.FRA_DATO, rad),
            tom = parseÅrMåned(Domenebegrep.TIL_DATO, rad),
            ytelseType = ytelseType,
            beløp = parseInt(DomenebegrepTilkjentYtelse.BELØP, rad),
            behandling = behandling,
            tilkjentYtelse = null,
            kildeBehandlingId = parseValgfriLong(DomenebegrepTilkjentYtelse.KILDEBEHANDLING_ID, rad),
            aktør = parseAktør(rad),
            periodeIdOffset = parseValgfriLong(DomenebegrepUtbetalingsoppdrag.PERIODE_ID, rad),
            forrigeperiodeIdOffset = parseValgfriLong(DomenebegrepUtbetalingsoppdrag.FORRIGE_PERIODE_ID, rad),
        ).copy(id = parseValgfriLong(Domenebegrep.ID, rad) ?: andelId.toLong())
    }

    private fun parseAktør(rad: Map<String, String>): Aktør {
        val id = (parseValgfriInt(DomenebegrepTilkjentYtelse.IDENT, rad) ?: 1).toString()
        val aktørId = id.padStart(13, '0')
        val fødselsnummer = lagFødselsnummer(id)
        return Aktør(aktørId).also {
            it.personidenter.add(Personident(fødselsnummer, it))
        }
    }

    private fun lagFødselsnummer(id: String) = id.padStart(11, '0')
}

enum class DomenebegrepBehandlingsinformasjon(override val nøkkel: String) : Domenenøkkel {
    ENDRET_MIGRERINGSDATO("Endret migreringsdato"),
}

enum class DomenebegrepTilkjentYtelse(override val nøkkel: String) : Domenenøkkel {
    YTELSE_TYPE("Ytelse"),
    UTEN_ANDELER("Uten andeler"),
    BELØP("Beløp"),
    KILDEBEHANDLING_ID("Kildebehandling"),
    IDENT("Ident"),
}

enum class DomenebegrepUtbetalingsoppdrag(override val nøkkel: String) : Domenenøkkel {
    KODE_ENDRING("Kode endring"),
    ER_ENDRING("Er endring"),
    PERIODE_ID("Periode id"),
    FORRIGE_PERIODE_ID("Forrige periode id"),
    BELØP("Beløp"),
    YTELSE_TYPE("Ytelse"),
    OPPHØRSDATO("Opphørsdato"),
}

data class ForventetUtbetalingsoppdrag(
    val behandlingId: Long,
    val kodeEndring: Utbetalingsoppdrag.KodeEndring,
    val utbetalingsperiode: List<ForventetUtbetalingsperiode>,
)

data class ForventetUtbetalingsperiode(
    val erEndringPåEksisterendePeriode: Boolean,
    val periodeId: Long,
    val forrigePeriodeId: Long?,
    val sats: Int,
    val ytelse: YtelseType,
    val fom: LocalDate,
    val tom: LocalDate,
    val opphør: LocalDate?,
    val kildebehandlingId: Long?,
)
