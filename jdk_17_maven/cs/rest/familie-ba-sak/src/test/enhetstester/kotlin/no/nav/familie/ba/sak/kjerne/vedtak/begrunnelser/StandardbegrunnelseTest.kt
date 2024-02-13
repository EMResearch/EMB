package no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser

import io.mockk.mockk
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagEndretUtbetalingAndel
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.lagTriggesAv
import no.nav.familie.ba.sak.common.lagUtbetalingsperiodeDetalj
import no.nav.familie.ba.sak.common.lagUtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.common.lagVilkårsvurdering
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.config.testSanityKlient
import no.nav.familie.ba.sak.datagenerator.brev.lagMinimertPerson
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.brev.domene.EndretUtbetalingsperiodeDeltBostedTriggere
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.tilMinimertEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.brev.domene.tilMinimertPersonResultat
import no.nav.familie.ba.sak.kjerne.brev.domene.ØvrigTrigger
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.lagDødsfallFraPdl
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.Personident
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.domene.tilMinimertVedtaksperiode
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.domene.tilMinimertePersoner
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class StandardbegrunnelseTest {

    private val behandling = lagBehandling()
    private val søker = tilfeldigPerson(personType = PersonType.SØKER)
    private val barn = tilfeldigPerson(personType = PersonType.BARN)
    private val utvidetVedtaksperiodeMedBegrunnelser = lagUtvidetVedtaksperiodeMedBegrunnelser(
        type = Vedtaksperiodetype.UTBETALING,
        utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj()),
    )
    private val vilkårsvurdering =
        lagVilkårsvurdering(søker.aktør, lagBehandling(), Resultat.OPPFYLT)
    val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, søker, barn)

    private val aktørerMedUtbetaling = listOf(søker.aktør, barn.aktør)

    private val sanityBegrunnelser = testSanityKlient.hentBegrunnelserMap()
    private val featureToggleService: FeatureToggleService = mockk()

    @Test
    fun `Oppfyller vilkår skal gi true`() {
        assertTrue(
            Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET
                .triggesForPeriode(

                    sanityBegrunnelser = sanityBegrunnelser,
                    minimertVedtaksperiode = utvidetVedtaksperiodeMedBegrunnelser.tilMinimertVedtaksperiode(),
                    minimertePersonResultater = vilkårsvurdering.personResultater.map { it.tilMinimertPersonResultat() },
                    minimertePersoner = personopplysningGrunnlag.tilMinimertePersoner(),
                    aktørIderMedUtbetaling = aktørerMedUtbetaling.map { it.aktørId },
                    erFørsteVedtaksperiodePåFagsak = false,
                    ytelserForSøkerForrigeMåned = emptyList(),
                    ytelserForrigePeriode = emptyList(),
                ),
        )
    }

    @Test
    fun `Annen periode type skal gi false`() {
        assertFalse(
            Standardbegrunnelse.OPPHØR_UTVANDRET
                .triggesForPeriode(

                    sanityBegrunnelser = sanityBegrunnelser,
                    minimertVedtaksperiode = utvidetVedtaksperiodeMedBegrunnelser.tilMinimertVedtaksperiode(),
                    minimertePersonResultater = vilkårsvurdering.personResultater.map { it.tilMinimertPersonResultat() },
                    minimertePersoner = personopplysningGrunnlag.tilMinimertePersoner(),
                    aktørIderMedUtbetaling = aktørerMedUtbetaling.map { it.aktørId },
                    erFørsteVedtaksperiodePåFagsak = false,
                    ytelserForSøkerForrigeMåned = emptyList(),
                    ytelserForrigePeriode = emptyList(),
                ),
        )
    }

    @Test
    fun `Har ikke barn med seksårsdag skal gi false`() {
        assertFalse(
            Standardbegrunnelse.REDUKSJON_UNDER_6_ÅR
                .triggesForPeriode(

                    sanityBegrunnelser = sanityBegrunnelser,
                    minimertVedtaksperiode = utvidetVedtaksperiodeMedBegrunnelser.tilMinimertVedtaksperiode(),
                    minimertePersonResultater = vilkårsvurdering.personResultater.map { it.tilMinimertPersonResultat() },
                    minimertePersoner = personopplysningGrunnlag.tilMinimertePersoner(),
                    aktørIderMedUtbetaling = aktørerMedUtbetaling.map { it.aktørId },
                    erFørsteVedtaksperiodePåFagsak = false,
                    ytelserForSøkerForrigeMåned = emptyList(),
                    ytelserForrigePeriode = emptyList(),
                ),
        )
    }

    @Test
    fun `Har barn med seksårsdag skal gi true`() {
        val minimertePersoner =
            listOf(
                lagMinimertPerson(type = PersonType.BARN, fødselsdato = LocalDate.now().minusYears(6)),
                lagMinimertPerson(type = PersonType.SØKER),
            )

        assertTrue(
            Standardbegrunnelse.REDUKSJON_UNDER_6_ÅR
                .triggesForPeriode(

                    sanityBegrunnelser = sanityBegrunnelser,
                    minimertVedtaksperiode = utvidetVedtaksperiodeMedBegrunnelser.tilMinimertVedtaksperiode(),
                    minimertePersonResultater = vilkårsvurdering.personResultater.map { it.tilMinimertPersonResultat() },
                    minimertePersoner = minimertePersoner,
                    aktørIderMedUtbetaling = aktørerMedUtbetaling.map { it.aktørId },
                    erFørsteVedtaksperiodePåFagsak = false,
                    ytelserForSøkerForrigeMåned = emptyList(),
                    ytelserForrigePeriode = emptyList(),
                ),
        )
    }

    @Test
    fun `Har sats endring skal gi true`() {
        val vedtaksperiodeMedBegrunnelserSatsEndring = lagUtvidetVedtaksperiodeMedBegrunnelser(
            fom = LocalDate.of(2021, 9, 1),
            type = Vedtaksperiodetype.UTBETALING,
            utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj()),
        )

        assertTrue(
            Standardbegrunnelse.INNVILGET_SATSENDRING
                .triggesForPeriode(

                    sanityBegrunnelser = sanityBegrunnelser,
                    minimertVedtaksperiode = vedtaksperiodeMedBegrunnelserSatsEndring.tilMinimertVedtaksperiode(),
                    minimertePersonResultater = vilkårsvurdering.personResultater.map { it.tilMinimertPersonResultat() },
                    minimertePersoner = personopplysningGrunnlag.tilMinimertePersoner(),
                    aktørIderMedUtbetaling = aktørerMedUtbetaling.map { it.aktørId },
                    erFørsteVedtaksperiodePåFagsak = false,
                    ytelserForSøkerForrigeMåned = emptyList(),
                    ytelserForrigePeriode = emptyList(),
                ),
        )
    }

    @Test
    fun `Har ikke sats endring skal gi false`() {
        val vedtaksperiodeMedBegrunnelserSatsEndring = lagUtvidetVedtaksperiodeMedBegrunnelser(
            fom = LocalDate.of(2021, 8, 1),
            type = Vedtaksperiodetype.UTBETALING,
            utbetalingsperiodeDetaljer = listOf(lagUtbetalingsperiodeDetalj()),
        )

        assertFalse(
            Standardbegrunnelse.INNVILGET_SATSENDRING
                .triggesForPeriode(

                    sanityBegrunnelser = sanityBegrunnelser,
                    minimertVedtaksperiode = vedtaksperiodeMedBegrunnelserSatsEndring.tilMinimertVedtaksperiode(),
                    minimertePersonResultater = vilkårsvurdering.personResultater.map { it.tilMinimertPersonResultat() },
                    minimertePersoner = personopplysningGrunnlag.tilMinimertePersoner(),
                    aktørIderMedUtbetaling = aktørerMedUtbetaling.map { it.aktørId },
                    erFørsteVedtaksperiodePåFagsak = false,
                    ytelserForSøkerForrigeMåned = emptyList(),
                    ytelserForrigePeriode = emptyList(),
                ),
        )
    }

    @Test
    fun `Oppfyller ikke vilkår for person skal gi false`() {
        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, barn)

        assertFalse(
            Standardbegrunnelse.INNVILGET_LOVLIG_OPPHOLD_EØS_BORGER
                .triggesForPeriode(

                    sanityBegrunnelser = sanityBegrunnelser,
                    minimertVedtaksperiode = utvidetVedtaksperiodeMedBegrunnelser.tilMinimertVedtaksperiode(),
                    minimertePersonResultater = vilkårsvurdering.personResultater.map { it.tilMinimertPersonResultat() },
                    minimertePersoner = personopplysningGrunnlag.tilMinimertePersoner(),
                    aktørIderMedUtbetaling = aktørerMedUtbetaling.map { it.aktørId },
                    erFørsteVedtaksperiodePåFagsak = false,
                    ytelserForSøkerForrigeMåned = emptyList(),
                    ytelserForrigePeriode = emptyList(),
                ),
        )
    }

    @Test
    fun `Oppfyller vilkår for person skal gi true`() {
        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, søker)

        assertTrue(
            Standardbegrunnelse.INNVILGET_LOVLIG_OPPHOLD_EØS_BORGER
                .triggesForPeriode(

                    sanityBegrunnelser = sanityBegrunnelser,
                    minimertVedtaksperiode = utvidetVedtaksperiodeMedBegrunnelser.tilMinimertVedtaksperiode(),
                    minimertePersonResultater = vilkårsvurdering.personResultater.map { it.tilMinimertPersonResultat() },
                    minimertePersoner = personopplysningGrunnlag.tilMinimertePersoner(),
                    aktørIderMedUtbetaling = aktørerMedUtbetaling.map { it.aktørId },
                    erFørsteVedtaksperiodePåFagsak = false,
                    ytelserForSøkerForrigeMåned = emptyList(),
                    ytelserForrigePeriode = emptyList(),
                ),
        )
    }

    @Test
    fun `Oppfyller etter endringsperiode skal gi true`() {
        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, barn)

        assertTrue(
            Standardbegrunnelse.ETTER_ENDRET_UTBETALING_AVTALE_DELT_BOSTED_FØLGES
                .triggesForPeriode(

                    sanityBegrunnelser = sanityBegrunnelser,
                    minimertePersoner = personopplysningGrunnlag.tilMinimertePersoner(),
                    minimertePersonResultater = vilkårsvurdering.personResultater.map { it.tilMinimertPersonResultat() },
                    aktørIderMedUtbetaling = aktørerMedUtbetaling.map { it.aktørId },
                    minimertVedtaksperiode = lagUtvidetVedtaksperiodeMedBegrunnelser(
                        type = Vedtaksperiodetype.UTBETALING,
                        fom = LocalDate.of(2021, 10, 1),
                        tom = LocalDate.of(2021, 10, 31),
                    ).tilMinimertVedtaksperiode(),
                    minimerteEndredeUtbetalingAndeler = listOf(
                        lagEndretUtbetalingAndel(
                            prosent = BigDecimal.ZERO,
                            behandlingId = behandling.id,
                            person = barn,
                            fom = YearMonth.of(2021, 6),
                            tom = YearMonth.of(2021, 9),
                            årsak = Årsak.DELT_BOSTED,
                        ),
                    ).map { it.tilMinimertEndretUtbetalingAndel() },
                    erFørsteVedtaksperiodePåFagsak = false,
                    ytelserForSøkerForrigeMåned = emptyList(),
                    ytelserForrigePeriode = emptyList(),
                ),
        )
    }

    @Test
    fun `Oppfyller ikke etter endringsperiode skal gi false`() {
        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, barn)

        assertFalse(
            Standardbegrunnelse.ETTER_ENDRET_UTBETALING_AVTALE_DELT_BOSTED_FØLGES
                .triggesForPeriode(

                    sanityBegrunnelser = sanityBegrunnelser,
                    minimertePersoner = personopplysningGrunnlag.tilMinimertePersoner(),
                    minimertePersonResultater = vilkårsvurdering.personResultater.map { it.tilMinimertPersonResultat() },
                    aktørIderMedUtbetaling = aktørerMedUtbetaling.map { it.aktørId },
                    minimertVedtaksperiode = lagUtvidetVedtaksperiodeMedBegrunnelser(
                        type = Vedtaksperiodetype.UTBETALING,
                        fom = LocalDate.of(2021, 10, 1),
                        tom = LocalDate.of(2021, 10, 31),
                    ).tilMinimertVedtaksperiode(),
                    minimerteEndredeUtbetalingAndeler = listOf(
                        lagEndretUtbetalingAndel(
                            prosent = BigDecimal.ZERO,
                            behandlingId = behandling.id,
                            person = barn,
                            fom = YearMonth.of(2021, 10),
                            tom = YearMonth.of(2021, 10),
                            årsak = Årsak.DELT_BOSTED,
                        ),
                    ).map { it.tilMinimertEndretUtbetalingAndel() },
                    erFørsteVedtaksperiodePåFagsak = false,
                    ytelserForSøkerForrigeMåned = emptyList(),
                    ytelserForrigePeriode = emptyList(),
                ),
        )
    }

    @Test
    fun `Oppfyller skal utbetales gir false`() {
        assertFalse(
            lagEndretUtbetalingAndel(prosent = BigDecimal.ZERO, person = barn)
                .tilMinimertEndretUtbetalingAndel()
                .oppfyllerSkalUtbetalesTrigger(
                    triggesAv = lagTriggesAv(endretUtbetalingSkalUtbetales = EndretUtbetalingsperiodeDeltBostedTriggere.SKAL_UTBETALES),
                ),
        )

        assertFalse(
            lagEndretUtbetalingAndel(prosent = BigDecimal.valueOf(100), person = barn)
                .tilMinimertEndretUtbetalingAndel()
                .oppfyllerSkalUtbetalesTrigger(
                    triggesAv = lagTriggesAv(endretUtbetalingSkalUtbetales = EndretUtbetalingsperiodeDeltBostedTriggere.SKAL_IKKE_UTBETALES),
                ),
        )
    }

    @Test
    fun `Oppfyller skal utbetales gir true`() {
        assertTrue(
            lagEndretUtbetalingAndel(prosent = BigDecimal.ZERO, person = barn)
                .tilMinimertEndretUtbetalingAndel()
                .oppfyllerSkalUtbetalesTrigger(
                    triggesAv = lagTriggesAv(endretUtbetalingSkalUtbetales = EndretUtbetalingsperiodeDeltBostedTriggere.SKAL_IKKE_UTBETALES),
                ),
        )

        assertTrue(
            lagEndretUtbetalingAndel(prosent = BigDecimal.valueOf(100), person = barn)
                .tilMinimertEndretUtbetalingAndel()
                .oppfyllerSkalUtbetalesTrigger(
                    triggesAv = lagTriggesAv(endretUtbetalingSkalUtbetales = EndretUtbetalingsperiodeDeltBostedTriggere.SKAL_UTBETALES),
                ),
        )
    }

    @Test
    fun `Alle begrunnelser er unike`() {
        val vedtakBegrunnelser = Standardbegrunnelse.values().groupBy { it.sanityApiNavn }
        assertEquals(vedtakBegrunnelser.size, Standardbegrunnelse.values().size)
    }

    private fun String.startsWithUppercaseLetter(): Boolean {
        return this.matches(Regex("[A-Z]{1}.*"))
    }

    @Test
    fun `Dersom dødsfalldato ligger i forrige ytelse-periode skal begrunnelsen begrunnelser med trigger BARN_DØD trigges`() {
        val fnr = "12345678910"
        val dødtBarn = lagPerson(personIdent = PersonIdent(fnr), type = PersonType.BARN)
        dødtBarn.dødsfall = lagDødsfallFraPdl(
            dødtBarn,
            dødsfallDatoFraPdl = LocalDate.now().minusMonths(1).withDayOfMonth(15).toString(),
            dødsfallAdresseFraPdl = null,
        )
        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, dødtBarn)

        val reduksjonBarnDødBegrunnelse = mapOf(
            Standardbegrunnelse.REDUKSJON_BARN_DØD to SanityBegrunnelse(
                apiNavn = "reduksjonBarnDod",
                navnISystem = "barnDød",
                ovrigeTriggere = listOf(ØvrigTrigger.BARN_DØD),
            ),
        )

        val ytelserForrigeMåned = listOf(
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = YearMonth.of(
                    LocalDate.now().minusMonths(1).year,
                    LocalDate.now().minusMonths(1).month,
                ),
                tom = YearMonth.of(LocalDate.now().year, LocalDate.now().month),
                aktør = Aktør(fnr + "00").also { it.personidenter.add(Personident(fnr, it)) },
            ),
        )

        assertTrue(
            Standardbegrunnelse.REDUKSJON_BARN_DØD
                .triggesForPeriode(
                    sanityBegrunnelser = reduksjonBarnDødBegrunnelse,
                    minimertVedtaksperiode = utvidetVedtaksperiodeMedBegrunnelser.tilMinimertVedtaksperiode(),
                    minimertePersonResultater = vilkårsvurdering.personResultater.map { it.tilMinimertPersonResultat() },
                    minimertePersoner = personopplysningGrunnlag.tilMinimertePersoner(),
                    aktørIderMedUtbetaling = aktørerMedUtbetaling.map { it.aktørId },
                    erFørsteVedtaksperiodePåFagsak = false,
                    ytelserForSøkerForrigeMåned = emptyList(),
                    ytelserForrigePeriode = ytelserForrigeMåned,
                ),
        )
    }

    @Test
    fun `Dersom dødsfalldato ligger etter en ytelse-periode skal ikke begrunnelser med trigger BARN_DØD trigges`() {
        val fnr = "12345678910"
        val dødtBarn = lagPerson(personIdent = PersonIdent(fnr), type = PersonType.BARN)
        val dødsfallDato = LocalDate.now().minusMonths(1).withDayOfMonth(15)
        dødtBarn.dødsfall =
            lagDødsfallFraPdl(dødtBarn, dødsfallDatoFraPdl = dødsfallDato.toString(), dødsfallAdresseFraPdl = null)
        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, dødtBarn)

        val reduksjonBarnDødBegrunnelse = mapOf(
            Standardbegrunnelse.REDUKSJON_BARN_DØD to SanityBegrunnelse(
                apiNavn = "reduksjonBarnDod",
                navnISystem = "barnDød",
                ovrigeTriggere = listOf(ØvrigTrigger.BARN_DØD),
            ),
        )

        val ytelserForrigeMåned = listOf(
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = YearMonth.of(
                    dødsfallDato.minusMonths(5).year,
                    dødsfallDato.minusMonths(5).month,
                ),
                tom = YearMonth.of(dødsfallDato.minusMonths(1).year, dødsfallDato.minusMonths(1).month),
                aktør = Aktør(fnr + "00").also { it.personidenter.add(Personident(fnr, it)) },
            ),
        )

        assertFalse(
            Standardbegrunnelse.REDUKSJON_BARN_DØD
                .triggesForPeriode(
                    sanityBegrunnelser = reduksjonBarnDødBegrunnelse,
                    minimertVedtaksperiode = utvidetVedtaksperiodeMedBegrunnelser.tilMinimertVedtaksperiode(),
                    minimertePersonResultater = vilkårsvurdering.personResultater.map { it.tilMinimertPersonResultat() },
                    minimertePersoner = personopplysningGrunnlag.tilMinimertePersoner(),
                    aktørIderMedUtbetaling = aktørerMedUtbetaling.map { it.aktørId },
                    erFørsteVedtaksperiodePåFagsak = false,
                    ytelserForSøkerForrigeMåned = emptyList(),
                    ytelserForrigePeriode = ytelserForrigeMåned,
                ),
        )
    }

    @Test
    fun `Dersom dødsfalldato ligger før en ytelse-periode skal ikke begrunnelser med trigger BARN_DØD trigges`() {
        val fnr = "12345678910"
        val dødtBarn = lagPerson(personIdent = PersonIdent(fnr), type = PersonType.BARN)
        val dødsfallDato = LocalDate.now().minusMonths(1).withDayOfMonth(15)
        dødtBarn.dødsfall =
            lagDødsfallFraPdl(dødtBarn, dødsfallDatoFraPdl = dødsfallDato.toString(), dødsfallAdresseFraPdl = null)
        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, dødtBarn)

        val reduksjonBarnDødBegrunnelse = mapOf(
            Standardbegrunnelse.REDUKSJON_BARN_DØD to SanityBegrunnelse(
                apiNavn = "reduksjonBarnDod",
                navnISystem = "barnDød",
                ovrigeTriggere = listOf(ØvrigTrigger.BARN_DØD),
            ),
        )

        val ytelserForrigeMåned = listOf(
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = YearMonth.of(
                    dødsfallDato.plusMonths(5).year,
                    dødsfallDato.plusMonths(5).month,
                ),
                tom = YearMonth.of(dødsfallDato.plusMonths(6).year, dødsfallDato.plusMonths(6).month),
                aktør = Aktør(fnr + "00").also { it.personidenter.add(Personident(fnr, it)) },
            ),
        )

        assertFalse(
            Standardbegrunnelse.REDUKSJON_BARN_DØD
                .triggesForPeriode(
                    sanityBegrunnelser = reduksjonBarnDødBegrunnelse,
                    minimertVedtaksperiode = utvidetVedtaksperiodeMedBegrunnelser.tilMinimertVedtaksperiode(),
                    minimertePersonResultater = vilkårsvurdering.personResultater.map { it.tilMinimertPersonResultat() },
                    minimertePersoner = personopplysningGrunnlag.tilMinimertePersoner(),
                    aktørIderMedUtbetaling = aktørerMedUtbetaling.map { it.aktørId },
                    erFørsteVedtaksperiodePåFagsak = false,
                    ytelserForSøkerForrigeMåned = emptyList(),
                    ytelserForrigePeriode = ytelserForrigeMåned,
                ),
        )
    }

    @Test
    fun `dødeBarnForrigePeriode() skal returnere barn som døde i forrige periode og som er tilknyttet ytelsen`() {
        val barn1Fnr = "12345678910"
        val barn2Fnr = "12345678911"

        // Barn1 dør før Barn2.
        var dødsfallDatoBarn1 = LocalDate.of(2022, 5, 12)
        var dødsfallDatoBarn2 = LocalDate.of(2022, 7, 2)
        var barnIBehandling = listOf(
            lagMinimertPerson(dødsfallsdato = dødsfallDatoBarn1, aktivPersonIdent = barn1Fnr),
            lagMinimertPerson(dødsfallsdato = dødsfallDatoBarn2, aktivPersonIdent = barn2Fnr),
        )
        var ytelserForrigePeriode = listOf(
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = YearMonth.of(
                    dødsfallDatoBarn1.minusMonths(1).year,
                    dødsfallDatoBarn1.minusMonths(1).month,
                ),
                tom = YearMonth.of(dødsfallDatoBarn1.year, dødsfallDatoBarn1.month),
                aktør = Aktør(barn1Fnr + "00").also { it.personidenter.add(Personident(barn1Fnr, it)) },
            ),
        )

        var dødeBarnForrigePeriode = dødeBarnForrigePeriode(ytelserForrigePeriode, barnIBehandling)
        assertEquals(
            1,
            dødeBarnForrigePeriode.size,
        )
        assertEquals(
            barn1Fnr,
            dødeBarnForrigePeriode[0],
        )

        ytelserForrigePeriode = listOf(
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = YearMonth.of(
                    dødsfallDatoBarn1.minusMonths(1).year,
                    dødsfallDatoBarn1.minusMonths(1).month,
                ),
                tom = YearMonth.of(dødsfallDatoBarn2.year, dødsfallDatoBarn2.month),
                aktør = Aktør(barn2Fnr + "00").also { it.personidenter.add(Personident(barn2Fnr, it)) },
            ),
        )

        dødeBarnForrigePeriode = dødeBarnForrigePeriode(ytelserForrigePeriode, barnIBehandling)
        assertEquals(
            1,
            dødeBarnForrigePeriode.size,
        )
        assertEquals(
            barn2Fnr,
            dødeBarnForrigePeriode[0],
        )

        // Barn1 og Barn2 dør i samme måned
        dødsfallDatoBarn1 = LocalDate.of(2022, 5, 12)
        dødsfallDatoBarn2 = LocalDate.of(2022, 5, 2)

        barnIBehandling = listOf(
            lagMinimertPerson(dødsfallsdato = dødsfallDatoBarn1, aktivPersonIdent = barn1Fnr),
            lagMinimertPerson(dødsfallsdato = dødsfallDatoBarn2, aktivPersonIdent = barn2Fnr),
        )

        ytelserForrigePeriode = listOf(
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = YearMonth.of(
                    dødsfallDatoBarn1.minusMonths(1).year,
                    dødsfallDatoBarn1.minusMonths(1).month,
                ),
                tom = YearMonth.of(dødsfallDatoBarn1.year, dødsfallDatoBarn1.month),
                aktør = Aktør(barn1Fnr + "00").also { it.personidenter.add(Personident(barn1Fnr, it)) },
            ),
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                fom = YearMonth.of(dødsfallDatoBarn2.minusMonths(1).year, dødsfallDatoBarn2.minusMonths(1).month),
                tom = YearMonth.of(dødsfallDatoBarn2.year, dødsfallDatoBarn2.month),
                aktør = Aktør(barn2Fnr + "00").also { it.personidenter.add(Personident(barn2Fnr, it)) },
            ),
        )

        dødeBarnForrigePeriode = dødeBarnForrigePeriode(ytelserForrigePeriode, barnIBehandling)
        assertEquals(
            2,
            dødeBarnForrigePeriode.size,
        )
        assertTrue(
            dødeBarnForrigePeriode.containsAll(barnIBehandling.map { it.aktivPersonIdent }),
        )
    }
}
