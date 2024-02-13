package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegistrerInstitusjonOgVerge
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.institusjon.InstitusjonService
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.verge.VergeService
import org.springframework.stereotype.Service

@Service
class RegistrerInstitusjonOgVerge(
    val institusjonService: InstitusjonService,
    val vergeService: VergeService,
    val loggService: LoggService,
    val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    val fagsakService: FagsakService,
) : BehandlingSteg<RestRegistrerInstitusjonOgVerge> {

    override fun utførStegOgAngiNeste(
        behandling: Behandling,
        data: RestRegistrerInstitusjonOgVerge,
    ): StegType {
        val verge = data.tilVerge(behandling)
        val institusjon = data.tilInstitusjon()
        if (verge != null) {
            vergeService.oppdaterVergeForBehandling(behandling, verge)
            loggService.opprettRegistrerVergeLogg(
                behandling,
            )
        }
        if (institusjon != null) {
            institusjonService.hentEllerOpprettInstitusjon(
                orgNummer = institusjon.orgNummer,
                tssEksternId = institusjon.tssEksternId,
            ).apply {
                val fagsak = behandling.fagsak
                fagsak.institusjon = this
                fagsakService.lagre(fagsak)
            }
            loggService.opprettRegistrerInstitusjonLogg(
                behandling,
            )
        }

        if (verge == null && institusjon?.orgNummer == null) {
            throw Feil("Ugyldig DTO for registrer verge")
        }

        return hentNesteStegForNormalFlyt(behandling = behandlingHentOgPersisterService.hent(behandlingId = behandling.id))
    }

    override fun stegType(): StegType {
        return StegType.REGISTRERE_INSTITUSJON_OG_VERGE
    }
}
