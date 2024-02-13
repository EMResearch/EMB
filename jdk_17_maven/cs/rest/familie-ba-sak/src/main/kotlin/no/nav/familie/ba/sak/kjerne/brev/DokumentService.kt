package no.nav.familie.ba.sak.kjerne.brev

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.RolleConfig
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.DEFAULT_JOURNALFØRENDE_ENHET
import no.nav.familie.ba.sak.integrasjoner.journalføring.UtgåendeJournalføringService
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.DbJournalpost
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.DbJournalpostType
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.JournalføringRepository
import no.nav.familie.ba.sak.integrasjoner.organisasjon.OrganisasjonService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.ValiderBrevmottakerService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.settpåvent.SettPåVentService
import no.nav.familie.ba.sak.kjerne.brev.domene.ManueltBrevRequest
import no.nav.familie.ba.sak.kjerne.brev.domene.erTilInstitusjon
import no.nav.familie.ba.sak.kjerne.brev.mottaker.BrevmottakerService
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.kjerne.steg.domene.MottakerInfo
import no.nav.familie.ba.sak.kjerne.steg.domene.toList
import no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling.VilkårsvurderingForNyBehandlingService
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.AnnenVurderingType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.leggTilBlankAnnenVurdering
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.DistribuerDokumentDTO
import no.nav.familie.ba.sak.task.DistribuerDokumentTask
import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.dokarkiv.AvsenderMottaker
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Førsteside
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.Properties

@Service
class DokumentService(
    private val journalføringRepository: JournalføringRepository,
    private val taskRepository: TaskRepositoryWrapper,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val vilkårsvurderingForNyBehandlingService: VilkårsvurderingForNyBehandlingService,
    private val rolleConfig: RolleConfig,
    private val settPåVentService: SettPåVentService,
    private val utgåendeJournalføringService: UtgåendeJournalføringService,
    private val fagsakRepository: FagsakRepository,
    private val organisasjonService: OrganisasjonService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val dokumentGenereringService: DokumentGenereringService,
    private val brevmottakerService: BrevmottakerService,
    private val validerBrevmottakerService: ValiderBrevmottakerService,
) {

    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun hentBrevForVedtak(vedtak: Vedtak): Ressurs<ByteArray> {
        if (SikkerhetContext.hentHøyesteRolletilgangForInnloggetBruker(rolleConfig) == BehandlerRolle.VEILEDER && vedtak.stønadBrevPdF == null) {
            throw FunksjonellFeil("Det finnes ikke noe vedtaksbrev.")
        } else {
            val pdf =
                vedtak.stønadBrevPdF ?: throw Feil("Klarte ikke finne vedtaksbrevbrev for vedtak med id ${vedtak.id}")
            return Ressurs.success(pdf)
        }
    }

    @Transactional
    fun sendManueltBrev(manueltBrevRequest: ManueltBrevRequest, behandling: Behandling? = null, fagsakId: Long) {
        behandling?.let {
            validerBrevmottakerService.validerAtBehandlingIkkeInneholderStrengtFortroligePersonerMedManuelleBrevmottakere(behandlingId = it.id)
        }
        val generertBrev = dokumentGenereringService.genererManueltBrev(manueltBrevRequest)
        val førsteside = if (manueltBrevRequest.brevmal.skalGenerereForside()) {
            Førsteside(
                språkkode = manueltBrevRequest.mottakerMålform.tilSpråkkode(),
                navSkjemaId = "NAV 33.00-07",
                overskriftstittel = "Ettersendelse til søknad om barnetrygd ordinær NAV 33-00.07",
            )
        } else {
            null
        }

        val fagsak = fagsakRepository.finnFagsak(fagsakId) ?: error("Finnes ikke fagsak for fagsakId=$fagsakId")
        val søkersident = fagsak.aktør.aktivFødselsnummer()

        val mottakere = lagMottakere(manueltBrevRequest, fagsak, behandling)
        val journalposterTilDistribusjon = mutableMapOf<String, MottakerInfo>()

        mottakere.forEach { mottakerInfo ->
            val journalpostId = utgåendeJournalføringService.journalførManueltBrev(
                fnr = fagsak.aktør.aktivFødselsnummer(),
                fagsakId = fagsakId.toString(),
                journalførendeEnhet = manueltBrevRequest.enhet?.enhetId ?: DEFAULT_JOURNALFØRENDE_ENHET,
                brev = generertBrev,
                førsteside = førsteside,
                dokumenttype = manueltBrevRequest.brevmal.tilFamilieKontrakterDokumentType(),
                avsenderMottaker = utledAvsenderMottaker(manueltBrevRequest, mottakerInfo),
                // mottakersnavn fyller ut kun når manuell mottaker finnes
                tilManuellMottakerEllerVerge = mottakerInfo.navn != null &&
                    mottakerInfo.navn != brevmottakerService.hentMottakerNavn(søkersident),
            ).also { journalposterTilDistribusjon[it] = mottakerInfo }

            behandling?.let {
                journalføringRepository.save(
                    DbJournalpost(behandling = it, journalpostId = journalpostId, type = DbJournalpostType.U),
                )
            }
        }

        if (behandling != null && manueltBrevRequest.brevmal.førerTilOpplysningsplikt()) {
            leggTilOpplysningspliktIVilkårsvurdering(behandling)
        }

        lagTaskerForÅDistribuereBrev(journalposterTilDistribusjon, behandling, manueltBrevRequest, fagsak)

        if (behandling != null && manueltBrevRequest.brevmal.setterBehandlingPåVent()) {
            settPåVentService.settBehandlingPåVent(
                behandlingId = behandling.id,
                frist = LocalDate.now()
                    .plusDays(
                        manueltBrevRequest.brevmal.ventefristDager(
                            manuellFrist = manueltBrevRequest.antallUkerSvarfrist?.let { it * 7 }?.toLong(),
                            behandlingKategori = behandling.kategori,
                        ),
                    ),
                årsak = manueltBrevRequest.brevmal.venteårsak(),
            )
        }
    }

    private fun lagMottakere(
        manueltBrevRequest: ManueltBrevRequest,
        fagsak: Fagsak,
        behandling: Behandling?,
    ): List<MottakerInfo> {
        val søkersident = fagsak.aktør.aktivFødselsnummer()
        val brevmottakere = behandling?.let { brevmottakerService.hentBrevmottakere(it.id) } ?: emptyList()
        return when {
            behandling == null -> MottakerInfo(
                brukerId = søkersident,
                brukerIdType = BrukerIdType.FNR,
                erInstitusjonVerge = false,
            ).toList()
            manueltBrevRequest.erTilInstitusjon -> MottakerInfo(
                brukerId = checkNotNull(fagsak.institusjon).orgNummer,
                brukerIdType = BrukerIdType.ORGNR,
                erInstitusjonVerge = false,
            ).toList()
            brevmottakere.isNotEmpty() -> brevmottakerService.lagMottakereFraBrevMottakere(
                brevmottakere,
                søkersident,
            )
            else -> MottakerInfo(
                brukerIdType = BrukerIdType.FNR,
                brukerId = søkersident,
                erInstitusjonVerge = false,
            ).toList()
        }
    }

    private fun utledAvsenderMottaker(
        manueltBrevRequest: ManueltBrevRequest,
        mottakerInfo: MottakerInfo,
    ): AvsenderMottaker? {
        return when {
            manueltBrevRequest.erTilInstitusjon -> {
                AvsenderMottaker(
                    idType = BrukerIdType.ORGNR,
                    id = manueltBrevRequest.mottakerIdent,
                    navn = utledInstitusjonNavn(manueltBrevRequest),
                )
            }
            mottakerInfo.brukerIdType != BrukerIdType.ORGNR && mottakerInfo.navn != null -> {
                AvsenderMottaker(
                    idType = mottakerInfo.brukerIdType,
                    id = mottakerInfo.brukerIdType?.let { mottakerInfo.brukerId },
                    navn = mottakerInfo.navn,
                )
            }
            else -> {
                null
            }
        }
    }

    private fun utledInstitusjonNavn(manueltBrevRequest: ManueltBrevRequest): String {
        return manueltBrevRequest.mottakerNavn.ifBlank {
            organisasjonService.hentOrganisasjon(manueltBrevRequest.mottakerIdent).navn
        }
    }

    private fun leggTilOpplysningspliktIVilkårsvurdering(behandling: Behandling) {
        val vilkårsvurdering = vilkårsvurderingService.hentAktivForBehandling(behandling.id)
            ?: vilkårsvurderingForNyBehandlingService.initierVilkårsvurderingForBehandling(
                behandling = behandling,
                bekreftEndringerViaFrontend = false,
                forrigeBehandlingSomErVedtatt = behandlingHentOgPersisterService
                    .hentForrigeBehandlingSomErVedtatt(behandling),
            )
        vilkårsvurdering.personResultater.single { it.erSøkersResultater() }
            .leggTilBlankAnnenVurdering(AnnenVurderingType.OPPLYSNINGSPLIKT)
    }

    private fun lagTaskerForÅDistribuereBrev(
        journalposterTilDistribusjon: Map<String, MottakerInfo>,
        behandling: Behandling?,
        manueltBrevRequest: ManueltBrevRequest,
        fagsak: Fagsak,
    ) = journalposterTilDistribusjon.forEach { journalPostTilDistribusjon ->
        DistribuerDokumentTask.opprettDistribuerDokumentTask(
            distribuerDokumentDTO = DistribuerDokumentDTO(
                personEllerInstitusjonIdent = journalPostTilDistribusjon.value.brukerId,
                behandlingId = behandling?.id,
                journalpostId = journalPostTilDistribusjon.key,
                brevmal = manueltBrevRequest.brevmal,
                erManueltSendt = true,
                manuellAdresseInfo = journalPostTilDistribusjon.value.manuellAdresseInfo,
            ),
            properties = Properties().apply
                {
                    this["fagsakIdent"] = fagsak.aktør.aktivFødselsnummer()
                    this["mottakerIdent"] = journalPostTilDistribusjon.value.brukerId
                    this["journalpostId"] = journalPostTilDistribusjon.key
                    this["behandlingId"] = behandling?.id.toString()
                    this["fagsakId"] = fagsak.id.toString()
                },
        ).also { taskRepository.save(it) }
    }
}
