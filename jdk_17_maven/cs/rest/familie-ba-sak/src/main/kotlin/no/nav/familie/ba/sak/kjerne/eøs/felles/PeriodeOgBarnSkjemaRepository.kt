package no.nav.familie.ba.sak.kjerne.eøs.felles

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.NoRepositoryBean

interface FinnPeriodeOgBarnSkjemaRepository<S : PeriodeOgBarnSkjemaEntitet<S>> {
    fun finnFraBehandlingId(behandlingId: Long): Collection<S>
}

@NoRepositoryBean
interface PeriodeOgBarnSkjemaRepository<S : PeriodeOgBarnSkjemaEntitet<S>> :
    JpaRepository<S, Long>, FinnPeriodeOgBarnSkjemaRepository<S>
