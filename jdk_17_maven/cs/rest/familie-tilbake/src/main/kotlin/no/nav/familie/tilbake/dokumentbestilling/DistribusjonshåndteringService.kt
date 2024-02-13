package no.nav.familie.tilbake.dokumentbestilling

import no.nav.familie.kontrakter.felles.dokdist.AdresseType
import no.nav.familie.kontrakter.felles.dokdist.ManuellAdresse
import no.nav.familie.kontrakter.felles.tilbakekreving.MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE
import no.nav.familie.kontrakter.felles.tilbakekreving.MottakerType.DØDSBO
import no.nav.familie.tilbake.behandling.BehandlingService
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.FeatureToggleService
import no.nav.familie.tilbake.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmetadata
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevmetadataUtil
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmottager
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmottager.BRUKER
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmottager.INSTITUSJON
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmottager.MANUELL_BRUKER
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmottager.MANUELL_TILLEGGSMOTTAKER
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmottager.VERGE
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.dokumentbestilling.felles.pdf.Brevdata
import no.nav.familie.tilbake.dokumentbestilling.felles.pdf.PdfBrevService
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.ManuellBrevmottakerRepository
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.domene.ManuellBrevmottaker
import no.nav.familie.tilbake.dokumentbestilling.vedtak.VedtaksbrevgunnlagService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class DistribusjonshåndteringService(
    private val brevmetadataUtil: BrevmetadataUtil,
    private val fagsakRepository: FagsakRepository,
    private val manuelleBrevmottakerRepository: ManuellBrevmottakerRepository,
    private val pdfBrevService: PdfBrevService,
    private val vedtaksbrevgrunnlagService: VedtaksbrevgunnlagService,
    private val featureToggleService: FeatureToggleService,
) {

    fun sendBrev(
        behandling: Behandling,
        brevtype: Brevtype,
        varsletBeløp: Long? = null,
        fritekst: String? = null,
        brevdata: (Brevmottager, Brevmetadata?) -> Brevdata,
    ) {
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        val vedtaksbrevgrunnlag = when (brevtype) {
            Brevtype.VEDTAK -> vedtaksbrevgrunnlagService.hentVedtaksbrevgrunnlag(behandling.id)
            else -> null
        }
        val støtterManuelleBrevmottakere: Boolean = BehandlingService.sjekkOmManuelleBrevmottakereErStøttet(
            behandling = behandling,
            fagsak = fagsak,
        )
        val brevmottakere = utledMottakere(
            behandling = behandling,
            fagsak = fagsak,
            erManuelleMottakereStøttet = støtterManuelleBrevmottakere,
            manueltRegistrerteMottakere = manuelleBrevmottakerRepository.findByBehandlingId(behandling.id).toSet(),
        ).toList()

        brevmottakere.filterNotNull().forEachIndexed { index, brevmottaker ->
            pdfBrevService.sendBrev(
                behandling = behandling,
                fagsak = fagsak,
                brevtype = brevtype,
                data = brevdata(
                    brevmottaker.somBrevmottager,
                    brevmetadataUtil.genererMetadataForBrev(
                        behandling.id,
                        vedtaksbrevgrunnlag,
                        brevmottager = brevmottaker.somBrevmottager,
                        manuellAdresseinfo = brevmottaker.manuellAdresse,
                        annenMottakersNavn = brevmottakere[brevmottakere.lastIndex - index].navn,
                    ),
                ),
                varsletBeløp = varsletBeløp,
                fritekst = fritekst,
            )
        }
    }
    companion object {
        fun utledMottakere(
            behandling: Behandling,
            fagsak: Fagsak,
            erManuelleMottakereStøttet: Boolean,
            manueltRegistrerteMottakere: Set<ManuellBrevmottaker>,
        ): Pair<Brevmottaker, Brevmottaker?> {
            return if (erManuelleMottakereStøttet) {
                require(manueltRegistrerteMottakere.all { it.behandlingId == behandling.id })

                val (manuellBrukeradresse, manuellTilleggsmottaker) = manueltRegistrerteMottakere
                    .partition { it.type == BRUKER_MED_UTENLANDSK_ADRESSE || it.type == DØDSBO }
                Pair(
                    first = manuellBrukeradresse.singleOrNull()?.let { ManuellBrevmottakerType(it) } ?: BrevmottagerType(BRUKER),
                    second = manuellTilleggsmottaker.singleOrNull()?.let { ManuellBrevmottakerType(it) },
                )
            } else {
                val defaultMottaker = if (fagsak.institusjon != null) INSTITUSJON else BRUKER
                val tilleggsmottaker = if (behandling.harVerge) VERGE else null
                Pair(
                    first = BrevmottagerType(defaultMottaker),
                    second = tilleggsmottaker?.let { BrevmottagerType(it) },
                )
            }
        }
    }
}

sealed interface Brevmottaker

class BrevmottagerType(val mottaker: Brevmottager) : Brevmottaker
class ManuellBrevmottakerType(val mottaker: ManuellBrevmottaker) : Brevmottaker

val Brevmottaker?.navn: String?
    get() = if (this is ManuellBrevmottakerType) mottaker.navn else null
val Brevmottaker.somBrevmottager: Brevmottager
    get() = (this as? BrevmottagerType)?.mottaker ?: (this as ManuellBrevmottakerType).run {
        if (mottaker.erTilleggsmottaker) MANUELL_TILLEGGSMOTTAKER else MANUELL_BRUKER
    }
val Brevmottaker?.manuellAdresse: Adresseinfo?
    get() = if (this is ManuellBrevmottakerType) {
        Adresseinfo(
            ident = mottaker.ident.orEmpty(),
            mottagernavn = mottaker.navn,
            manuellAdresse = if (mottaker.hasManuellAdresse()) {
                ManuellAdresse(
                    adresseType = when (mottaker.landkode) {
                        "NO" -> AdresseType.norskPostadresse
                        else -> AdresseType.utenlandskPostadresse
                    },
                    adresselinje1 = mottaker.adresselinje1,
                    adresselinje2 = mottaker.adresselinje2,
                    postnummer = mottaker.postnummer,
                    poststed = mottaker.poststed,
                    land = mottaker.landkode!!,
                )
            } else {
                null
            },
        )
    } else {
        null
    }
