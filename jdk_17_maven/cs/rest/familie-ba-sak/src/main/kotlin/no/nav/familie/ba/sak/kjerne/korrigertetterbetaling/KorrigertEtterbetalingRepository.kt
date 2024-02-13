package no.nav.familie.ba.sak.kjerne.korrigertetterbetaling

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface KorrigertEtterbetalingRepository : JpaRepository<KorrigertEtterbetaling, Long> {
    @Query("SELECT ke FROM KorrigertEtterbetaling ke JOIN ke.behandling b WHERE b.id = :behandlingId AND ke.aktiv = true")
    fun finnAktivtKorrigeringPåBehandling(behandlingId: Long): KorrigertEtterbetaling?

    @Query("SELECT ke FROM KorrigertEtterbetaling ke JOIN ke.behandling b WHERE b.id = :behandlingId")
    fun finnAlleKorrigeringerPåBehandling(behandlingId: Long): List<KorrigertEtterbetaling>
}
