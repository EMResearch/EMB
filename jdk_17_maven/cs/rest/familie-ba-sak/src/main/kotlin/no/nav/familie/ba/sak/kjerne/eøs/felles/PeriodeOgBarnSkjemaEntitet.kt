package no.nav.familie.ba.sak.kjerne.eøs.felles

import jakarta.persistence.MappedSuperclass
import no.nav.familie.ba.sak.common.BaseEntitet

@MappedSuperclass
abstract class PeriodeOgBarnSkjemaEntitet<T : PeriodeOgBarnSkjema<T>> :
    BaseEntitet(),
    PeriodeOgBarnSkjema<T> {

    abstract var id: Long
    abstract var behandlingId: Long
}
