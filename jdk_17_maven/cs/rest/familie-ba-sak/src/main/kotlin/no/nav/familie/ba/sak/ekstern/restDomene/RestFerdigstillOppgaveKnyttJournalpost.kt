package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import java.time.LocalDateTime

data class RestFerdigstillOppgaveKnyttJournalpost(
    val journalpostId: String,
    val tilknyttedeBehandlingIder: List<String> = emptyList(),
    val opprettOgKnyttTilNyBehandling: Boolean = false,
    val navIdent: String,
    val bruker: NavnOgIdent,
    val nyBehandlingstype: BehandlingType,
    val nyBehandlingsårsak: BehandlingÅrsak,
    val kategori: BehandlingKategori?,
    val underkategori: BehandlingUnderkategori?,
    val datoMottatt: LocalDateTime?,
)
