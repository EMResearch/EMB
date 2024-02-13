package no.nav.familie.ba.sak.kjerne.beregning

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.InternPeriodeOvergangsstønad
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.util.sisteSmåbarnstilleggSatsTilTester
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvFileSource
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class VilkårTilTilkjentYtelseTest {

    @BeforeEach
    fun førHverTest() {
        mockkObject(SatsTidspunkt)
        every { SatsTidspunkt.senesteSatsTidspunkt } returns LocalDate.of(2022, 12, 31)
    }

    @AfterEach
    fun etterHverTest() {
        unmockkObject(SatsTidspunkt)
    }

    @ParameterizedTest
    @CsvFileSource(
        resources = ["/beregning/vilkår_til_tilkjent_ytelse/søker_med_ett_barn_inntil_tre_perioder.csv"],
        numLinesToSkip = 1,
        delimiter = ';',
    )
    fun `test søker med ett barn, inntil tre perioder`(
        sakType: String,
        søkerPeriode1: String?,
        søkerVilkår1: String?,
        søkerPeriode2: String?,
        søkerVilkår2: String?,
        barn1Periode1: String?,
        barn1Vilkår1: String?,
        barn1Andel1Beløp: Int?,
        barn1Andel1Periode: String?,
        barn1Andel1Type: String?,
        barn1Andel2Beløp: Int?,
        barn1Andel2Periode: String?,
        barn1Andel2Type: String?,
        barn1Andel3Beløp: Int?,
        barn1Andel3Periode: String?,
        barn1Andel3Type: String?,
        erDeltBosted: Boolean?,
    ) {
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = LocalDate.of(2021, 9, 1))

        val vilkårsvurdering = TestVilkårsvurderingBuilder(sakType)
            .medPersonVilkårPeriode(søker, søkerVilkår1, søkerPeriode1, erDeltBosted)
            .medPersonVilkårPeriode(søker, søkerVilkår2, søkerPeriode2, erDeltBosted)
            .medPersonVilkårPeriode(barn1, barn1Vilkår1, barn1Periode1, erDeltBosted)
            .bygg()

        val delBeløp = if (erDeltBosted != null && erDeltBosted) 2 else 1

        val forventetTilkjentYtelse = TestTilkjentYtelseBuilder(vilkårsvurdering.behandling)
            .medAndelTilkjentYtelse(barn1, barn1Andel1Beløp?.div(delBeløp), barn1Andel1Periode, barn1Andel1Type)
            .medAndelTilkjentYtelse(barn1, barn1Andel2Beløp?.div(delBeløp), barn1Andel2Periode, barn1Andel2Type)
            .medAndelTilkjentYtelse(barn1, barn1Andel3Beløp?.div(delBeløp), barn1Andel3Periode, barn1Andel3Type)
            .bygg()

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(vilkårsvurdering.behandling.id, søker, barn1)

        val faktiskTilkjentYtelse = TilkjentYtelseUtils.beregnTilkjentYtelse(
            vilkårsvurdering = vilkårsvurdering,
            personopplysningGrunnlag = personopplysningGrunnlag,
            fagsakType = FagsakType.NORMAL,

        )

        Assertions.assertEquals(
            forventetTilkjentYtelse.andelerTilkjentYtelse,
            faktiskTilkjentYtelse.andelerTilkjentYtelse,
        )
    }

    @ParameterizedTest
    @CsvFileSource(
        resources = ["/beregning/vilkår_til_tilkjent_ytelse/søker_med_utvidet_og_ett_barn_inntil_to_perioder.csv"],
        numLinesToSkip = 1,
        delimiter = ';',
    )
    fun `test søker med utvidet og ett barn, inntil to perioder`(
        sakType: String,
        søkerPeriode1: String,
        søkerVilkår1: String,
        søkerAndel1Beløp: Int,
        søkerAndel1Periode: String,
        søkerAndel1Type: String,
        småbarnstilleggPeriode: String?,
        barn1Periode1: String?,
        barn1Vilkår1: String?,
        barn1Andel1Beløp: Int?,
        barn1Andel1Periode: String?,
        barn1Andel1Type: String?,
    ) {
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = LocalDate.of(2021, 9, 1))

        val vilkårsvurdering = TestVilkårsvurderingBuilder(sakType)
            .medPersonVilkårPeriode(søker, søkerVilkår1, søkerPeriode1)
            .medPersonVilkårPeriode(barn1, barn1Vilkår1, barn1Periode1)
            .bygg()

        val småbarnstilleggTestPeriode: TestPeriode? =
            if (småbarnstilleggPeriode != null) TestPeriode.parse(småbarnstilleggPeriode) else null

        val forventetTilkjentYtelse = if (småbarnstilleggTestPeriode != null) {
            TestTilkjentYtelseBuilder(vilkårsvurdering.behandling)
                .medAndelTilkjentYtelse(barn1, barn1Andel1Beløp, barn1Andel1Periode, barn1Andel1Type)
                .medAndelTilkjentYtelse(søker, søkerAndel1Beløp, søkerAndel1Periode, søkerAndel1Type)
                .medAndelTilkjentYtelse(
                    søker,
                    sisteSmåbarnstilleggSatsTilTester(),
                    småbarnstilleggPeriode,
                    YtelseType.SMÅBARNSTILLEGG.name,
                )
                .bygg()
        } else {
            TestTilkjentYtelseBuilder(vilkårsvurdering.behandling)
                .medAndelTilkjentYtelse(barn1, barn1Andel1Beløp, barn1Andel1Periode, barn1Andel1Type)
                .medAndelTilkjentYtelse(søker, søkerAndel1Beløp, søkerAndel1Periode, søkerAndel1Type)
                .bygg()
        }

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(vilkårsvurdering.behandling.id, søker, barn1)

        val faktiskTilkjentYtelse = TilkjentYtelseUtils.beregnTilkjentYtelse(
            vilkårsvurdering = vilkårsvurdering,
            personopplysningGrunnlag = personopplysningGrunnlag,
            fagsakType = FagsakType.NORMAL,

        ) { aktør ->
            if (småbarnstilleggTestPeriode != null) {
                listOf(
                    InternPeriodeOvergangsstønad(
                        personIdent = aktør.aktivFødselsnummer(),
                        fomDato = småbarnstilleggTestPeriode.fraOgMed,
                        tomDato = småbarnstilleggTestPeriode.tilOgMed!!,
                    ),
                )
            } else {
                emptyList()
            }
        }

        Assertions.assertEquals(
            forventetTilkjentYtelse.andelerTilkjentYtelse,
            faktiskTilkjentYtelse.andelerTilkjentYtelse,
        )
    }

    @ParameterizedTest
    @CsvFileSource(
        resources = ["/beregning/vilkår_til_tilkjent_ytelse/søker_med_to_barn_inntil_to_perioder.csv"],
        numLinesToSkip = 1,
        delimiter = ';',
    )
    fun `test søker med to barn, inntil to perioder`(
        søkerPeriode1: String?,
        søkerVilkår1: String?,
        søkerPeriode2: String?,
        søkerVilkår2: String?,
        barn1Periode1: String?,
        barn1Vilkår1: String?,
        barn2Periode1: String?,
        barn2Vilkår1: String?,
        barn1Andel1Beløp: Int?,
        barn1Andel1Periode: String?,
        barn1Andel1Type: String?,
        barn1Andel2Beløp: Int?,
        barn1Andel2Periode: String?,
        barn1Andel2Type: String?,
        barn1Andel3Beløp: Int?,
        barn1Andel3Periode: String?,
        barn1Andel3Type: String?,
        barn2Andel1Beløp: Int?,
        barn2Andel1Periode: String?,
        barn2Andel1Type: String?,
        barn2Andel2Beløp: Int?,
        barn2Andel2Periode: String?,
        barn2Andel2Type: String?,
    ) {
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = LocalDate.of(2020, 2, 1))
        val barn2 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = LocalDate.of(2022, 4, 1))

        val vilkårsvurdering = TestVilkårsvurderingBuilder("NASJONAL")
            .medPersonVilkårPeriode(søker, søkerVilkår1, søkerPeriode1)
            .medPersonVilkårPeriode(søker, søkerVilkår2, søkerPeriode2)
            .medPersonVilkårPeriode(barn1, barn1Vilkår1, barn1Periode1)
            .medPersonVilkårPeriode(barn2, barn2Vilkår1, barn2Periode1)
            .bygg()

        val forventetTilkjentYtelse = TestTilkjentYtelseBuilder(vilkårsvurdering.behandling)
            .medAndelTilkjentYtelse(barn1, barn1Andel1Beløp, barn1Andel1Periode, barn1Andel1Type)
            .medAndelTilkjentYtelse(barn1, barn1Andel2Beløp, barn1Andel2Periode, barn1Andel2Type)
            .medAndelTilkjentYtelse(barn1, barn1Andel3Beløp, barn1Andel3Periode, barn1Andel3Type)
            .medAndelTilkjentYtelse(barn2, barn2Andel1Beløp, barn2Andel1Periode, barn2Andel1Type)
            .medAndelTilkjentYtelse(barn2, barn2Andel2Beløp, barn2Andel2Periode, barn2Andel2Type)
            .bygg()

        val personopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(vilkårsvurdering.behandling.id, søker, barn1, barn2)

        val faktiskTilkjentYtelse = TilkjentYtelseUtils.beregnTilkjentYtelse(
            vilkårsvurdering = vilkårsvurdering,
            personopplysningGrunnlag = personopplysningGrunnlag,
            fagsakType = FagsakType.NORMAL,

        )

        Assertions.assertEquals(
            forventetTilkjentYtelse.andelerTilkjentYtelse,
            faktiskTilkjentYtelse.andelerTilkjentYtelse,
        )
    }
}

class TestVilkårsvurderingBuilder(sakType: String) {

    private val identPersonResultatMap = mutableMapOf<String, PersonResultat>()
    private val vilkårsvurdering =
        Vilkårsvurdering(
            behandling = lagBehandling(
                behandlingKategori = BehandlingKategori.valueOf(sakType),
            ),
        )

    fun medPersonVilkårPeriode(
        person: Person,
        vilkår: String?,
        periode: String?,
        erDeltBosted: Boolean? = null,
    ): TestVilkårsvurderingBuilder {
        if (vilkår.isNullOrEmpty() || periode.isNullOrEmpty()) {
            return this
        }

        val ident = person.aktør.aktivFødselsnummer()
        val aktørId = person.aktør
        val personResultat =
            identPersonResultatMap.getOrPut(ident) { PersonResultat(0, vilkårsvurdering, aktørId) }

        val testperiode = TestPeriode.parse(periode)

        val vilkårsresultater = TestVilkårParser.parse(vilkår).map {
            VilkårResultat(
                personResultat = personResultat,
                vilkårType = it,
                resultat = Resultat.OPPFYLT,
                periodeFom = testperiode.fraOgMed,
                periodeTom = testperiode.tilOgMed,
                begrunnelse = "",
                sistEndretIBehandlingId = vilkårsvurdering.behandling.id,
                utdypendeVilkårsvurderinger = listOfNotNull(
                    if (erDeltBosted == true) UtdypendeVilkårsvurdering.DELT_BOSTED else null,
                ),
            )
        }.toSet()

        personResultat.setSortedVilkårResultater(
            personResultat.vilkårResultater.plus(vilkårsresultater)
                .toSet(),
        )

        return this
    }

    fun bygg(): Vilkårsvurdering {
        vilkårsvurdering.personResultater = identPersonResultatMap.values.toSet()

        return vilkårsvurdering
    }
}

class TestTilkjentYtelseBuilder(val behandling: Behandling) {

    private val tilkjentYtelse = TilkjentYtelse(
        behandling = behandling,
        opprettetDato = LocalDate.now(),
        endretDato = LocalDate.now(),
    )

    fun medAndelTilkjentYtelse(
        person: Person,
        beløp: Int?,
        periode: String?,
        type: String?,
    ): TestTilkjentYtelseBuilder {
        if (beløp == null || periode.isNullOrEmpty() || type.isNullOrEmpty()) {
            return this
        }

        val stønadPeriode = TestPeriode.parse(periode)

        tilkjentYtelse.andelerTilkjentYtelse.add(
            AndelTilkjentYtelse(
                behandlingId = behandling.id,
                tilkjentYtelse = tilkjentYtelse,
                aktør = person.aktør,
                stønadFom = stønadPeriode.fraOgMed.toYearMonth(),
                stønadTom = stønadPeriode.tilOgMed!!.toYearMonth(),
                kalkulertUtbetalingsbeløp = beløp.toInt(),
                nasjonaltPeriodebeløp = beløp.toInt(),
                type = YtelseType.valueOf(type),
                sats = beløp.toInt(),
                prosent = BigDecimal(100),
            ),
        )

        return this
    }

    fun bygg(): TilkjentYtelse {
        return tilkjentYtelse
    }
}

data class TestPeriode(val fraOgMed: LocalDate, val tilOgMed: LocalDate?) {

    companion object {

        private val yearMonthRegex = """^(\d{4}-\d{2}).*?(\d{4}-\d{2})?$""".toRegex()
        private val localDateRegex = """^(\d{4}-\d{2}-\d{2}).*?(\d{4}-\d{2}-\d{2})?$""".toRegex()

        fun parse(s: String): TestPeriode {
            return prøvLocalDate(s) ?: prøvYearMonth(s)
                ?: throw IllegalArgumentException("Kunne ikke parse periode '$s'")
        }

        private fun prøvLocalDate(s: String): TestPeriode? {
            val localDateMatch = localDateRegex.find(s)

            if (localDateMatch != null && localDateMatch.groupValues.size == 3) {
                val fom = localDateMatch.groupValues[1].let { LocalDate.parse(it) }
                val tom =
                    localDateMatch.groupValues[2].let { if (it.length == 10) LocalDate.parse(it) else null }

                return TestPeriode(fom!!, tom)
            }
            return null
        }

        private fun prøvYearMonth(s: String): TestPeriode? {
            val yearMonthMatch = yearMonthRegex.find(s)

            if (yearMonthMatch != null && yearMonthMatch.groupValues.size == 3) {
                val fom = yearMonthMatch.groupValues[1].let { YearMonth.parse(it) }
                val tom =
                    yearMonthMatch.groupValues[2].let {
                        if (it.length == 7) {
                            YearMonth.parse(it)
                        } else {
                            null
                        }
                    }

                return TestPeriode(fom!!.atDay(1), tom?.atEndOfMonth())
            }
            return null
        }
    }
}

object TestVilkårParser {

    fun parse(s: String): List<Vilkår> {
        return s.split(',')
            .map {
                when (it.replace("""\s*""".toRegex(), "").lowercase()) {
                    "opphold" -> Vilkår.LOVLIG_OPPHOLD
                    "<18" -> Vilkår.UNDER_18_ÅR
                    "<18år" -> Vilkår.UNDER_18_ÅR
                    "under18" -> Vilkår.UNDER_18_ÅR
                    "under18år" -> Vilkår.UNDER_18_ÅR
                    "bosatt" -> Vilkår.BOSATT_I_RIKET
                    "bormedsøker" -> Vilkår.BOR_MED_SØKER
                    "gift" -> Vilkår.GIFT_PARTNERSKAP
                    "partnerskap" -> Vilkår.GIFT_PARTNERSKAP
                    "utvidet" -> Vilkår.UTVIDET_BARNETRYGD
                    else -> throw IllegalArgumentException("Ukjent vilkår: $s")
                }
            }.toList()
    }
}
