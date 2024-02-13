package no.nav.familie.ba.sak.integrasjoner.oppgave.domene

import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface OppgaveRepository : JpaRepository<DbOppgave, Long> {

    @Query(value = "SELECT o FROM Oppgave o WHERE o.erFerdigstilt = false AND o.behandling = :behandling AND o.type = :oppgavetype")
    fun findByOppgavetypeAndBehandlingAndIkkeFerdigstilt(oppgavetype: Oppgavetype, behandling: Behandling): DbOppgave?

    @Query(value = "SELECT o FROM Oppgave o WHERE o.erFerdigstilt = false AND o.behandling = :behandling AND o.type = :oppgavetype")
    fun finnOppgaverSomSkalFerdigstilles(oppgavetype: Oppgavetype, behandling: Behandling): List<DbOppgave>

    @Query(value = "SELECT o FROM Oppgave o WHERE o.erFerdigstilt = false AND o.behandling = :behandling")
    fun findByBehandlingAndIkkeFerdigstilt(behandling: Behandling): List<DbOppgave>

    @Query(value = "SELECT o FROM Oppgave o WHERE o.erFerdigstilt = false AND o.behandling.id = :behandlingId")
    fun findByBehandlingIdAndIkkeFerdigstilt(behandlingId: Long): List<DbOppgave>

    fun findByGsakId(gsakId: String): DbOppgave?

    @Query(value = "SELECT o FROM Oppgave o WHERE o.erFerdigstilt = true AND o.behandling.id = :behandlingId AND o.type = :oppgavetype")
    fun findByBehandlingAndTypeAndErFerdigstilt(behandlingId: Long, oppgavetype: Oppgavetype): List<DbOppgave>
}
