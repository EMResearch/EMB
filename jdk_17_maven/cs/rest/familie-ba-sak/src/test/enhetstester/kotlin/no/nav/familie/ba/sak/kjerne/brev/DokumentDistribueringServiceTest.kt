package no.nav.familie.ba.sak.kjerne.brev

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.task.DistribuerDokumentDTO
import no.nav.familie.http.client.RessursException
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.prosessering.internal.TaskService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatus
import org.springframework.web.client.RestClientResponseException

@ExtendWith(MockKExtension::class)
internal class DokumentDistribueringServiceTest {

    @MockK(relaxed = true)
    private lateinit var taskService: TaskService

    @MockK
    private lateinit var integrasjonClient: IntegrasjonClient

    @MockK(relaxed = true)
    private lateinit var loggService: LoggService

    @InjectMockKs
    private lateinit var dokumentDistribueringService: DokumentDistribueringService

    @Test
    fun `Skal kalle 'loggBrevIkkeDistribuertUkjentAdresse' ved 400 kode og 'Mottaker har ukjent adresse' melding`() {
        every {
            integrasjonClient.distribuerBrev(any())
        } throws RessursException(
            httpStatus = HttpStatus.BAD_REQUEST,
            ressurs = Ressurs.failure(),
            cause = RestClientResponseException("Mottaker har ukjent adresse", 400, "", null, null, null),
        )

        dokumentDistribueringService.prøvDistribuerBrevOgLoggHendelseFraBehandling(
            distribuerDokumentDTO = lagDistribuerDokumentDTO(),
            loggBehandlerRolle = BehandlerRolle.BESLUTTER,
        )

        verify(exactly = 1) { loggService.opprettBrevIkkeDistribuertUkjentAdresseLogg(any(), any()) }
    }

    @Test
    fun `Skal kalle 'håndterMottakerDødIngenAdressePåBehandling' ved 410 Gone svar under distribuering`() {
        every {
            integrasjonClient.distribuerBrev(any())
        } throws RessursException(
            httpStatus = HttpStatus.GONE,
            ressurs = Ressurs.failure(),
            cause = RestClientResponseException("", 410, "", null, null, null),
        )

        dokumentDistribueringService.prøvDistribuerBrevOgLoggHendelseFraBehandling(
            distribuerDokumentDTO = lagDistribuerDokumentDTO(),
            loggBehandlerRolle = BehandlerRolle.BESLUTTER,
        )

        verify(exactly = 1) {
            loggService.opprettBrevIkkeDistribuertUkjentDødsboadresseLogg(any(), any())
        }
    }

    @Test
    fun `Skal hoppe over distribuering ved 409 Conflict mot dokdist`() {
        every {
            integrasjonClient.distribuerBrev(any())
        } throws RessursException(
            httpStatus = HttpStatus.CONFLICT,
            ressurs = Ressurs.failure(),
            cause = RestClientResponseException("", 409, "", null, null, null),
        )

        assertDoesNotThrow {
            dokumentDistribueringService.prøvDistribuerBrevOgLoggHendelseFraBehandling(
                distribuerDokumentDTO = lagDistribuerDokumentDTO(),
                loggBehandlerRolle = BehandlerRolle.BESLUTTER,
            )
        }
    }

    private fun lagDistribuerDokumentDTO() = DistribuerDokumentDTO(
        journalpostId = "testId",
        behandlingId = 1L,
        brevmal = Brevmal.SVARTIDSBREV,
        personEllerInstitusjonIdent = "test",
        erManueltSendt = true,
    )
}
