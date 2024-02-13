package no.nav.familie.ba.sak.datagenerator.endretUtbetaling

import no.nav.familie.ba.sak.common.randomAktør
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertEndretAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import java.math.BigDecimal
import java.time.YearMonth

fun lagMinimertEndretUtbetalingAndel(
    aktørId: String = randomAktør(randomFnr()).aktørId,
    fom: YearMonth? = YearMonth.now(),
    tom: YearMonth? = YearMonth.now(),
    årsak: Årsak? = Årsak.DELT_BOSTED,
    prosent: BigDecimal? = BigDecimal.valueOf(100),
) = MinimertEndretAndel(
    aktørId = aktørId,
    fom = fom,
    tom = tom,
    årsak = årsak,
    prosent = prosent,
)
