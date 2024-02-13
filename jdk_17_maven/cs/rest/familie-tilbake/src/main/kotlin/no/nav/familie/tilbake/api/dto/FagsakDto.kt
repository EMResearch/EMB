package no.nav.familie.tilbake.api.dto

import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.Språkkode
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.behandling.domain.Behandlingstype
import no.nav.familie.tilbake.integration.pdl.internal.Kjønn
import java.time.LocalDate
import java.util.UUID

data class FagsakDto(
    val eksternFagsakId: String,
    val ytelsestype: Ytelsestype,
    val fagsystem: Fagsystem,
    val språkkode: Språkkode,
    val bruker: BrukerDto,
    val behandlinger: List<BehandlingsoppsummeringDto>,
    val institusjon: InstitusjonDto? = null,
)

data class BrukerDto(
    val personIdent: String,
    val navn: String,
    val fødselsdato: LocalDate,
    val kjønn: Kjønn,
    val dødsdato: LocalDate? = null,
)

data class BehandlingsoppsummeringDto(
    val behandlingId: UUID,
    val eksternBrukId: UUID,
    val type: Behandlingstype,
    val status: Behandlingsstatus,
)

data class InstitusjonDto(
    val organisasjonsnummer: String,
    val navn: String,
)
