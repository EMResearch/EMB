package no.nav.familie.ba.sak.kjerne.vedtak

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface VedtakRepository : JpaRepository<Vedtak, Long> {

    @Query(value = "SELECT v FROM Vedtak v WHERE v.behandling.id = :behandlingId")
    fun finnVedtakForBehandling(behandlingId: Long): List<Vedtak>

    @Query("SELECT v FROM Vedtak v WHERE v.behandling.id = :behandlingId AND v.aktiv = true")
    fun findByBehandlingAndAktivOptional(behandlingId: Long): Vedtak?

    @Query("SELECT v FROM Vedtak v WHERE v.behandling.id = :behandlingId AND v.aktiv = true")
    fun findByBehandlingAndAktiv(behandlingId: Long): Vedtak

    @Query("SELECT v.vedtaksdato FROM Vedtak v WHERE v.behandling.id = :behandlingId AND v.aktiv = true")
    fun finnVedtaksdatoForBehandling(behandlingId: Long): LocalDateTime?
}
