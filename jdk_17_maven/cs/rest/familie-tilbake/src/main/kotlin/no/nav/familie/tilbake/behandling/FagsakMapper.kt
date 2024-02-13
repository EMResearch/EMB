package no.nav.familie.tilbake.behandling

import no.nav.familie.tilbake.api.dto.BehandlingsoppsummeringDto
import no.nav.familie.tilbake.api.dto.BrukerDto
import no.nav.familie.tilbake.api.dto.FagsakDto
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.integration.pdl.internal.Personinfo
import no.nav.familie.tilbake.organisasjon.OrganisasjonService

object FagsakMapper {

    fun tilRespons(
        fagsak: Fagsak,
        personinfo: Personinfo,
        behandlinger: List<Behandling>,
        organisasjonService: OrganisasjonService,
    ): FagsakDto {
        val bruker = BrukerDto(
            personIdent = fagsak.bruker.ident,
            navn = personinfo.navn,
            fødselsdato = personinfo.fødselsdato,
            kjønn = personinfo.kjønn,
            dødsdato = personinfo.dødsdato,
        )

        val behandlingListe = behandlinger.map {
            BehandlingsoppsummeringDto(
                behandlingId = it.id,
                eksternBrukId = it.eksternBrukId,
                type = it.type,
                status = it.status,
            )
        }

        val institusjon = fagsak.institusjon?.let {
            organisasjonService.mapTilInstitusjonDto(orgnummer = it.organisasjonsnummer)
        }

        return FagsakDto(
            eksternFagsakId = fagsak.eksternFagsakId,
            ytelsestype = fagsak.ytelsestype,
            fagsystem = fagsak.fagsystem,
            språkkode = fagsak.bruker.språkkode,
            bruker = bruker,
            behandlinger = behandlingListe,
            institusjon = institusjon,
        )
    }
}
