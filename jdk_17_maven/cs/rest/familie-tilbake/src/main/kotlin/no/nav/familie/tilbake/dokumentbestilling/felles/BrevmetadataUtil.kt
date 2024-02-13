package no.nav.familie.tilbake.dokumentbestilling.felles

import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.BehandlingService
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.config.FeatureToggleService
import no.nav.familie.tilbake.dokumentbestilling.DistribusjonshåndteringService
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.ManuellBrevmottakerRepository
import no.nav.familie.tilbake.dokumentbestilling.manuellAdresse
import no.nav.familie.tilbake.dokumentbestilling.somBrevmottager
import no.nav.familie.tilbake.dokumentbestilling.vedtak.Vedtaksbrevgrunnlag
import no.nav.familie.tilbake.organisasjon.OrganisasjonService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BrevmetadataUtil(
    private val behandlingRepository: BehandlingRepository,
    private val fagsakRepository: FagsakRepository,
    private val manuelleBrevmottakerRepository: ManuellBrevmottakerRepository,
    private val eksterneDataForBrevService: EksterneDataForBrevService,
    private val organisasjonService: OrganisasjonService,
    private val featureToggleService: FeatureToggleService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun genererMetadataForBrev(
        behandlingId: UUID,
        vedtaksbrevgrunnlag: Vedtaksbrevgrunnlag? = null,
        brevmottager: Brevmottager = Brevmottager.BRUKER,
        manuellAdresseinfo: Adresseinfo? = null,
        annenMottakersNavn: String? = null,
    ): Brevmetadata? {
        require(brevmottager != brevmottager.MANUELL || manuellAdresseinfo != null) {
            "For en manuelt registrert brevmottaker kan ikke manuellAdresseinfo være null"
        }

        val behandling: Behandling by lazy { behandlingRepository.findByIdOrThrow(behandlingId) }
        val fagsak: Fagsak by lazy { fagsakRepository.findByIdOrThrow(behandling.fagsakId) }
        val fagsystem = vedtaksbrevgrunnlag?.fagsystem ?: fagsak.fagsystem

        val aktivVerge = vedtaksbrevgrunnlag?.aktivVerge ?: behandling.aktivVerge

        val personinfo = eksterneDataForBrevService.hentPerson(
            ident = vedtaksbrevgrunnlag?.bruker?.ident ?: fagsak.bruker.ident,
            fagsystem = fagsystem,
        )
        val adresseinfo = manuellAdresseinfo ?: eksterneDataForBrevService.hentAdresse(
            personinfo = personinfo,
            brevmottager = brevmottager,
            verge = aktivVerge,
            fagsystem = fagsystem,
        )
        val vergenavn = BrevmottagerUtil.getVergenavn(aktivVerge, adresseinfo)

        val gjelderDødsfall = personinfo.dødsdato != null

        val persistertSaksbehandlerId =
            vedtaksbrevgrunnlag?.behandling?.ansvarligSaksbehandler ?: behandling.ansvarligSaksbehandler

        val brevmetadata = Brevmetadata(
            sakspartId = personinfo.ident,
            sakspartsnavn = personinfo.navn,
            finnesVerge = aktivVerge != null,
            vergenavn = vergenavn,
            finnesAnnenMottaker = annenMottakersNavn != null || aktivVerge != null,
            annenMottakersNavn = annenMottakersNavn,
            mottageradresse = adresseinfo,
            behandlendeEnhetId = vedtaksbrevgrunnlag?.behandling?.behandlendeEnhet ?: behandling.behandlendeEnhet,
            behandlendeEnhetsNavn = vedtaksbrevgrunnlag?.behandling?.behandlendeEnhetsNavn ?: behandling.behandlendeEnhetsNavn,
            ansvarligSaksbehandler = hentAnsvarligSaksbehandlerNavn(persistertSaksbehandlerId, vedtaksbrevgrunnlag),
            saksnummer = vedtaksbrevgrunnlag?.eksternFagsakId ?: fagsak.eksternFagsakId,
            språkkode = vedtaksbrevgrunnlag?.bruker?.språkkode ?: fagsak.bruker.språkkode,
            ytelsestype = vedtaksbrevgrunnlag?.ytelsestype ?: fagsak.ytelsestype,
            gjelderDødsfall = gjelderDødsfall,
            institusjon = (vedtaksbrevgrunnlag?.institusjon ?: fagsak.institusjon)?.let {
                organisasjonService.mapTilInstitusjonForBrevgenerering(it.organisasjonsnummer)
            },
        )
        return brevmetadata
    }

    fun lagBrevmetadataForMottakerTilForhåndsvisning(
        vedtaksbrevgrunnlag: Vedtaksbrevgrunnlag,
    ): Pair<Brevmetadata?, Brevmottager> {
        return lagBrevmetadataForMottakerTilForhåndsvisning(
            behandlingId = vedtaksbrevgrunnlag.behandling.id,
            vedtaksbrevgrunnlag = vedtaksbrevgrunnlag,
        )
    }

    fun lagBrevmetadataForMottakerTilForhåndsvisning(
        behandlingId: UUID,
        vedtaksbrevgrunnlag: Vedtaksbrevgrunnlag? = null,
    ): Pair<Brevmetadata?, Brevmottager> {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)

        val støtterManuelleBrevmottakere = BehandlingService.sjekkOmManuelleBrevmottakereErStøttet(
            behandling = behandling,
            fagsak = fagsak,
        )
        val (bruker, tilleggsmottaker) = DistribusjonshåndteringService.utledMottakere(
            behandling = behandling,
            fagsak = fagsak,
            erManuelleMottakereStøttet = støtterManuelleBrevmottakere,
            manueltRegistrerteMottakere = manuelleBrevmottakerRepository.findByBehandlingId(behandling.id).toSet(),
        )
        val (brevmottager, manuellAdresseinfo) = when (tilleggsmottaker) {
            null -> bruker.somBrevmottager to bruker.manuellAdresse
            else -> tilleggsmottaker.somBrevmottager to tilleggsmottaker.manuellAdresse
        }
        val metadata = genererMetadataForBrev(
            behandlingId = behandling.id,
            vedtaksbrevgrunnlag = vedtaksbrevgrunnlag,
            brevmottager = brevmottager,
            manuellAdresseinfo = manuellAdresseinfo,
            annenMottakersNavn = tilleggsmottaker?.let {
                eksterneDataForBrevService.hentPerson(fagsak.bruker.ident, fagsak.fagsystem).navn
            },
        )
        return metadata to brevmottager
    }

    private fun hentAnsvarligSaksbehandlerNavn(
        persistertSaksbehandlerId: String,
        vedtaksbrevgrunnlag: Vedtaksbrevgrunnlag?,
    ): String {
        return when {
            vedtaksbrevgrunnlag?.aktivtSteg == Behandlingssteg.FORESLÅ_VEDTAK ->
                eksterneDataForBrevService.hentPåloggetSaksbehandlernavnMedDefault(persistertSaksbehandlerId)

            vedtaksbrevgrunnlag != null ->
                eksterneDataForBrevService.hentSaksbehandlernavn(persistertSaksbehandlerId)

            persistertSaksbehandlerId != Constants.BRUKER_ID_VEDTAKSLØSNINGEN ->
                eksterneDataForBrevService.hentPåloggetSaksbehandlernavnMedDefault(persistertSaksbehandlerId)

            else -> ""
        }
    }
}

private val Brevmottager.MANUELL
    get() = when (this) {
        Brevmottager.MANUELL_BRUKER,
        Brevmottager.MANUELL_TILLEGGSMOTTAKER,
        -> this
        else -> null
    }
