package no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser

import no.nav.familie.ba.sak.common.lagTriggesAv
import no.nav.familie.ba.sak.datagenerator.brev.lagMinimertUtbetalingsperiodeDetalj
import no.nav.familie.ba.sak.datagenerator.endretUtbetaling.lagMinimertEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.brev.domene.EndretUtbetalingsperiodeDeltBostedTriggere
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class TriggesAvTest {

    val vilkårUtenUtvidetBarnetrygd: Set<Vilkår> = emptySet()
    val vilkårMedUtvidetBarnetrygd: Set<Vilkår> = setOf(Vilkår.UTVIDET_BARNETRYGD)

    val endretUtbetalingAndelNull =
        lagMinimertEndretUtbetalingAndel(
            prosent = BigDecimal.ZERO,
            årsak = Årsak.DELT_BOSTED,
        )
    val endretUtbetalingAndelIkkeNull =
        lagMinimertEndretUtbetalingAndel(
            prosent = BigDecimal.ONE,
            årsak = Årsak.DELT_BOSTED,
        )

    val triggesAvEtterEndretUtbetaling = lagTriggesAv(
        etterEndretUtbetaling = true,
        endretUtbetalingSkalUtbetales = EndretUtbetalingsperiodeDeltBostedTriggere.SKAL_UTBETALES,
        endringsaarsaker = setOf(
            Årsak.DELT_BOSTED,
        ),
        vilkår = vilkårMedUtvidetBarnetrygd,
    )

    val triggesIkkeAvSkalUtbetalesMedUtvidetVilkår =
        lagTriggesAv(
            endretUtbetalingSkalUtbetales = EndretUtbetalingsperiodeDeltBostedTriggere.SKAL_IKKE_UTBETALES,
            etterEndretUtbetaling = false,
            endringsaarsaker = setOf(
                Årsak.DELT_BOSTED,
            ),
            vilkår = vilkårMedUtvidetBarnetrygd,
        )

    val triggesIkkeAvSkalUtbetalesUtenUtvidetVilkår =
        lagTriggesAv(
            endretUtbetalingSkalUtbetales = EndretUtbetalingsperiodeDeltBostedTriggere.SKAL_IKKE_UTBETALES,
            etterEndretUtbetaling = false,
            endringsaarsaker = setOf(
                Årsak.DELT_BOSTED,
            ),
            vilkår = vilkårUtenUtvidetBarnetrygd,
        )

    val triggesAvSkalUtbetalesMedUtvidetVilkår = lagTriggesAv(
        endretUtbetalingSkalUtbetales = EndretUtbetalingsperiodeDeltBostedTriggere.SKAL_UTBETALES,
        etterEndretUtbetaling = false,
        endringsaarsaker = setOf(
            Årsak.DELT_BOSTED,
        ),
        vilkår = vilkårMedUtvidetBarnetrygd,
    )

    val triggesAvSkalUtbetalesUtenUtvidetVilkår = lagTriggesAv(
        endretUtbetalingSkalUtbetales = EndretUtbetalingsperiodeDeltBostedTriggere.SKAL_UTBETALES,
        etterEndretUtbetaling = false,
        endringsaarsaker = setOf(
            Årsak.DELT_BOSTED,
        ),
        vilkår = vilkårUtenUtvidetBarnetrygd,
    )

    @Test
    fun `Skal gi false dersom er etter endret utbetaling`() {
        val erEtterEndretUbetaling = triggesAvEtterEndretUtbetaling.erTriggereOppfyltForEndretUtbetaling(
            minimertEndretAndel = endretUtbetalingAndelIkkeNull,
            minimerteUtbetalingsperiodeDetaljer = emptyList(),
        )

        Assertions.assertFalse(erEtterEndretUbetaling)

        val erEtterEndretUbetalingMedToggle = triggesAvEtterEndretUtbetaling.erTriggereOppfyltForEndretUtbetaling(
            minimertEndretAndel = endretUtbetalingAndelIkkeNull,
            minimerteUtbetalingsperiodeDetaljer = listOf(
                lagMinimertUtbetalingsperiodeDetalj(
                    ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                ),
            ),

        )
        Assertions.assertFalse(erEtterEndretUbetalingMedToggle)
    }

    @Test
    fun `Triggere for endret utbetaling-begrunnelser skal bli true ved riktig utbetalingsandel`() {
        val skalUtbetalesMedUtbetaling =
            triggesAvSkalUtbetalesMedUtvidetVilkår.erTriggereOppfyltForEndretUtbetaling(
                minimertEndretAndel = endretUtbetalingAndelIkkeNull,
                minimerteUtbetalingsperiodeDetaljer = listOf(
                    lagMinimertUtbetalingsperiodeDetalj(
                        ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                        erPåvirketAvEndring = true,
                    ),
                ),
            )

        val skalUtbetalesUtenUtbetaling =
            triggesAvSkalUtbetalesMedUtvidetVilkår.erTriggereOppfyltForEndretUtbetaling(
                minimertEndretAndel = endretUtbetalingAndelNull,
                minimerteUtbetalingsperiodeDetaljer = listOf(
                    lagMinimertUtbetalingsperiodeDetalj(
                        ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                        erPåvirketAvEndring = true,
                    ),
                ),
            )

        val skalIkkeUtbetalesUtenUtbetaling =
            triggesIkkeAvSkalUtbetalesMedUtvidetVilkår.erTriggereOppfyltForEndretUtbetaling(
                minimertEndretAndel = endretUtbetalingAndelNull,
                minimerteUtbetalingsperiodeDetaljer = listOf(
                    lagMinimertUtbetalingsperiodeDetalj(
                        ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                        erPåvirketAvEndring = true,
                    ),
                ),
            )

        val skalIkkeUtbetalesMedUtbetaling =
            triggesIkkeAvSkalUtbetalesMedUtvidetVilkår.erTriggereOppfyltForEndretUtbetaling(
                minimertEndretAndel = endretUtbetalingAndelIkkeNull,
                minimerteUtbetalingsperiodeDetaljer = listOf(
                    lagMinimertUtbetalingsperiodeDetalj(
                        ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                        erPåvirketAvEndring = true,
                    ),
                ),
            )

        Assertions.assertTrue(skalUtbetalesMedUtbetaling)
        Assertions.assertFalse(skalUtbetalesUtenUtbetaling)
        Assertions.assertTrue(skalIkkeUtbetalesUtenUtbetaling)
        Assertions.assertFalse(skalIkkeUtbetalesMedUtbetaling)
    }

    @Test
    fun `Skal gi riktig resultat for om endret utbetaling begrunnelse trigges ved utvidetScenario`() {
        Assertions.assertTrue(
            triggesAvSkalUtbetalesMedUtvidetVilkår.erTriggereOppfyltForEndretUtbetaling(
                minimertEndretAndel = endretUtbetalingAndelIkkeNull,
                minimerteUtbetalingsperiodeDetaljer = listOf(
                    lagMinimertUtbetalingsperiodeDetalj(
                        ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                        erPåvirketAvEndring = true,
                    ),
                ),
            ),
        )

        Assertions.assertFalse(
            triggesAvSkalUtbetalesMedUtvidetVilkår.erTriggereOppfyltForEndretUtbetaling(
                minimertEndretAndel = endretUtbetalingAndelIkkeNull,
                minimerteUtbetalingsperiodeDetaljer = listOf(
                    lagMinimertUtbetalingsperiodeDetalj(ytelseType = YtelseType.ORDINÆR_BARNETRYGD),
                ),
            ),
        )

        Assertions.assertTrue(
            triggesAvSkalUtbetalesUtenUtvidetVilkår.erTriggereOppfyltForEndretUtbetaling(
                minimertEndretAndel = endretUtbetalingAndelIkkeNull,
                minimerteUtbetalingsperiodeDetaljer = listOf(
                    lagMinimertUtbetalingsperiodeDetalj(
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    ),
                ),
            ),
        )

        Assertions.assertFalse(
            triggesAvSkalUtbetalesUtenUtvidetVilkår.erTriggereOppfyltForEndretUtbetaling(
                minimertEndretAndel = endretUtbetalingAndelIkkeNull,
                minimerteUtbetalingsperiodeDetaljer = listOf(
                    lagMinimertUtbetalingsperiodeDetalj(
                        ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                        erPåvirketAvEndring = true,
                    ),
                ),
            ),
        )

        Assertions.assertFalse(
            triggesAvSkalUtbetalesMedUtvidetVilkår.erTriggereOppfyltForEndretUtbetaling(
                minimertEndretAndel = endretUtbetalingAndelIkkeNull,
                minimerteUtbetalingsperiodeDetaljer = listOf(
                    lagMinimertUtbetalingsperiodeDetalj(
                        ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                        erPåvirketAvEndring = false,
                    ),
                ),
            ),
        )

        Assertions.assertTrue(
            triggesAvSkalUtbetalesUtenUtvidetVilkår.erTriggereOppfyltForEndretUtbetaling(
                minimertEndretAndel = endretUtbetalingAndelIkkeNull,
                minimerteUtbetalingsperiodeDetaljer = listOf(
                    lagMinimertUtbetalingsperiodeDetalj(
                        ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                        erPåvirketAvEndring = false,
                    ),
                ),
            ),
        )
    }

    @Test
    fun `Skal ikke være oppfylt hvis endringsperiode og triggesav ulik årsak`() {
        val endretUtbetalingAndel = lagMinimertEndretUtbetalingAndel(
            prosent = BigDecimal.ZERO,
            årsak = Årsak.ALLEREDE_UTBETALT,
        )

        Assertions.assertFalse(
            triggesIkkeAvSkalUtbetalesUtenUtvidetVilkår.erTriggereOppfyltForEndretUtbetaling(
                minimertEndretAndel = endretUtbetalingAndel,
                minimerteUtbetalingsperiodeDetaljer = emptyList(),
            ),
        )

        Assertions.assertFalse(
            triggesIkkeAvSkalUtbetalesUtenUtvidetVilkår.erTriggereOppfyltForEndretUtbetaling(
                minimertEndretAndel = endretUtbetalingAndel,
                minimerteUtbetalingsperiodeDetaljer = listOf(
                    lagMinimertUtbetalingsperiodeDetalj(
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    ),
                ),

            ),
        )
    }
}
