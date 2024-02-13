package no.nav.familie.ba.sak.datagenerator.brev

import no.nav.familie.ba.sak.common.tilfeldigSøker
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestPerson
import no.nav.familie.ba.sak.integrasjoner.økonomi.sats
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertUtbetalingsperiodeDetalj
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.vedtak.domene.MinimertRestPerson
import no.nav.familie.ba.sak.kjerne.vedtak.domene.tilMinimertPerson
import java.math.BigDecimal

fun lagMinimertUtbetalingsperiodeDetalj(
    person: MinimertRestPerson = tilfeldigSøker().tilRestPerson().tilMinimertPerson(),
    ytelseType: YtelseType = YtelseType.ORDINÆR_BARNETRYGD,
    utbetaltPerMnd: Int = sats(YtelseType.ORDINÆR_BARNETRYGD),
    prosent: BigDecimal = BigDecimal.valueOf(100),
    erPåvirketAvEndring: Boolean = false,
    endringsårsak: Årsak? = null,
) = MinimertUtbetalingsperiodeDetalj(person, ytelseType, utbetaltPerMnd, erPåvirketAvEndring, endringsårsak, prosent)
