package no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.domene

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface FødselshendelsefiltreringResultatRepository : JpaRepository<FødselshendelsefiltreringResultat, Long> {

    @Query(value = "SELECT f FROM FødselshendelsefiltreringResultat f WHERE f.behandlingId = :behandlingId")
    fun finnFødselshendelsefiltreringResultater(behandlingId: Long): List<FødselshendelsefiltreringResultat>
}
