package no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker

import no.nav.familie.kontrakter.felles.tilbakekreving.Brevmottaker
import no.nav.familie.kontrakter.felles.tilbakekreving.ManuellAdresseInfo
import no.nav.familie.tilbake.api.dto.ManuellBrevmottakerRequestDto
import no.nav.familie.tilbake.api.dto.ManuellBrevmottakerResponsDto
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.domene.ManuellBrevmottaker
import java.util.UUID

object ManuellBrevmottakerMapper {

    fun tilDomene(behandlingId: UUID, manuellBrevmottakerRequestDto: ManuellBrevmottakerRequestDto, navnFraRegister: String?) =
        ManuellBrevmottaker(
            behandlingId = behandlingId,
            type = manuellBrevmottakerRequestDto.type,
            navn = navnFraRegister ?: manuellBrevmottakerRequestDto.navn,
            ident = manuellBrevmottakerRequestDto.personIdent,
            orgNr = manuellBrevmottakerRequestDto.organisasjonsnummer,
            adresselinje1 = manuellBrevmottakerRequestDto.manuellAdresseInfo?.adresselinje1,
            adresselinje2 = manuellBrevmottakerRequestDto.manuellAdresseInfo?.adresselinje2,
            postnummer = manuellBrevmottakerRequestDto.manuellAdresseInfo?.postnummer?.trim(),
            poststed = manuellBrevmottakerRequestDto.manuellAdresseInfo?.poststed?.trim(),
            landkode = manuellBrevmottakerRequestDto.manuellAdresseInfo?.landkode,
            vergetype = manuellBrevmottakerRequestDto.vergetype,
        )

    fun tilRespons(manuellBrevmottaker: ManuellBrevmottaker) = ManuellBrevmottakerResponsDto(
        id = manuellBrevmottaker.id,
        brevmottaker = Brevmottaker(
            type = manuellBrevmottaker.type,
            navn = manuellBrevmottaker.navn,
            personIdent = manuellBrevmottaker.ident,
            organisasjonsnummer = manuellBrevmottaker.orgNr,
            manuellAdresseInfo = if (manuellBrevmottaker.hasManuellAdresse()) {
                ManuellAdresseInfo(
                    adresselinje1 = manuellBrevmottaker.adresselinje1!!,
                    adresselinje2 = manuellBrevmottaker.adresselinje2,
                    postnummer = manuellBrevmottaker.postnummer!!,
                    poststed = manuellBrevmottaker.poststed!!,
                    landkode = manuellBrevmottaker.landkode!!,
                )
            } else {
                null
            },
            vergetype = manuellBrevmottaker.vergetype,
        ),
    )
}
