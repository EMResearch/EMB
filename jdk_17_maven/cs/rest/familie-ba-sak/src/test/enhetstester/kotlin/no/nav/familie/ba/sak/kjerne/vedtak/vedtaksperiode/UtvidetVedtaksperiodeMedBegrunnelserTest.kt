package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagEndretUtbetalingAndel
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.lagVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.common.tilfeldigSøker
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestPerson
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.tilUtvidetVedtaksperiodeMedBegrunnelser
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.YearMonth

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UtvidetVedtaksperiodeMedBegrunnelserTest {

    val barn1 = tilfeldigPerson(personType = PersonType.BARN)
    val barn2 = tilfeldigPerson(personType = PersonType.BARN)
    val barn3 = tilfeldigPerson(personType = PersonType.BARN)
    val søker = tilfeldigSøker()

    @Test
    fun `Skal kun legge på utbetalingsdetaljer som gjelder riktig andeler tilkjent ytelse for fortsatt innvilget`() {
        val behandling = lagBehandling()

        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(
            behandlingId = behandling.id,
            barnasIdenter = listOf(barn1.aktør.aktivFødselsnummer(), barn2.aktør.aktivFødselsnummer()),
            søkerPersonIdent = søker.aktør.aktivFødselsnummer(),
            søkerAktør = søker.aktør,
            barnAktør = listOf(barn1.aktør, barn2.aktør),
        )

        val fom = YearMonth.of(2018, 6)
        val tom = YearMonth.of(2018, 8)

        val endretUtbetalingAndel = lagEndretUtbetalingAndel(
            behandlingId = behandling.id,
            fom = fom,
            tom = tom,
            person = barn2,
        )

        val andelerTilkjentYtelse = listOf(
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                behandling = behandling,
                endretUtbetalingAndeler = emptyList(),
                fom = fom.minusMonths(2),
                tom = tom,
                person = barn1,
            ),
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                behandling = behandling,
                endretUtbetalingAndeler = listOf(endretUtbetalingAndel),
                fom = fom,
                tom = tom,
                person = barn2,
            ),
            lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                behandling = behandling,
                endretUtbetalingAndeler = emptyList(),
                fom = tom.plusMonths(1),
                tom = tom.plusMonths(3),
                person = barn1,
            ),
        )

        val vedtaksperiodeMedBegrunnelser = lagVedtaksperiodeMedBegrunnelser(
            fom = null,
            tom = null,
            type = Vedtaksperiodetype.FORTSATT_INNVILGET,
        )

        val utvidetVedtaksperiodeMedBegrunnelser =
            vedtaksperiodeMedBegrunnelser.tilUtvidetVedtaksperiodeMedBegrunnelser(
                personopplysningGrunnlag = personopplysningGrunnlag,
                andelerTilkjentYtelse = andelerTilkjentYtelse,
            )

        Assertions.assertEquals(1, utvidetVedtaksperiodeMedBegrunnelser.utbetalingsperiodeDetaljer.size)
        Assertions.assertEquals(
            barn1.tilRestPerson().personIdent,
            utvidetVedtaksperiodeMedBegrunnelser.utbetalingsperiodeDetaljer.single().person.personIdent,
        )
        Assertions.assertFalse(utvidetVedtaksperiodeMedBegrunnelser.utbetalingsperiodeDetaljer.single().erPåvirketAvEndring)
    }
}
