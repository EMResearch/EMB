package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.common.NullablePeriode
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagEndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.lagTriggesAv
import no.nav.familie.ba.sak.common.lagVilkårsvurdering
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.brev.domene.RestBehandlingsgrunnlagForBrev
import no.nav.familie.ba.sak.kjerne.brev.domene.tilMinimertPersonResultat
import no.nav.familie.ba.sak.kjerne.brev.domene.tilMinimertRestEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.brev.hentPersonidenterGjeldendeForBegrunnelse
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.periodeErOppyltForYtelseType
import no.nav.familie.ba.sak.kjerne.vedtak.domene.tilMinimertPerson
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VedtaksperiodeServiceUtilsTest {

    @Test
    fun `Skal legge til alle barn med utbetaling ved utvidet barnetrygd`() {
        val behandling = lagBehandling()
        val søker = lagPerson(type = PersonType.SØKER)
        val barn = lagPerson(type = PersonType.BARN)

        val persongrunnlag =
            lagTestPersonopplysningGrunnlag(behandlingId = behandling.id, personer = arrayOf(søker, barn))
        val triggesAv = lagTriggesAv(vilkår = setOf(Vilkår.UTVIDET_BARNETRYGD))
        val vilkårsvurdering = lagVilkårsvurdering(
            søkerAktør = søker.aktør,
            behandling = behandling,
            resultat = Resultat.OPPFYLT,
        )
        val identerMedUtbetaling = listOf(barn.aktør.aktivFødselsnummer(), søker.aktør.aktivFødselsnummer())

        val personidenterForBegrunnelse = hentPersonidenterGjeldendeForBegrunnelse(
            triggesAv = triggesAv,
            vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET,
            vedtaksperiodetype = Vedtaksperiodetype.UTBETALING,
            periode = NullablePeriode(LocalDate.now().minusMonths(1), null),
            restBehandlingsgrunnlagForBrev = RestBehandlingsgrunnlagForBrev(
                personerPåBehandling = persongrunnlag.personer.map { it.tilMinimertPerson() },
                minimertePersonResultater = vilkårsvurdering.personResultater.map { it.tilMinimertPersonResultat() },
                minimerteEndredeUtbetalingAndeler = emptyList(),
                fagsakType = FagsakType.NORMAL,
            ),
            identerMedUtbetalingPåPeriode = identerMedUtbetaling,
            erFørsteVedtaksperiodePåFagsak = false,
            minimerteUtbetalingsperiodeDetaljer = listOf(),
            dødeBarnForrigePeriode = emptyList(),
            begrunnelse = Standardbegrunnelse.INNVILGET_BOR_ALENE_MED_BARN,
        )

        Assertions.assertEquals(
            setOf(barn.aktør.aktivFødselsnummer(), søker.aktør.aktivFødselsnummer()),
            personidenterForBegrunnelse,
        )
    }

    @Test
    fun `Skal legge til alle barn fra endret utbetaling ved utvidet barnetrygd og endret utbetaling`() {
        val behandling = lagBehandling()
        val søker = lagPerson(type = PersonType.SØKER)
        val barn1 = lagPerson(type = PersonType.BARN)
        val barn2 = lagPerson(type = PersonType.BARN)

        val fom = LocalDate.now().withDayOfMonth(1)
        val tom = LocalDate.now().let {
            it.withDayOfMonth(it.lengthOfMonth())
        }

        val persongrunnlag =
            lagTestPersonopplysningGrunnlag(behandlingId = behandling.id, personer = arrayOf(søker, barn2))
        val triggesAv = lagTriggesAv(vilkår = setOf(Vilkår.UTVIDET_BARNETRYGD))
        val vilkårsvurdering = lagVilkårsvurdering(
            søkerAktør = søker.aktør,
            behandling = behandling,
            resultat = Resultat.OPPFYLT,
        )

        val identerMedUtbetaling = listOf(barn1.aktør.aktivFødselsnummer(), søker.aktør.aktivFødselsnummer())
        val endredeUtbetalingAndeler = listOf(
            lagEndretUtbetalingAndelMedAndelerTilkjentYtelse(
                person = barn2,
                fom = fom.toYearMonth(),
                tom = tom.toYearMonth(),
            ),
        )

        val personidenterForBegrunnelse = hentPersonidenterGjeldendeForBegrunnelse(
            triggesAv = triggesAv,
            periode = NullablePeriode(fom, tom),
            vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET,
            vedtaksperiodetype = Vedtaksperiodetype.UTBETALING,
            restBehandlingsgrunnlagForBrev = RestBehandlingsgrunnlagForBrev(
                personerPåBehandling = persongrunnlag.personer.map { it.tilMinimertPerson() },
                minimertePersonResultater = vilkårsvurdering.personResultater.map { it.tilMinimertPersonResultat() },
                minimerteEndredeUtbetalingAndeler = endredeUtbetalingAndeler
                    .map { it.tilMinimertRestEndretUtbetalingAndel() },
                fagsakType = FagsakType.NORMAL,
            ),
            identerMedUtbetalingPåPeriode = identerMedUtbetaling,
            erFørsteVedtaksperiodePåFagsak = false,
            minimerteUtbetalingsperiodeDetaljer = listOf(),
            dødeBarnForrigePeriode = emptyList(),
            begrunnelse = Standardbegrunnelse.INNVILGET_BOR_ALENE_MED_BARN,
        )

        Assertions.assertEquals(
            setOf(barn1.aktør.aktivFødselsnummer(), barn2.aktør.aktivFødselsnummer(), søker.aktør.aktivFødselsnummer()),
            personidenterForBegrunnelse.toSet(),
        )
    }

    val ytelseTyperSmåbarnstillegg =
        setOf(YtelseType.SMÅBARNSTILLEGG, YtelseType.UTVIDET_BARNETRYGD, YtelseType.ORDINÆR_BARNETRYGD)
    val ytelseTyperUtvidetOgOrdinær =
        setOf(YtelseType.UTVIDET_BARNETRYGD, YtelseType.ORDINÆR_BARNETRYGD)
    val ytelseTyperOrdinær =
        setOf(YtelseType.ORDINÆR_BARNETRYGD)

    @Test
    fun `Skal gi riktig svar for småbarnstillegg-trigger ved innvilget VedtakBegrunnelseType`() {
        Assertions.assertEquals(
            true,
            VedtakBegrunnelseType.INNVILGET.periodeErOppyltForYtelseType(
                ytelseType = YtelseType.SMÅBARNSTILLEGG,
                ytelseTyperForPeriode = ytelseTyperSmåbarnstillegg,
                ytelserGjeldeneForSøkerForrigeMåned = emptyList(),
            ),
        )

        Assertions.assertEquals(
            false,
            VedtakBegrunnelseType.INNVILGET.periodeErOppyltForYtelseType(
                ytelseType = YtelseType.SMÅBARNSTILLEGG,
                ytelseTyperForPeriode = ytelseTyperUtvidetOgOrdinær,
                ytelserGjeldeneForSøkerForrigeMåned = emptyList(),
            ),
        )
    }

    @Test
    fun `Skal gi riktig svar for småbarnstillegg-trigger når VedtakBegrunnelseType er reduksjon`() {
        Assertions.assertEquals(
            true,
            VedtakBegrunnelseType.REDUKSJON.periodeErOppyltForYtelseType(
                ytelseType = YtelseType.SMÅBARNSTILLEGG,
                ytelseTyperForPeriode = ytelseTyperUtvidetOgOrdinær,
                ytelserGjeldeneForSøkerForrigeMåned = listOf(YtelseType.SMÅBARNSTILLEGG),
            ),
        )

        Assertions.assertEquals(
            false,
            VedtakBegrunnelseType.REDUKSJON.periodeErOppyltForYtelseType(
                ytelseType = YtelseType.SMÅBARNSTILLEGG,
                ytelseTyperForPeriode = ytelseTyperSmåbarnstillegg,
                ytelserGjeldeneForSøkerForrigeMåned = listOf(YtelseType.SMÅBARNSTILLEGG),
            ),
        )

        Assertions.assertEquals(
            false,
            VedtakBegrunnelseType.REDUKSJON.periodeErOppyltForYtelseType(
                ytelseType = YtelseType.SMÅBARNSTILLEGG,
                ytelseTyperForPeriode = ytelseTyperUtvidetOgOrdinær,
                ytelserGjeldeneForSøkerForrigeMåned = listOf(YtelseType.ORDINÆR_BARNETRYGD),
            ),
        )

        Assertions.assertEquals(
            false,
            VedtakBegrunnelseType.REDUKSJON.periodeErOppyltForYtelseType(
                ytelseType = YtelseType.SMÅBARNSTILLEGG,
                ytelseTyperForPeriode = ytelseTyperUtvidetOgOrdinær,
                ytelserGjeldeneForSøkerForrigeMåned = listOf(),
            ),
        )
    }

    @Test
    fun `Skal gi false når VedtakBegrunnelseType ikke er innvilget eller reduksjon `() {
        Assertions.assertEquals(
            false,
            VedtakBegrunnelseType.AVSLAG.periodeErOppyltForYtelseType(
                ytelseType = YtelseType.SMÅBARNSTILLEGG,
                ytelseTyperForPeriode = ytelseTyperSmåbarnstillegg,
                ytelserGjeldeneForSøkerForrigeMåned = emptyList(),
            ),
        )
    }

    @Test
    fun `Skal gi riktig svar for utvidet-trigger ved innvilget`() {
        Assertions.assertEquals(
            true,
            VedtakBegrunnelseType.INNVILGET.periodeErOppyltForYtelseType(
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                ytelseTyperForPeriode = ytelseTyperUtvidetOgOrdinær,
                ytelserGjeldeneForSøkerForrigeMåned = emptyList(),
            ),
        )

        Assertions.assertEquals(
            false,
            VedtakBegrunnelseType.INNVILGET.periodeErOppyltForYtelseType(
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                ytelseTyperForPeriode = ytelseTyperOrdinær,
                ytelserGjeldeneForSøkerForrigeMåned = emptyList(),
            ),
        )
    }

    @Test
    fun `Skal gi riktig svar for utvidet barnetrygd-trigger når VedtakBegrunnelseType er reduksjon`() {
        Assertions.assertEquals(
            true,
            VedtakBegrunnelseType.REDUKSJON.periodeErOppyltForYtelseType(
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                ytelseTyperForPeriode = ytelseTyperOrdinær,
                ytelserGjeldeneForSøkerForrigeMåned = listOf(YtelseType.UTVIDET_BARNETRYGD),
            ),
        )

        Assertions.assertEquals(
            false,
            VedtakBegrunnelseType.REDUKSJON.periodeErOppyltForYtelseType(
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                ytelseTyperForPeriode = ytelseTyperUtvidetOgOrdinær,
                ytelserGjeldeneForSøkerForrigeMåned = listOf(YtelseType.UTVIDET_BARNETRYGD),
            ),
        )

        Assertions.assertEquals(
            false,
            VedtakBegrunnelseType.REDUKSJON.periodeErOppyltForYtelseType(
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                ytelseTyperForPeriode = ytelseTyperOrdinær,
                ytelserGjeldeneForSøkerForrigeMåned = listOf(YtelseType.ORDINÆR_BARNETRYGD),
            ),
        )

        Assertions.assertEquals(
            false,
            VedtakBegrunnelseType.REDUKSJON.periodeErOppyltForYtelseType(
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                ytelseTyperForPeriode = ytelseTyperOrdinær,
                ytelserGjeldeneForSøkerForrigeMåned = listOf(),
            ),
        )
    }
}
