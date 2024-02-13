package no.nav.familie.ba.sak.kjerne.grunnlag.søknad

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface SøknadGrunnlagRepository : JpaRepository<SøknadGrunnlag, Long> {

    @Query("SELECT gr FROM SøknadGrunnlag gr WHERE gr.behandlingId = :behandlingId AND gr.aktiv = true")
    fun hentAktiv(behandlingId: Long): SøknadGrunnlag?

    @Query("SELECT gr FROM SøknadGrunnlag gr WHERE gr.behandlingId = :behandlingId")
    fun hent(behandlingId: Long): SøknadGrunnlag

    @Query("SELECT gr FROM SøknadGrunnlag gr WHERE gr.behandlingId = :behandlingId")
    fun hentAlle(behandlingId: Long): List<SøknadGrunnlag>
}
