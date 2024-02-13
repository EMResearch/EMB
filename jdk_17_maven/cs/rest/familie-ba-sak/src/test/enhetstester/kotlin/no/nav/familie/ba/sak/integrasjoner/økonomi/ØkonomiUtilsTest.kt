package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.common.årMnd
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiUtils.andelerTilOpphørMedDato
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiUtils.andelerTilOpprettelse
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiUtils.grupperAndeler
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiUtils.oppdaterBeståendeAndelerMedOffset
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiUtils.sisteBeståendeAndelPerKjede
import no.nav.familie.ba.sak.kjerne.beregning.BeregningTestUtil.sisteAndelPerIdent
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType.ORDINÆR_BARNETRYGD
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType.SMÅBARNSTILLEGG
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.YearMonth

internal class ØkonomiUtilsTest {

    @Test
    fun `skal separere småbarnstillegg`() {
        val person = tilfeldigPerson()
        val kjederBehandling = grupperAndeler(
            listOf(
                lagAndelTilkjentYtelse(
                    årMnd("2023-10"),
                    årMnd("2025-01"),
                    SMÅBARNSTILLEGG,
                    1054,
                    person = person,
                ),
                lagAndelTilkjentYtelse(
                    årMnd("2027-10"),
                    årMnd("2028-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = person,
                ),
            ).forIverksetting(),
        )

        assertEquals(2, kjederBehandling.size)
    }

    @Test
    fun `skal siste før første berørte andel i kjede`() {
        val person = tilfeldigPerson()
        val person2 = tilfeldigPerson()

        val kjederBehandling1 = grupperAndeler(
            listOf(
                lagAndelTilkjentYtelse(
                    årMnd("2019-04"),
                    årMnd("2020-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = person,
                ),
                lagAndelTilkjentYtelse(
                    årMnd("2022-01"),
                    årMnd("2023-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = person,
                ),
                lagAndelTilkjentYtelse(
                    årMnd("2019-04"),
                    årMnd("2020-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = person2,
                ),
                lagAndelTilkjentYtelse(
                    årMnd("2022-01"),
                    årMnd("2023-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = person2,
                ),
            ).forIverksetting(),
        )
        val kjederBehandling2 = grupperAndeler(
            listOf(
                lagAndelTilkjentYtelse(
                    årMnd("2019-04"),
                    årMnd("2020-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = person,
                ),
                lagAndelTilkjentYtelse(
                    årMnd("2022-01"),
                    årMnd("2022-10"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = person,
                ),
                lagAndelTilkjentYtelse(
                    årMnd("2019-04"),
                    årMnd("2020-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = person2,
                ),
                lagAndelTilkjentYtelse(
                    årMnd("2022-01"),
                    årMnd("2023-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = person2,
                ),
            ).forIverksetting(),
        )

        val sisteBeståendePerKjede =
            sisteBeståendeAndelPerKjede(forrigeKjeder = kjederBehandling1, oppdaterteKjeder = kjederBehandling2)
        val identOgTypePerson = IdentOgYtelse(person.aktør.aktivFødselsnummer(), ORDINÆR_BARNETRYGD)
        val identOgYtelsePerson2 = IdentOgYtelse(person2.aktør.aktivFødselsnummer(), ORDINÆR_BARNETRYGD)
        assertEquals(årMnd("2019-04"), sisteBeståendePerKjede[identOgTypePerson]?.stønadFom)
        assertEquals(årMnd("2022-01"), sisteBeståendePerKjede[identOgYtelsePerson2]?.stønadFom)
    }

    @Test
    fun `skal sette null som siste bestående for person med endring i første`() {
        val person = tilfeldigPerson()

        val kjederBehandling1 = grupperAndeler(
            listOf(
                lagAndelTilkjentYtelse(
                    årMnd("2019-04"),
                    årMnd("2020-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = person,
                ),
                lagAndelTilkjentYtelse(
                    årMnd("2022-01"),
                    årMnd("2023-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = person,
                ),
            ).forIverksetting(),
        )
        val kjederBehandling2 = grupperAndeler(
            listOf(
                lagAndelTilkjentYtelse(
                    årMnd("2018-04"),
                    årMnd("2020-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = person,
                ),
                lagAndelTilkjentYtelse(
                    årMnd("2022-01"),
                    årMnd("2023-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = person,
                ),
            ).forIverksetting(),
        )

        val sisteBeståendePerKjede =
            sisteBeståendeAndelPerKjede(forrigeKjeder = kjederBehandling1, oppdaterteKjeder = kjederBehandling2)
        assertEquals(null, sisteBeståendePerKjede[IdentOgYtelse(person.aktør.aktørId, ORDINÆR_BARNETRYGD)])
    }

    @Test
    fun `skal sette null som siste bestående for ny person`() {
        val person = tilfeldigPerson()
        val kjederBehandling = grupperAndeler(
            listOf(
                lagAndelTilkjentYtelse(
                    årMnd("2023-10"),
                    årMnd("2025-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = person,
                ),
                lagAndelTilkjentYtelse(
                    årMnd("2027-10"),
                    årMnd("2028-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = person,
                ),
            ).forIverksetting(),
        )

        val sisteBeståendePerKjede =
            sisteBeståendeAndelPerKjede(forrigeKjeder = emptyMap(), oppdaterteKjeder = kjederBehandling)
        assertEquals(null, sisteBeståendePerKjede[IdentOgYtelse(person.aktør.aktørId, ORDINÆR_BARNETRYGD)]?.stønadFom)
    }

    @Test
    fun `skal settes null som siste bestående ved fullt opphørt person`() {
        val person = tilfeldigPerson()

        val kjederBehandling = grupperAndeler(
            listOf(
                lagAndelTilkjentYtelse(
                    årMnd("2023-10"),
                    årMnd("2025-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = person,
                ),
                lagAndelTilkjentYtelse(
                    årMnd("2027-10"),
                    årMnd("2028-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = person,
                ),
            ).forIverksetting(),
        )

        val sisteBeståendePerKjede =
            sisteBeståendeAndelPerKjede(forrigeKjeder = kjederBehandling, oppdaterteKjeder = emptyMap())
        assertEquals(null, sisteBeståendePerKjede[IdentOgYtelse(person.aktør.aktørId, ORDINÆR_BARNETRYGD)]?.stønadFom)
    }

    @Test
    fun `skal velge rette perioder til opphør og oppbygging fra endring`() {
        val person = tilfeldigPerson()

        val datoSomSkalOppdateres = "2022-01"
        val datoSomErOppdatert = "2021-01"

        val kjederBehandling1 = grupperAndeler(
            listOf(
                lagAndelTilkjentYtelse(
                    årMnd("2019-04"),
                    årMnd("2020-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = person,
                    aktør = person.aktør,
                    periodeIdOffset = 0,
                    forrigeperiodeIdOffset = null,
                ),
                lagAndelTilkjentYtelse(
                    årMnd(datoSomSkalOppdateres),
                    årMnd("2023-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = person,
                    aktør = person.aktør,
                    periodeIdOffset = 1,
                    forrigeperiodeIdOffset = 0,
                ),
                lagAndelTilkjentYtelse(
                    årMnd("2025-04"),
                    årMnd("2026-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = person,
                    aktør = person.aktør,
                    periodeIdOffset = 2,
                    forrigeperiodeIdOffset = 1,
                ),
            ).forIverksetting(),
        )
        val kjederBehandling2 = grupperAndeler(
            listOf(
                lagAndelTilkjentYtelse(
                    årMnd("2019-04"),
                    årMnd("2020-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = person,
                    aktør = person.aktør,
                ),
                lagAndelTilkjentYtelse(
                    årMnd(datoSomErOppdatert),
                    årMnd("2023-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = person,
                    aktør = person.aktør,
                ),
                lagAndelTilkjentYtelse(
                    årMnd("2025-04"),
                    årMnd("2026-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = person,
                    aktør = person.aktør,
                ),
            ).forIverksetting(),
        )

        val sisteBeståendePerKjede =
            sisteBeståendeAndelPerKjede(
                forrigeKjeder = kjederBehandling1,
                oppdaterteKjeder = kjederBehandling2,
            )
        val andelerTilOpprettelse =
            andelerTilOpprettelse(
                oppdaterteKjeder = kjederBehandling2,
                sisteBeståendeAndelIHverKjede = sisteBeståendePerKjede,
            )
        val andelerTilOpphørMedDato =
            andelerTilOpphørMedDato(
                forrigeKjeder = kjederBehandling1,
                sisteBeståendeAndelIHverKjede = sisteBeståendePerKjede,
                sisteAndelPerIdent = sisteAndelPerIdent(kjederBehandling1.values.flatten()),
            )

        assertEquals(1, andelerTilOpprettelse.size)
        assertEquals(2, andelerTilOpprettelse.first().size)
        assertEquals(1, andelerTilOpphørMedDato.size)
        assertEquals(årMnd(datoSomSkalOppdateres), andelerTilOpphørMedDato.first().second)
    }

    @Test
    fun `skal opphøre først barn helt og innvilge nytt barn når første barn ikke er innvilget i andre behandling`() {
        val førsteBarn = tilfeldigPerson()
        val andreBarn = tilfeldigPerson()

        val kjederBehandling1 = grupperAndeler(
            listOf(
                lagAndelTilkjentYtelse(
                    årMnd("2019-04"),
                    årMnd("2020-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = førsteBarn,
                    aktør = førsteBarn.aktør,
                    periodeIdOffset = 0,
                    forrigeperiodeIdOffset = null,
                ),
                lagAndelTilkjentYtelse(
                    årMnd("2020-02"),
                    årMnd("2023-01"),
                    ORDINÆR_BARNETRYGD,
                    1345,
                    person = førsteBarn,
                    aktør = førsteBarn.aktør,
                    periodeIdOffset = 1,
                    forrigeperiodeIdOffset = 0,
                ),
                lagAndelTilkjentYtelse(
                    årMnd("2023-02"),
                    årMnd("2026-01"),
                    ORDINÆR_BARNETRYGD,
                    1654,
                    person = førsteBarn,
                    aktør = førsteBarn.aktør,
                    periodeIdOffset = 2,
                    forrigeperiodeIdOffset = 1,
                ),
            ).forIverksetting(),
        )
        val kjederBehandling2 = grupperAndeler(
            listOf(
                lagAndelTilkjentYtelse(
                    årMnd("2020-04"),
                    årMnd("2021-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = andreBarn,
                    aktør = andreBarn.aktør,
                ),
                lagAndelTilkjentYtelse(
                    årMnd("2021-02"),
                    årMnd("2023-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = andreBarn,
                    aktør = andreBarn.aktør,
                ),
                lagAndelTilkjentYtelse(
                    årMnd("2025-04"),
                    årMnd("2026-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = andreBarn,
                    aktør = andreBarn.aktør,
                ),
            ).forIverksetting(),
        )

        val sisteBeståendePerKjede =
            sisteBeståendeAndelPerKjede(
                forrigeKjeder = kjederBehandling1,
                oppdaterteKjeder = kjederBehandling2,
            )
        val andelerTilOpprettelse =
            andelerTilOpprettelse(
                oppdaterteKjeder = kjederBehandling2,
                sisteBeståendeAndelIHverKjede = sisteBeståendePerKjede,
            )
        val andelerTilOpphørMedDato =
            andelerTilOpphørMedDato(
                forrigeKjeder = kjederBehandling1,
                sisteBeståendeAndelIHverKjede = sisteBeståendePerKjede,
                sisteAndelPerIdent = sisteAndelPerIdent(kjederBehandling1.values.flatten()),
            )

        assertEquals(1, andelerTilOpphørMedDato.size)
        assertEquals(YearMonth.of(2023, 2), andelerTilOpphørMedDato.first().first.stønadFom)
        assertEquals(YearMonth.of(2019, 4), andelerTilOpphørMedDato.first().second)
        assertEquals(3, andelerTilOpprettelse.first().size)
        assertEquals(YearMonth.of(2020, 4), andelerTilOpprettelse.first().first().stønadFom)
        assertEquals(YearMonth.of(2021, 1), andelerTilOpprettelse.first().first().stønadTom)
        assertEquals(YearMonth.of(2025, 4), andelerTilOpprettelse.first().last().stønadFom)
        assertEquals(YearMonth.of(2026, 1), andelerTilOpprettelse.first().last().stønadTom)
    }

    @Test
    fun `skal gjøre separate endringer på ordinær og småbarnstillegg`() {
        val person = tilfeldigPerson()

        val kjederBehandling1 = grupperAndeler(
            listOf(
                lagAndelTilkjentYtelse(
                    årMnd("2019-04"),
                    årMnd("2020-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = person,
                ),
            ).forIverksetting(),
        )
        val kjederBehandling2 = grupperAndeler(
            listOf(
                lagAndelTilkjentYtelse(
                    årMnd("2019-04"),
                    årMnd("2019-06"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = person,
                ),
                lagAndelTilkjentYtelse(
                    årMnd("2022-01"),
                    årMnd("2023-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = person,
                ),
                lagAndelTilkjentYtelse(
                    årMnd("2019-04"),
                    årMnd("2020-01"),
                    SMÅBARNSTILLEGG,
                    1054,
                    person = person,
                ),
            ).forIverksetting(),
        )

        val sisteBeståendePerKjede =
            sisteBeståendeAndelPerKjede(
                forrigeKjeder = kjederBehandling1,
                oppdaterteKjeder = kjederBehandling2,
            )
        val andelerTilOpprettelse =
            andelerTilOpprettelse(
                oppdaterteKjeder = kjederBehandling2,
                sisteBeståendeAndelIHverKjede = sisteBeståendePerKjede,
            )
        val andelerTilOpphørMedDato =
            andelerTilOpphørMedDato(
                forrigeKjeder = kjederBehandling1,
                sisteBeståendeAndelIHverKjede = sisteBeståendePerKjede,
                sisteAndelPerIdent = sisteAndelPerIdent(kjederBehandling1.values.flatten()),
            )

        assertEquals(2, andelerTilOpprettelse.size)
        assertEquals(1, andelerTilOpphørMedDato.size)
        assertEquals(årMnd("2019-04"), andelerTilOpphørMedDato.first().second)
    }

    @Test
    fun `skal oppdatere offset på bestående behandler i oppdaterte kjeder`() {
        val person = tilfeldigPerson()
        val person2 = tilfeldigPerson()

        val kjederBehandling1 = grupperAndeler(
            listOf(
                lagAndelTilkjentYtelse(
                    årMnd("2019-04"),
                    årMnd("2020-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    periodeIdOffset = 1,
                    forrigeperiodeIdOffset = 0,
                    person = person,
                ),
                lagAndelTilkjentYtelse(
                    årMnd("2019-04"),
                    årMnd("2020-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    periodeIdOffset = 3,
                    forrigeperiodeIdOffset = 2,
                    person = person2,
                ),
            ).forIverksetting(),
        )
        val kjederBehandling2 = grupperAndeler(
            listOf(
                lagAndelTilkjentYtelse(
                    årMnd("2019-04"),
                    årMnd("2020-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = person,
                ),
                lagAndelTilkjentYtelse(
                    årMnd("2019-12"),
                    årMnd("2020-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = person2,
                ),
            ).forIverksetting(),
        )

        val oppdaterte =
            oppdaterBeståendeAndelerMedOffset(
                forrigeKjeder = kjederBehandling1,
                oppdaterteKjeder = kjederBehandling2,
            )

        val identOgYtelse = IdentOgYtelse(person.aktør.aktivFødselsnummer(), ORDINÆR_BARNETRYGD)
        val identOgYtelsePerson2 = IdentOgYtelse(person2.aktør.aktivFødselsnummer(), ORDINÆR_BARNETRYGD)
        assertEquals(1, oppdaterte.getValue(identOgYtelse).first().periodeOffset)
        assertEquals(0, oppdaterte.getValue(identOgYtelse).first().forrigePeriodeOffset)
        assertEquals(null, oppdaterte.getValue(identOgYtelsePerson2).first().periodeOffset)
        assertEquals(null, oppdaterte.getValue(identOgYtelsePerson2).first().forrigePeriodeOffset)
    }
}

fun Collection<AndelTilkjentYtelse>.forIverksetting() =
    AndelTilkjentYtelseForIverksettingFactory().pakkInnForUtbetaling(this)
