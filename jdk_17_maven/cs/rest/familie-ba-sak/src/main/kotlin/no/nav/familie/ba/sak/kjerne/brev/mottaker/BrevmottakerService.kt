package no.nav.familie.ba.sak.kjerne.brev.mottaker

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.zeroSingleOrThrow
import no.nav.familie.ba.sak.ekstern.restDomene.RestBrevmottaker
import no.nav.familie.ba.sak.ekstern.restDomene.tilBrevMottaker
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.behandling.ValiderBrevmottakerService
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.domene.ManuellAdresseInfo
import no.nav.familie.ba.sak.kjerne.steg.domene.MottakerInfo
import no.nav.familie.ba.sak.kjerne.steg.domene.toList
import no.nav.familie.kontrakter.felles.BrukerIdType
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BrevmottakerService(
    private val brevmottakerRepository: BrevmottakerRepository,
    private val loggService: LoggService,
    private val personidentService: PersonidentService,
    private val personopplysningerService: PersonopplysningerService,
    private val validerBrevmottakerService: ValiderBrevmottakerService,
) {

    @Transactional
    fun leggTilBrevmottaker(restBrevMottaker: RestBrevmottaker, behandlingId: Long) {
        val brevmottaker = restBrevMottaker.tilBrevMottaker(behandlingId)

        validerBrevmottakerService.validerAtBehandlingIkkeInneholderStrengtFortroligePersonerMedManuelleBrevmottakere(behandlingId, brevmottaker)

        loggService.opprettBrevmottakerLogg(
            brevmottaker = brevmottaker,
            brevmottakerFjernet = false,
        )

        brevmottakerRepository.save(brevmottaker)
    }

    @Transactional
    fun fjernBrevmottaker(id: Long) {
        val brevmottaker =
            brevmottakerRepository.findByIdOrNull(id) ?: throw Feil("Finner ikke brevmottaker med id=$id")

        loggService.opprettBrevmottakerLogg(
            brevmottaker = brevmottaker,
            brevmottakerFjernet = true,
        )

        brevmottakerRepository.deleteById(id)
    }

    fun hentBrevmottakere(behandlingId: Long) = brevmottakerRepository.finnBrevMottakereForBehandling(behandlingId)

    fun hentRestBrevmottakere(behandlingId: Long) =
        brevmottakerRepository.finnBrevMottakereForBehandling(behandlingId).map {
            RestBrevmottaker(
                id = it.id,
                type = it.type,
                navn = it.navn,
                adresselinje1 = it.adresselinje1,
                adresselinje2 = it.adresselinje2,
                postnummer = it.postnummer,
                poststed = it.poststed,
                landkode = it.landkode,
            )
        }

    fun lagMottakereFraBrevMottakere(
        manueltRegistrerteMottakere: List<Brevmottaker>,
        søkersident: String,
        søkersnavn: String = hentMottakerNavn(søkersident),
    ): List<MottakerInfo> {
        manueltRegistrerteMottakere.singleOrNull { it.type == MottakerType.DØDSBO }?.let {
            // brev sendes kun til den manuelt registerte dødsboadressen
            return lagMottakerInfoUtenBrukerId(navn = søkersnavn, manuellAdresseInfo = lagManuellAdresseInfo(it)).toList()
        }

        val manuellAdresseUtenlands = manueltRegistrerteMottakere.filter { it.type == MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE }
            .zeroSingleOrThrow {
                FunksjonellFeil("Mottakerfeil: Det er registrert mer enn en utenlandsk adresse tilhørende bruker")
            }?.let {
                lagMottakerInfoMedBrukerId(
                    brukerId = søkersident,
                    navn = søkersnavn,
                    manuellAdresseInfo = lagManuellAdresseInfo(it),
                )
            }

        // brev sendes til brukers (manuelt) registerte adresse (i utlandet)
        val bruker = manuellAdresseUtenlands ?: lagMottakerInfoMedBrukerId(brukerId = søkersident, navn = søkersnavn)

        // ...og evt. til en manuelt registrert verge eller fullmektig i tillegg
        val manuellTilleggsmottaker = manueltRegistrerteMottakere.filter { it.type != MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE }
            .zeroSingleOrThrow {
                FunksjonellFeil("Mottakerfeil: ${first().type.visningsnavn} kan ikke kombineres med ${last().type.visningsnavn}")
            }?.let {
                lagMottakerInfoUtenBrukerId(navn = it.navn, manuellAdresseInfo = lagManuellAdresseInfo(it))
            }

        return listOfNotNull(bruker, manuellTilleggsmottaker)
    }

    fun hentMottakerNavn(personIdent: String): String {
        val aktør = personidentService.hentAktør(personIdent)
        return personopplysningerService.hentPersoninfoNavnOgAdresse(aktør).let {
            it.navn!!
        }
    }

    private fun lagManuellAdresseInfo(brevmottaker: Brevmottaker) = ManuellAdresseInfo(
        adresselinje1 = brevmottaker.adresselinje1,
        adresselinje2 = brevmottaker.adresselinje2,
        postnummer = brevmottaker.postnummer,
        poststed = brevmottaker.poststed,
        landkode = brevmottaker.landkode,
    )

    private fun lagMottakerInfoUtenBrukerId(
        navn: String,
        manuellAdresseInfo: ManuellAdresseInfo,
    ): MottakerInfo = MottakerInfo(
        brukerId = "",
        brukerIdType = null,
        erInstitusjonVerge = false,
        navn = navn,
        manuellAdresseInfo = manuellAdresseInfo,
    )

    private fun lagMottakerInfoMedBrukerId(
        brukerId: String,
        navn: String,
        manuellAdresseInfo: ManuellAdresseInfo? = null,
    ) = MottakerInfo(
        brukerId = brukerId,
        brukerIdType = BrukerIdType.FNR,
        erInstitusjonVerge = false,
        navn = navn,
        manuellAdresseInfo = manuellAdresseInfo,
    )
}
